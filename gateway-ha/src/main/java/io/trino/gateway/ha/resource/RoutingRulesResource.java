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
package io.trino.gateway.ha.resource;

import com.google.inject.Inject;
import io.airlift.log.Logger;
import io.trino.gateway.ha.domain.RoutingRule;
import io.trino.gateway.ha.router.IRoutingRulesManager;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static java.util.Objects.requireNonNull;

@RolesAllowed("ADMIN")
@Path("/rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoutingRulesResource
{
    private static final Logger log = Logger.get(RoutingRulesResource.class);

    private final IRoutingRulesManager routingRulesManager;

    @Inject
    public RoutingRulesResource(IRoutingRulesManager routingRulesManager)
    {
        this.routingRulesManager = requireNonNull(routingRulesManager, "routingRulesManager is null");
    }

    @GET
    public Response getAllRules()
    {
        try {
            List<RoutingRule> rules = routingRulesManager.getRoutingRules();
            return Response.ok(rules).build();
        }
        catch (Exception e) {
            log.error(e, "Failed to retrieve routing rules");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }

    @POST
    public Response createRule(RoutingRule rule)
    {
        try {
            routingRulesManager.createRoutingRule(rule);
            log.info("Created routing rule: %s", rule.name());
            return Response.status(Response.Status.CREATED)
                    .entity(rule)
                    .build();
        }
        catch (UnsupportedOperationException e) {
            log.error(e, "Operation not supported");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        catch (Exception e) {
            log.error(e, "Failed to create routing rule");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }

    @PUT
    public Response updateRule(RoutingRule rule)
    {
        try {
            List<RoutingRule> updatedRules = routingRulesManager.updateRoutingRule(rule);
            log.info("Updated routing rule: %s", rule.name());
            return Response.ok(updatedRules).build();
        }
        catch (UnsupportedOperationException e) {
            log.error(e, "Operation not supported");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        catch (Exception e) {
            log.error(e, "Failed to update routing rule");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }

    @DELETE
    @Path("/{name}")
    public Response deleteRule(@PathParam("name") String name)
    {
        try {
            routingRulesManager.deleteRoutingRule(name);
            log.info("Deleted routing rule: %s", name);
            return Response.noContent().build();
        }
        catch (UnsupportedOperationException e) {
            log.error(e, "Operation not supported");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        catch (Exception e) {
            log.error(e, "Failed to delete routing rule");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }
}
