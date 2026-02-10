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

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface RoutingRulesDao
{
    @SqlQuery("SELECT * FROM routing_rules ORDER BY priority DESC, name")
    List<RoutingRuleEntity> findAll();

    @SqlQuery("""
            SELECT * FROM routing_rules
            WHERE name = :name
            LIMIT 1
            """)
    RoutingRuleEntity findByName(@Bind("name") String name);

    @SqlUpdate("""
            INSERT INTO routing_rules (name, description, priority, condition, actions, engine)
            VALUES (:name, :description, :priority, :condition, :actions, :engine)
            """)
    @GetGeneratedKeys("id")
    long create(@Bind("name") String name,
                @Bind("description") String description,
                @Bind("priority") Integer priority,
                @Bind("condition") String condition,
                @Bind("actions") List<String> actions,
                @Bind("engine") String engine);

    @SqlUpdate("""
            UPDATE routing_rules
            SET description = :description, priority = :priority, condition = :condition, actions = :actions, engine = :engine
            WHERE name = :name
            """)
    void update(@Bind("name") String name,
                @Bind("description") String description,
                @Bind("priority") Integer priority,
                @Bind("condition") String condition,
                @Bind("actions") List<String> actions,
                @Bind("engine") String engine);

    @SqlUpdate("""
            DELETE FROM routing_rules
            WHERE name = :name
            """)
    void deleteByName(@Bind("name") String name);
}
