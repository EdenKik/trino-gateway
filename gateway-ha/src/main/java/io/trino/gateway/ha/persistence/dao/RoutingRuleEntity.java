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

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

/**
 * Database entity for routing rules.
 *
 * @param id rule id (primary key, auto-generated)
 * @param name rule name (unique identifier)
 * @param description rule description
 * @param priority execution priority
 * @param condition condition expression
 * @param actions list of action expressions
 * @param engine rule engine type
 */
public record RoutingRuleEntity(
        @ColumnName("id") Integer id,
        @ColumnName("name") String name,
        @ColumnName("description") String description,
        @ColumnName("priority") Integer priority,
        @ColumnName("condition") String condition,
        @ColumnName("actions") List<String> actions,
        @ColumnName("engine") String engine)
{
    public RoutingRuleEntity
    {
        requireNonNull(name, "name is null");
        description = requireNonNullElse(description, "");
        priority = requireNonNullElse(priority, 0);
        requireNonNull(condition, "condition is null");
        requireNonNull(actions, "actions is null");
        engine = requireNonNullElse(engine, "MVEL");
    }
}
