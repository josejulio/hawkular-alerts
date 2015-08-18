/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.alerts.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import static org.hawkular.alerts.rest.HawkularAlertsApp.TENANT_HEADER_NAME;

import java.util.Collection;
import java.util.Map;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.hawkular.alerts.api.model.condition.AvailabilityCondition;
import org.hawkular.alerts.api.model.condition.CompareCondition;
import org.hawkular.alerts.api.model.condition.Condition;
import org.hawkular.alerts.api.model.condition.ExternalCondition;
import org.hawkular.alerts.api.model.condition.StringCondition;
import org.hawkular.alerts.api.model.condition.ThresholdCondition;
import org.hawkular.alerts.api.model.condition.ThresholdRangeCondition;
import org.hawkular.alerts.api.model.dampening.Dampening;
import org.hawkular.alerts.api.model.trigger.Mode;
import org.hawkular.alerts.api.model.trigger.Tag;
import org.hawkular.alerts.api.model.trigger.Trigger;
import org.hawkular.alerts.api.services.DefinitionsService;
import org.hawkular.alerts.engine.exception.NotFoundException;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * REST endpoint for triggers
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Path("/triggers")
@Api(value = "/triggers",
        description = "Trigger Handling")
public class TriggersHandler {
    private static final Logger log = Logger.getLogger(TriggersHandler.class);

    @HeaderParam(TENANT_HEADER_NAME)
    String tenantId;

    @EJB
    DefinitionsService definitions;

    ObjectMapper objectMapper;

    public TriggersHandler() {
        log.debugf("Creating instance.");
        objectMapper = new ObjectMapper();
    }

    @GET
    @Path("/")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Find all Trigger definitions",
            notes = "Pagination is not yet implemented")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response findTriggers() {
        try {
            Collection<Trigger> triggers = definitions.getTriggers(tenantId);
            log.debugf("Triggers: %s ", triggers);
            return ResponseUtil.ok(triggers);
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @GET
    @Path("/tag")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Find all Trigger definitions for the specified tag. At least one of category and name is required",
            notes = "Pagination is not yet implemented")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Triggers list found. Can be empty."),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response findTriggersByTag(
            @ApiParam(required = false, value = "The tag category. If not supplied or empty only tag name is used")
            @QueryParam("category")
            final String category,
            @ApiParam(required = false, value = "The tag name. If not supplied or empty only tag category is used")
            @QueryParam("name")
            final String name) {
        try {
            Collection<Trigger> triggers = definitions.getTriggersByTag(tenantId, category, name);
            log.debugf("Triggers: %s ", triggers);
            return ResponseUtil.ok(triggers);
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @POST
    @Path("/")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Create a new trigger definitions. If trigger ID is null, a (likely) unique ID will be generated",
            response = Trigger.class,
            notes = "Returns Trigger created if operation finished correctly")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Trigger Created"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters") })
    public Response createTrigger(
            @ApiParam(value = "Trigger definition to be created", name = "trigger", required = true)
            final Trigger trigger) {
        try {
            if (null != trigger) {
                if (isEmpty(trigger.getId())) {
                    trigger.setId(Trigger.generateId());
                } else if (definitions.getTrigger(tenantId, trigger.getId()) != null) {
                    return ResponseUtil.badRequest("Trigger with ID [" + trigger.getId() + "] exists.");
                }
                definitions.addTrigger(tenantId, trigger);
                log.debugf("Trigger: %s ", trigger.toString());
                return ResponseUtil.ok(trigger);
            } else {
                return ResponseUtil.badRequest("Trigger is null");
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @GET
    @Path("/{triggerId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get an existing trigger definition",
            response = Trigger.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Trigger found"),
            @ApiResponse(code = 404, message = "Trigger not found"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getTrigger(
            @ApiParam(value = "Trigger definition id to be retrieved", required = true)
            @PathParam("triggerId")
            final String triggerId) {
        try {
            Trigger found = definitions.getTrigger(tenantId, triggerId);
            if (found != null) {
                log.debugf("Trigger: %s ", found);
                return ResponseUtil.ok(found);
            } else {
                return ResponseUtil.notFound("triggerId: " + triggerId + " not found");
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @PUT
    @Path("/{triggerId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Update an existing trigger definition")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Trigger updated"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 404, message = "Trigger doesn't exist/Invalid Parameters") })
    public Response updateTrigger(
            @ApiParam(value = "Trigger definition id to be updated", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Updated trigger definition", name = "trigger", required = true)
            final Trigger trigger) {
        try {
            if (trigger != null && !isEmpty(triggerId)) {
                trigger.setId(triggerId);
            }
            definitions.updateTrigger(tenantId, trigger);
            log.debugf("Trigger: %s ", trigger);
            return ResponseUtil.ok();

        } catch (NotFoundException e) {
            return ResponseUtil.notFound("Trigger " + triggerId + " doesn't exist for update");

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @POST
    @Path("/{triggerId}/child")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Create a new child trigger for the specified parent trigger. If childId is null, a (likely) unique"
                    + " ID will be generated. If context is null it will be inherited from the parent.",
            response = Trigger.class,
            notes = "Returns Trigger created if operation finished correctly")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Trigger Created"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 404, message = "Parent trigger not found."),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters") })
    public Response createChildTrigger(
            @ApiParam(required = true, value = "Parent Trigger id")
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(required = false, name = "childId", value = "Child Trigger id. Generated if null.")
            final String childId,
            @ApiParam(required = true, name = "childName", value = "Child Trigger name. Unique.")
            final String childName,
            @ApiParam(required = false, name = "childContext", value = "Child Trigger context. Inherited if null")
            final Map<String, String> childContext,
            @ApiParam(required = true, name = "dataIdMap",
                    value = "Condition DataId token replacements for each dataId in conditions.")
            final Map<String, String> dataIdMap) {
        try {
            Trigger child = definitions.addChildTrigger(tenantId, triggerId, childId, childName, childContext,
                    dataIdMap);
            log.debugf("Child Trigger: %s ", child.toString());
            return ResponseUtil.ok(child);

        } catch (NotFoundException e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.notFound(e.getMessage());
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @POST
    @Path("/{triggerId}/orphan/{childTriggerId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Make a non-orphan child trigger into an orphan.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Trigger updated"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 404, message = "Trigger doesn't exist/Invalid Parameters") })
    public Response orphanChildTrigger(
            @ApiParam(value = "Parent Trigger id", required = true)//
            @PathParam("triggerId")//
            final String triggerId,
            @ApiParam(value = "Child Trigger id to be made an orphan.", required = true)//
            @PathParam("childTriggerId")//
            final String childTriggerId) {
        try {
            Trigger child = definitions.orphanChildTrigger(tenantId, childTriggerId);
            log.debugf("Orphan Child Trigger: %s ", child);
            return ResponseUtil.ok();

        } catch (NotFoundException e) {
            return ResponseUtil.notFound("Child Trigger " + childTriggerId + " doesn't exist for update");

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @POST
    @Path("/{triggerId}/unorphan/{childTriggerId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Make am orphan child trigger into a non-orphan.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Trigger updated"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 404, message = "Trigger doesn't exist/Invalid Parameters") })
    public Response unorphanChildTrigger(
            @ApiParam(value = "Parent Trigger id", required = true)//
            @PathParam("triggerId")//
            final String triggerId,
            @ApiParam(value = "Child Trigger id for the child to be converted", required = true)//
            @PathParam("childTriggerId")//
            final String childTriggerId,
            @ApiParam(required = false, name = "childContext", value = "Child Trigger context. Inherited if null")//
            final Map<String, String> childContext,
            @ApiParam(required = true, name = "dataIdMap",
                    value = "Condition DataId token replacements for each dataId in conditions.")//
            final Map<String, String> dataIdMap) {
        try {
            Trigger child = definitions.unorphanChildTrigger(tenantId, childTriggerId, childContext, dataIdMap);
            log.debugf("Child Trigger: %s ", child);
            return ResponseUtil.ok();

        } catch (NotFoundException e) {
            return ResponseUtil.notFound("Trigger " + triggerId + " doesn't exist for update");

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @DELETE
    @Path("/{triggerId}/parent")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Delete a parent trigger. Use this endpoint when needing to preserve child triggers.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Parent Trigger Removed"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 404, message = "Parent Trigger not found"),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters") })
    public Response deleteParentTrigger(
            @ApiParam(required = true, value = "Parent Trigger id")//
            @PathParam("triggerId")//
            final String triggerId,
            @ApiParam(required = true, value = "Convert the non-orphan child triggers to standard triggers.")//
            @QueryParam("keepChildren")//
            final boolean keepChildren,
            @ApiParam(required = true, value = "Convert the non-orphan child triggers to standard triggers.")//
            @QueryParam("keepOrphans")//
            final boolean keepOrphans) {
        try {
            definitions.removeParentTrigger(tenantId, triggerId, keepChildren, keepOrphans);
            log.debugf("Remove Parent Trigger: %s/%s ", tenantId, triggerId);
            return ResponseUtil.ok();

        } catch (NotFoundException e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.notFound(e.getMessage());
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @DELETE
    @Path("/{triggerId}")
    @ApiOperation(value = "Delete an existing trigger definition")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Trigger deleted"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 404, message = "Trigger not found") })
    public Response deleteTrigger(
            @ApiParam(value = "Trigger definition id to be deleted", required = true)
            @PathParam("triggerId")
            final String triggerId) {
        try {
            definitions.removeTrigger(tenantId, triggerId);
            log.debugf("TriggerId: %s ", triggerId);
            return ResponseUtil.ok();

        } catch (NotFoundException e) {
            return ResponseUtil.notFound("Trigger " + triggerId + " doesn't exist for delete");

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @GET
    @Path("/{triggerId}/dampenings")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get all Dampenings for a Trigger (1 Dampening per mode).")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response getTriggerDampenings(
            @ApiParam(value = "Trigger definition id to be retrieved", required = true)
            @PathParam("triggerId")
            final String triggerId) {
        try {
            Collection<Dampening> dampenings = definitions.getTriggerDampenings(tenantId, triggerId, null);
            log.debugf("Dampenings: %s ", dampenings);
            return ResponseUtil.ok(dampenings);
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @GET
    @Path("/{triggerId}/dampenings/mode/{triggerMode}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get a dampening using triggerId and triggerMode")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getTriggerModeDampenings(
            @ApiParam(value = "Trigger definition id to be retrieved", required = true)//
            @PathParam("triggerId")//
            final String triggerId,//
            @ApiParam(value = "Trigger mode", required = true)//
            @PathParam("triggerMode")//
            final Mode triggerMode) {
        try {
            Collection<Dampening> dampenings = definitions.getTriggerDampenings(tenantId, triggerId,
                    triggerMode);
            log.debugf("Dampenings: %s ", dampenings);
            return ResponseUtil.ok(dampenings);
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @GET
    @Path("/{triggerId}/dampenings/{dampeningId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get an existing dampening")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Dampening Found"),
            @ApiResponse(code = 404, message = "No Dampening Found"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getDampening(
            @ApiParam(value = "Trigger definition id to be retrieved", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Dampening id", required = true)
            @PathParam("dampeningId")
            final String dampeningId) {
        try {
            Dampening found = definitions.getDampening(tenantId, dampeningId);
            log.debugf("Dampening: %s ", found);
            if (found == null) {
                return ResponseUtil.notFound("No dampening found for triggerId: " + triggerId + " and dampeningId:" +
                        dampeningId);
            }
            return ResponseUtil.ok(found);
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @POST
    @Path("/{triggerId}/dampenings")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create a new dampening", notes = "Returns Dampening created if operation finishes correctly")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Dampening created"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters") })
    public Response createDampening(
            @ApiParam(value = "Trigger definition id attached to dampening", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Dampening definition to be created", required = true)
            final Dampening dampening) {
        try {
            dampening.setTriggerId(triggerId);
            boolean exists = (definitions.getDampening(triggerId, dampening.getDampeningId()) != null);
            if (!exists) {
                // make sure we have the best chance of clean data..
                Dampening d = getCleanDampening(dampening);
                definitions.addDampening(tenantId, d);
                log.debugf("Dampening: %s ", d);
                return ResponseUtil.ok(d);
            } else {
                return ResponseUtil.badRequest("Existing dampening for dampeningId: " + dampening.getDampeningId());
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    private Dampening getCleanDampening(Dampening dampening) throws Exception {
        switch (dampening.getType()) {
            case STRICT:
                return Dampening.forStrict(dampening.getTriggerId(), dampening.getTriggerMode(),
                        dampening.getEvalTrueSetting());

            case STRICT_TIME:
                return Dampening.forStrictTime(dampening.getTriggerId(), dampening.getTriggerMode(),
                        dampening.getEvalTimeSetting());

            case STRICT_TIMEOUT:
                return Dampening.forStrictTimeout(dampening.getTriggerId(), dampening.getTriggerMode(),
                        dampening.getEvalTimeSetting());
            case RELAXED_COUNT:
                return Dampening.forRelaxedCount(dampening.getTriggerId(), dampening.getTriggerMode(),
                        dampening.getEvalTrueSetting(),
                        dampening.getEvalTotalSetting());
            case RELAXED_TIME:
                return Dampening.forRelaxedTime(dampening.getTriggerId(), dampening.getTriggerMode(),
                        dampening.getEvalTrueSetting(), dampening.getEvalTimeSetting());

            default:
                throw new Exception("Unhandled Dampening Type: " + dampening.toString());
        }
    }

    @PUT
    @Path("/{triggerId}/dampenings/{dampeningId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Update an existing dampening definition. Note that the trigger mode can not be changed.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Dampening Updated"),
            @ApiResponse(code = 404, message = "No Dampening Found"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response updateDampening(
            @ApiParam(value = "Trigger definition id to be retrieved", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Dampening id", required = true)
            @PathParam("dampeningId")
            final String dampeningId,
            @ApiParam(value = "Updated dampening definition", required = true)
            final Dampening dampening) {
        try {
            boolean exists = (definitions.getDampening(tenantId, dampeningId) != null);
            if (exists) {
                // make sure we have the best chance of clean data..
                Dampening d = getCleanDampening(dampening);
                definitions.updateDampening(tenantId, d);
                log.debugf("Dampening: %s ", d);
                return ResponseUtil.ok(d);
            } else {
                return ResponseUtil.notFound("No dampening found for dampeningId: " + dampeningId);
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @DELETE
    @Path("/{triggerId}/dampenings/{dampeningId}")
    @ApiOperation(value = "Delete an existing dampening definition")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Dampening deleted"),
            @ApiResponse(code = 404, message = "No Dampening found"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response deleteDampening(
            @ApiParam(value = "Trigger definition id to be retrieved", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Dampening id for dampening definition to be deleted", required = true)
            @PathParam("dampeningId")
            final String dampeningId) {
        try {
            boolean exists = (definitions.getDampening(tenantId, dampeningId) != null);
            if (exists) {
                definitions.removeDampening(tenantId, dampeningId);
                log.debugf("DampeningId: %s ", dampeningId);
                return ResponseUtil.ok();
            } else {
                return ResponseUtil.notFound("Dampening not found for dampeningId: " + dampeningId);
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @GET
    @Path("/{triggerId}/conditions")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get a map with all conditions for a specific trigger.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getTriggerConditions(
            @ApiParam(value = "Trigger definition id to be retrieved", required = true)
            @PathParam("triggerId")
            final String triggerId) {
        try {
            Collection<Condition> conditions = definitions.getTriggerConditions(tenantId, triggerId, null);
            log.debugf("Conditions: %s ", conditions);
            return ResponseUtil.ok(conditions);
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @GET
    @Path("/{triggerId}/conditions/{conditionId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get a condition for a specific trigger id.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Condition found"),
            @ApiResponse(code = 404, message = "No Condition found"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getTriggerCondition(
            @ApiParam(value = "Trigger definition id to be retrieved", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @PathParam("conditionId")
            final String conditionId) {
        try {
            Trigger trigger = definitions.getTrigger(tenantId, triggerId);
            if (trigger == null) {
                return ResponseUtil.notFound("No trigger found for triggerId: " + triggerId);
            }
            Condition found = definitions.getCondition(tenantId, conditionId);
            if (found == null) {
                return ResponseUtil.notFound("No condition found for conditionId: " + conditionId);
            }
            if (!found.getTriggerId().equals(triggerId)) {
                return ResponseUtil.notFound("ConditionId: " + conditionId + " does not belong to triggerId: " +
                        triggerId);
            }
            return ResponseUtil.ok(found);
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @POST
    @Path("/{triggerId}/conditions")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create a new condition for a specific trigger. Add/Delete/Update of a condition "
            + "effectively replaces the condition set for the trigger.  The new condition set is returned. "
            + "Clients code should then use the new condition set as ConditionIds may have changed!")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Condition created"),
            @ApiResponse(code = 404, message = "No trigger found"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters") })
    public Response createCondition(
            @ApiParam(value = "Trigger definition id to be retrieved", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Json representation of a condition. For examples of Condition types, See "
                    + "https://github.com/hawkular/hawkular-alerts/blob/master/hawkular-alerts-rest-tests/"
                    + "src/test/groovy/org/hawkular/alerts/rest/ConditionsITest.groovy")
            String jsonCondition) {
        try {
            Trigger trigger = definitions.getTrigger(tenantId, triggerId);
            if (trigger == null) {
                return ResponseUtil.notFound("No trigger found for triggerId: " + triggerId);
            }
            if (isEmpty(jsonCondition) || !jsonCondition.contains("type")) {
                return ResponseUtil.badRequest("json condition empty or without type");
            } else {
                Condition.Type conditionType = conditionType(jsonCondition);
                if (conditionType == null || isEmpty(triggerId)) {
                    return ResponseUtil.badRequest("Bad type in json condition");
                }
                Condition condition = null;
                if (conditionType.equals(Condition.Type.AVAILABILITY)) {
                    condition = objectMapper.readValue(jsonCondition, AvailabilityCondition.class);
                } else if (conditionType.equals(Condition.Type.COMPARE)) {
                    condition = objectMapper.readValue(jsonCondition, CompareCondition.class);
                } else if (conditionType.equals(Condition.Type.STRING)) {
                    condition = objectMapper.readValue(jsonCondition, StringCondition.class);
                } else if (conditionType.equals(Condition.Type.THRESHOLD)) {
                    condition = objectMapper.readValue(jsonCondition, ThresholdCondition.class);
                } else if (conditionType.equals(Condition.Type.RANGE)) {
                    condition = objectMapper.readValue(jsonCondition, ThresholdRangeCondition.class);
                } else if (conditionType.equals(Condition.Type.EXTERNAL)) {
                    condition = objectMapper.readValue(jsonCondition, ExternalCondition.class);
                }
                if (condition == null) {
                    return ResponseUtil.badRequest("Bad json condition");
                }
                condition.setTriggerId(triggerId);
                Collection<Condition> conditions = definitions.addCondition(tenantId,
                        condition.getTriggerId(),
                        condition.getTriggerMode(),
                        condition);
                log.debugf("Conditions: %s ", conditions);
                return ResponseUtil.ok(conditions);
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @PUT
    @Path("/{triggerId}/conditions/{conditionId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Update an existing condition for a specific trigger. Add/Delete/Update of a condition "
            + "effectively replaces the condition set for the trigger.  The new condition set is returned. "
            + "Clients code should then use the new condition set as ConditionIds may have changed!")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Condition updated"),
            @ApiResponse(code = 404, message = "No Condition found"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters") })
    public Response updateCondition(
            @ApiParam(value = "Trigger definition id to be retrieved", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @PathParam("conditionId")
            final String conditionId,
            @ApiParam(value = "Json representation of a condition")
            String jsonCondition) {
        try {
            Trigger trigger = definitions.getTrigger(tenantId, triggerId);
            if (trigger == null) {
                return ResponseUtil.notFound("No trigger found for triggerId: " + triggerId);
            }
            if (isEmpty(jsonCondition) || !jsonCondition.contains("type")) {
                return ResponseUtil.badRequest("json condition empty or without type");
            } else {
                Condition.Type conditionType = conditionType(jsonCondition);
                if (conditionType == null || isEmpty(conditionId)) {
                    return ResponseUtil.badRequest("Bad type in json condition");
                }
                Condition condition = null;
                if (conditionType.equals(Condition.Type.AVAILABILITY)) {
                    condition = objectMapper.readValue(jsonCondition, AvailabilityCondition.class);
                } else if (conditionType.equals(Condition.Type.COMPARE)) {
                    condition = objectMapper.readValue(jsonCondition, CompareCondition.class);
                } else if (conditionType.equals(Condition.Type.STRING)) {
                    condition = objectMapper.readValue(jsonCondition, StringCondition.class);
                } else if (conditionType.equals(Condition.Type.THRESHOLD)) {
                    condition = objectMapper.readValue(jsonCondition, ThresholdCondition.class);
                } else if (conditionType.equals(Condition.Type.RANGE)) {
                    condition = objectMapper.readValue(jsonCondition, ThresholdRangeCondition.class);
                } else if (conditionType.equals(Condition.Type.EXTERNAL)) {
                    condition = objectMapper.readValue(jsonCondition, ExternalCondition.class);
                }
                if (condition == null) {
                    return ResponseUtil.badRequest("Bad json condition");
                }
                condition.setTriggerId(triggerId);
                boolean exists = false;
                if (conditionId.equals(condition.getConditionId())) {
                    exists = (definitions.getCondition(tenantId, condition.getConditionId()) != null);
                }
                if (!exists) {
                    return ResponseUtil.notFound("Condition not found for conditionId: " + conditionId);
                } else {
                    Collection<Condition> conditions = definitions.updateCondition(tenantId, condition);
                    log.debugf("Conditions: %s ", conditions);
                    return ResponseUtil.ok(conditions);
                }
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @DELETE
    @Path("/{triggerId}/conditions/{conditionId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Delete an existing condition for a specific trigger. Add/Delete/Update of a condition "
            + "effectively replaces the condition set for the trigger.  The new condition set is returned. "
            + "Clients code should then use the new condition set as ConditionIds may have changed!")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Condition deleted"),
            @ApiResponse(code = 404, message = "No Condition found"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters") })
    public Response deleteCondition(
            @ApiParam(value = "Trigger definition id to be retrieved", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @PathParam("conditionId")
            final String conditionId) {
        try {
            Trigger trigger = definitions.getTrigger(tenantId, triggerId);
            if (trigger == null) {
                return ResponseUtil.notFound("No trigger found for triggerId: " + triggerId);
            }
            Condition condition = definitions.getCondition(tenantId, conditionId);
            if (condition == null) {
                return ResponseUtil.notFound("No condition found for conditionId: " + conditionId);
            }
            if (!condition.getTriggerId().equals(triggerId)) {
                return ResponseUtil.badRequest("ConditionId: " + conditionId + " does not belong to triggerId: " +
                        triggerId);
            }
            Collection<Condition> conditions = definitions.removeCondition(tenantId, conditionId);
            log.debugf("Conditions: %s ", conditions);
            return ResponseUtil.ok(conditions);
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    private Condition.Type conditionType(String jsonCondition) {
        int startType = jsonCondition.indexOf("\"type\"") + 6;
        int endType = jsonCondition.indexOf(",", startType);
        if (endType == -1) {
            endType = jsonCondition.indexOf("}", startType);
            if (endType == -1) {
                return null;
            }
        }
        String type = jsonCondition.substring(startType, endType);
        startType = type.indexOf('"') + 1;
        endType = type.indexOf('"', startType);
        type = type.substring(startType, endType);
        try {
            return Condition.Type.valueOf(type);
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
        }
        return null;
    }

    @POST
    @Path("/tags")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create a new trigger tag", notes = "Returns Tag created if operation finished correctly")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Tag created"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters") })
    public Response createTag(
            @ApiParam(value = "Tag to be created", required = true)
            final Tag tag) {
        try {
            if (isEmpty(tag.getTriggerId()) || isEmpty(tag.getName())) {
                return ResponseUtil.badRequest("Invalid tag, triggerId or name required");
            }
            definitions.addTag(tenantId, tag);
            log.debugf("Tag: %s ", tag);
            return ResponseUtil.ok(tag);
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @POST
    @Path("/{triggerId}/tags")
    @ApiOperation(value = "Delete existing Tags from a Trigger")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Tags deleted"),
            @ApiResponse(code = 404, message = "No Trigger Found"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters") })
    public Response deleteTags(
            @ApiParam(value = "Trigger id of tags to be deleted", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Category of tags to be deleted", required = false)
            @QueryParam("category")
            final String category,
            @ApiParam(value = "Name of tags to be deleted", required = false)
            @QueryParam("name")
            final String name) {
        try {
            definitions.removeTags(tenantId, triggerId, category, name);
            return ResponseUtil.ok();
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    @GET
    @Path("/{triggerId}/tags")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get tags for a trigger.",
            responseContainer = "Collection<Tag>",
            response = Tag.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters") })
    public Response getTriggerTags(
            @ApiParam(value = "Trigger id for the retrieved Tags", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Category of tags to be retrieved", required = false)
            @QueryParam("category")
            final String category) {
        try {
            Collection<Tag> tags = definitions.getTriggerTags(tenantId, triggerId, category);
            log.debugf("Tags: " + tags);
            return ResponseUtil.ok(tags);
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    private boolean isEmpty(String s) {
        return null == s || s.trim().isEmpty();
    }

}
