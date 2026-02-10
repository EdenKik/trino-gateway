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
package io.trino.gateway.ha.persistence.dao;

import io.trino.gateway.ha.TestingJdbcConnectionManager;
import io.trino.gateway.ha.config.DataStoreConfiguration;
import io.trino.gateway.ha.persistence.JdbcConnectionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
final class TestRoutingRulesDao
{
    private JdbcConnectionManager connectionManager;
    private DataStoreConfiguration config;

    @BeforeAll
    void setup()
    {
        config = TestingJdbcConnectionManager.dataStoreConfig();
        connectionManager = TestingJdbcConnectionManager.createTestingJdbcConnectionManager(config);
    }

    @AfterAll
    void cleanup()
    {
        TestingJdbcConnectionManager.destroyTestingDatabase(config);
    }

    @Test
    void testCreateAndFindAll()
    {
        RoutingRulesDao dao = connectionManager.getJdbi().onDemand(RoutingRulesDao.class);

        dao.create("test-rule", "Test routing rule", 100, "true", List.of("action1", "action2"), "MVEL");

        List<RoutingRuleEntity> rules = dao.findAll();
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).name()).isEqualTo("test-rule");
        assertThat(rules.get(0).description()).isEqualTo("Test routing rule");
        assertThat(rules.get(0).priority()).isEqualTo(100);
        assertThat(rules.get(0).condition()).isEqualTo("true");
        assertThat(rules.get(0).actions()).containsExactly("action1", "action2");
        assertThat(rules.get(0).engine()).isEqualTo("MVEL");
    }

    @Test
    void testFindByName()
    {
        RoutingRulesDao dao = connectionManager.getJdbi().onDemand(RoutingRulesDao.class);

        dao.create("find-test", "Find by name test", 50, "condition", List.of("action"), "MVEL");

        RoutingRuleEntity rule = dao.findByName("find-test");
        assertThat(rule).isNotNull();
        assertThat(rule.name()).isEqualTo("find-test");
        assertThat(rule.priority()).isEqualTo(50);
    }

    @Test
    void testUpdate()
    {
        RoutingRulesDao dao = connectionManager.getJdbi().onDemand(RoutingRulesDao.class);

        dao.create("update-test", "Original description", 10, "original", List.of("old"), "MVEL");

        dao.update("update-test", "Updated description", 20, "updated", List.of("new1", "new2"), "MVEL");

        RoutingRuleEntity updated = dao.findByName("update-test");
        assertThat(updated.description()).isEqualTo("Updated description");
        assertThat(updated.priority()).isEqualTo(20);
        assertThat(updated.condition()).isEqualTo("updated");
        assertThat(updated.actions()).containsExactly("new1", "new2");
    }

    @Test
    void testDelete()
    {
        RoutingRulesDao dao = connectionManager.getJdbi().onDemand(RoutingRulesDao.class);

        dao.create("delete-test", "To be deleted", 5, "delete", List.of("bye"), "MVEL");
        assertThat(dao.findByName("delete-test")).isNotNull();

        dao.deleteByName("delete-test");

        assertThat(dao.findByName("delete-test")).isNull();
    }

    @Test
    void testOrderByPriorityDesc()
    {
        RoutingRulesDao dao = connectionManager.getJdbi().onDemand(RoutingRulesDao.class);

        dao.create("low-priority", "Low", 1, "low", List.of("a"), "MVEL");
        dao.create("high-priority", "High", 100, "high", List.of("b"), "MVEL");
        dao.create("med-priority", "Medium", 50, "med", List.of("c"), "MVEL");

        List<RoutingRuleEntity> rules = dao.findAll();
        assertThat(rules).hasSize(3);
        assertThat(rules.get(0).name()).isEqualTo("high-priority");
        assertThat(rules.get(1).name()).isEqualTo("med-priority");
        assertThat(rules.get(2).name()).isEqualTo("low-priority");
    }
}
