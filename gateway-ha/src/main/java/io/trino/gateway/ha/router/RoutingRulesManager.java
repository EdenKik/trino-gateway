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

import com.google.inject.Inject;
import io.trino.gateway.ha.config.HaGatewayConfiguration;
import io.trino.gateway.ha.config.RoutingRulesConfiguration;
import io.trino.gateway.ha.domain.RoutingRule;

import java.util.List;

public class RoutingRulesManager
        implements IRoutingRulesManager
{
    private final IRoutingRulesManager routingRulesManager;

    @Inject
    public RoutingRulesManager(
            HaGatewayConfiguration configuration,
            FileRoutingRulesManager fileRoutingRulesManager,
            DatabaseRoutingRulesManager databaseRoutingRulesManager)
    {
        RoutingRulesConfiguration routingRulesConfig = configuration.getRoutingRules();
        if (routingRulesConfig.isRulesEngineEnabled()) {
            switch (routingRulesConfig.getRulesType()) {
                case DATABASE -> this.routingRulesManager = databaseRoutingRulesManager;
                default -> this.routingRulesManager = fileRoutingRulesManager;
            }
        }
        else {
            this.routingRulesManager = fileRoutingRulesManager;
        }
    }

    @Override
    public List<RoutingRule> getRoutingRules()
    {
        return routingRulesManager.getRoutingRules();
    }

    @Override
    public List<RoutingRule> updateRoutingRule(RoutingRule routingRule)
    {
        return routingRulesManager.updateRoutingRule(routingRule);
    }

    @Override
    public void createRoutingRule(RoutingRule routingRule)
    {
        routingRulesManager.createRoutingRule(routingRule);
    }

    @Override
    public void deleteRoutingRule(String name)
    {
        routingRulesManager.deleteRoutingRule(name);
    }
}
