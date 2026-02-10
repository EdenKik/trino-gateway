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

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import io.airlift.log.Logger;
import io.airlift.units.Duration;
import io.trino.gateway.ha.config.RequestAnalyzerConfig;
import io.trino.gateway.ha.domain.RoutingRule;
import io.trino.gateway.ha.router.schema.RoutingSelectorResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Suppliers.memoizeWithExpiration;
import static io.trino.gateway.ha.handler.HttpUtils.TRINO_QUERY_PROPERTIES;
import static io.trino.gateway.ha.handler.HttpUtils.TRINO_REQUEST_USER;
import static java.util.Collections.sort;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class DatabaseRoutingGroupSelector
        implements RoutingGroupSelector
{
    private static final Logger log = Logger.get(DatabaseRoutingGroupSelector.class);
    public static final String RESULTS_ROUTING_GROUP_KEY = "routingGroup";

    private final Supplier<List<io.trino.gateway.ha.router.RoutingRule>> rules;
    private final boolean analyzeRequest;

    public DatabaseRoutingGroupSelector(IRoutingRulesManager routingRulesManager, Duration rulesRefreshPeriod, RequestAnalyzerConfig requestAnalyzerConfig)
    {
        this.analyzeRequest = requestAnalyzerConfig.isAnalyzeRequest();

        rules = memoizeWithExpiration(
                () -> loadAndCompileRules(routingRulesManager),
                rulesRefreshPeriod.toMillis(),
                MILLISECONDS);
    }

    private List<io.trino.gateway.ha.router.RoutingRule> loadAndCompileRules(IRoutingRulesManager manager)
    {
        List<RoutingRule> domainRules = manager.getRoutingRules();
        List<io.trino.gateway.ha.router.RoutingRule> executableRules = new ArrayList<>();

        for (RoutingRule rule : domainRules) {
            try {
                // Determine engine, default to MVEL if not specified
                // Currently only MVEL is supported/implemented as executable RoutingRule
                MVELRoutingRule mvelRule = new MVELRoutingRule(
                        rule.name(),
                        rule.description(),
                        rule.priority(),
                        rule.condition(),
                        (List) rule.actions());
                executableRules.add(mvelRule);
            }
            catch (Exception e) {
                log.error(e, "Failed to compile routing rule: %s", rule.name());
            }
        }
        sort(executableRules);
        return executableRules;
    }

    @Override
    public RoutingSelectorResponse findRoutingDestination(HttpServletRequest request)
    {
        Map<String, String> result = new HashMap<>();
        Map<String, Object> state = new HashMap<>();

        Map<String, Object> data;
        if (analyzeRequest) {
            TrinoQueryProperties trinoQueryProperties = (TrinoQueryProperties) request.getAttribute(TRINO_QUERY_PROPERTIES);
            TrinoRequestUser trinoRequestUser = (TrinoRequestUser) request.getAttribute(TRINO_REQUEST_USER);
            data = ImmutableMap.of("request", request, TRINO_QUERY_PROPERTIES, trinoQueryProperties, TRINO_REQUEST_USER, trinoRequestUser);
        }
        else {
            data = ImmutableMap.of("request", request);
        }

        rules.get().forEach(rule -> {
            if (rule.evaluateCondition(data, state)) {
                log.debug("%s evaluated to true on request: %s", rule, request);
                rule.evaluateAction(result, data, state);
            }
        });
        return new RoutingSelectorResponse(result.get(RESULTS_ROUTING_GROUP_KEY));
    }
}
