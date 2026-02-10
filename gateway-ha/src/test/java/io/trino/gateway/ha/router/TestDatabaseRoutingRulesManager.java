/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.gateway.ha.router;

import io.trino.gateway.ha.TestingJdbcConnectionManager;
import io.trino.gateway.ha.config.DataStoreConfiguration;
import io.trino.gateway.ha.config.HaGatewayConfiguration;
import io.trino.gateway.ha.config.RoutingRulesConfiguration;
import io.trino.gateway.ha.config.RulesType;
import io.trino.gateway.ha.domain.RoutingRule;
import io.trino.gateway.ha.persistence.JdbcConnectionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
final class TestDatabaseRoutingRulesManager
{
    private JdbcConnectionManager connectionManager;
    private DataStoreConfiguration config;
    private IRoutingRulesManager routingRulesManager;

    @BeforeAll
    void setup()
    {
        config = TestingJdbcConnectionManager.dataStoreConfig();
        connectionManager = TestingJdbcConnectionManager.createTestingJdbcConnectionManager(config);

        HaGatewayConfiguration gatewayConfig = new HaGatewayConfiguration();
        RoutingRulesConfiguration routingRulesConfig = new RoutingRulesConfiguration();
        routingRulesConfig.setRulesType(RulesType.DATABASE);
        gatewayConfig.setRoutingRules(routingRulesConfig);

        routingRulesManager = new DatabaseRoutingRulesManager(connectionManager);
    }

    @AfterAll
    void cleanup()
    {
        TestingJdbcConnectionManager.destroyTestingDatabase(config);
    }

    @Test
    void testGetRoutingRulesFromDatabase()
    {
        // Create a test rule
        RoutingRule rule = new RoutingRule(
                "test-rule",
                "Test routing rule",
                100,
                List.of("result.put(\"routingGroup\", \"test-group\")"),
                "true");

        routingRulesManager.createRoutingRule(rule);

        List<RoutingRule> rules = routingRulesManager.getRoutingRules();
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).name()).isEqualTo("test-rule");
        assertThat(rules.get(0).priority()).isEqualTo(100);
    }

    @Test
    void testCreateRoutingRule()
    {
        RoutingRule rule = new RoutingRule(
                "airflow-rule",
                "Route airflow queries to ETL",
                50,
                List.of("result.put(\"routingGroup\", \"etl\")"),
                "request.getHeader(\"X-Trino-Source\") == \"airflow\"");

        routingRulesManager.createRoutingRule(rule);

        List<RoutingRule> rules = routingRulesManager.getRoutingRules();
        assertThat(rules).anyMatch(r -> r.name().equals("airflow-rule"));
    }

    @Test
    void testUpdateRoutingRule()
    {
        RoutingRule original = new RoutingRule(
                "update-test",
                "Original description",
                10,
                List.of("result.put(\"routingGroup\", \"original\")"),
                "true");

        routingRulesManager.createRoutingRule(original);

        RoutingRule updated = new RoutingRule(
                "update-test",
                "Updated description",
                20,
                List.of("result.put(\"routingGroup\", \"updated\")"),
                "false");

        List<RoutingRule> result = routingRulesManager.updateRoutingRule(updated);

        assertThat(result).anyMatch(r ->
                r.name().equals("update-test") &&
                r.description().equals("Updated description") &&
                r.priority() == 20);
    }

    @Test
    void testDeleteRoutingRule()
    {
        RoutingRule rule = new RoutingRule(
                "delete-me",
                "To be deleted",
                5,
                List.of("action"),
                "condition");

        routingRulesManager.createRoutingRule(rule);
        assertThat(routingRulesManager.getRoutingRules()).anyMatch(r -> r.name().equals("delete-me"));

        routingRulesManager.deleteRoutingRule("delete-me");

        assertThat(routingRulesManager.getRoutingRules()).noneMatch(r -> r.name().equals("delete-me"));
    }

    @Test
    void testRuleLogicEvaluation()
    {
        // Test that rules loaded from DB can be compiled and executed as MVELRoutingRule
        RoutingRule rule = new RoutingRule(
                "logic-test",
                "Test MVEL execution",
                100,
                List.of("result.put(\"routingGroup\", \"test-group\")"),
                "request.get(\"source\") == \"test-source\"");

        routingRulesManager.createRoutingRule(rule);

        List<RoutingRule> rules = routingRulesManager.getRoutingRules();
        RoutingRule loadedRule = rules.stream()
                .filter(r -> r.name().equals("logic-test"))
                .findFirst()
                .orElseThrow();

        // Convert to MVELRoutingRule for execution
        MVELRoutingRule mvelRule = new MVELRoutingRule(
                loadedRule.name(),
                loadedRule.description(),
                loadedRule.priority(),
                loadedRule.condition(),
                (List) loadedRule.actions());

        // Test condition evaluation
        Map<String, Object> data = new HashMap<>();
        data.put("source", "test-source");
        Map<String, Object> state = new HashMap<>();

        boolean conditionResult = mvelRule.evaluateCondition(data, state);
        assertThat(conditionResult).isTrue();

        // Test action evaluation
        Map<String, String> result = new HashMap<>();
        mvelRule.evaluateAction(result, data, state);
        assertThat(result.get("routingGroup")).isEqualTo("test-group");
    }

    @Test
    void testUnsupportedOperationsForFileMode()
    {
        HaGatewayConfiguration fileConfig = new HaGatewayConfiguration();
        RoutingRulesConfiguration fileRulesConfig = new RoutingRulesConfiguration();
        fileRulesConfig.setRulesType(RulesType.FILE);
        fileRulesConfig.setRulesConfigPath("some/path");
        fileConfig.setRoutingRules(fileRulesConfig);

        IRoutingRulesManager fileManager = new FileRoutingRulesManager(fileConfig);

        RoutingRule rule = new RoutingRule("test", "desc", 1, List.of("action"), "condition");

        assertThatThrownBy(() -> fileManager.createRoutingRule(rule))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Creating rules is only supported for DATABASE rules type");

        assertThatThrownBy(() -> fileManager.deleteRoutingRule("test"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Deleting rules is only supported for DATABASE rules type");
    }
}
