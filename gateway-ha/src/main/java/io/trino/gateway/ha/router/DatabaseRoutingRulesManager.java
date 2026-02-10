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
import io.trino.gateway.ha.domain.RoutingRule;
import io.trino.gateway.ha.persistence.JdbcConnectionManager;
import io.trino.gateway.ha.persistence.dao.RoutingRuleEntity;
import io.trino.gateway.ha.persistence.dao.RoutingRulesDao;

import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;

public class DatabaseRoutingRulesManager
        implements IRoutingRulesManager
{
    private final JdbcConnectionManager jdbcConnectionManager;

    @Inject
    public DatabaseRoutingRulesManager(JdbcConnectionManager jdbcConnectionManager)
    {
        this.jdbcConnectionManager = requireNonNull(jdbcConnectionManager, "jdbcConnectionManager is null");
    }

    @Override
    public List<RoutingRule> getRoutingRules()
    {
        List<RoutingRuleEntity> entities = jdbcConnectionManager.getJdbi()
                .onDemand(RoutingRulesDao.class)
                .findAll();

        return entities.stream()
                .map(entity -> new RoutingRule(
                        entity.name(),
                        entity.description(),
                        entity.priority(),
                        entity.actions(),
                        entity.condition()))
                .collect(toImmutableList());
    }

    @Override
    public List<RoutingRule> updateRoutingRule(RoutingRule routingRule)
    {
        RoutingRulesDao dao = jdbcConnectionManager.getJdbi().onDemand(RoutingRulesDao.class);

        dao.update(
                routingRule.name(),
                routingRule.description(),
                routingRule.priority(),
                routingRule.condition(),
                routingRule.actions(),
                "MVEL");

        return getRoutingRules();
    }

    @Override
    public void createRoutingRule(RoutingRule routingRule)
    {
        RoutingRulesDao dao = jdbcConnectionManager.getJdbi().onDemand(RoutingRulesDao.class);
        dao.create(
                routingRule.name(),
                routingRule.description(),
                routingRule.priority(),
                routingRule.condition(),
                routingRule.actions(),
                "MVEL");
    }

    @Override
    public void deleteRoutingRule(String name)
    {
        RoutingRulesDao dao = jdbcConnectionManager.getJdbi().onDemand(RoutingRulesDao.class);
        dao.deleteByName(name);
    }
}
