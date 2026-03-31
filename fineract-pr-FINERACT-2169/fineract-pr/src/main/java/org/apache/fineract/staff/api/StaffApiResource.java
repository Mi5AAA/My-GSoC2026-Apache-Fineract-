/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.organisation.staff.api;

import com.google.gson.JsonElement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.command.CommandBus;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.staff.command.CreateStaffCommand;
import org.apache.fineract.organisation.staff.command.UpdateStaffCommand;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.organisation.staff.service.StaffReadPlatformService;
import org.springframework.stereotype.Component;

/**
 * REST resource for Staff management.
 *
 * <p>Write operations (POST, PUT) are now routed through the typed {@link CommandBus} as part of
 * the FINERACT-2169 command infrastructure migration. Read operations are unchanged.
 *
 * <p>MIGRATION NOTE: The previous {@code CommandWrapperBuilder.createStaff()} / {@code
 * CommandWrapperBuilder.updateStaff()} calls have been replaced with {@link CreateStaffCommand}
 * and {@link UpdateStaffCommand} records dispatched via {@link CommandBus#dispatch}.
 */
@Path("/v1/staff")
@Component
@Tag(name = "Staff", description = "Allows you to model staff members.")
@RequiredArgsConstructor
public class StaffApiResource {

    private final StaffReadPlatformService readPlatformService;
    private final CommandBus commandBus;
    private final FromJsonHelper fromJsonHelper;

    // ─── READ (unchanged) ────────────────────────────────────────────────────

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Staff", description = "Returns the list of staff members.")
    public String retrieveAll(
            @Context UriInfo uriInfo,
            @QueryParam("officeId") @Parameter(description = "officeId") final Long officeId,
            @QueryParam("staffInOfficeHierarchy") final boolean staffInOfficeHierarchy,
            @QueryParam("isActive") final boolean isActive,
            @QueryParam("status") final String status) {

        // Read path unchanged — StaffReadPlatformService is not part of the migration
        return readPlatformService.retrieveAll(officeId, staffInOfficeHierarchy, isActive, status);
    }

    @GET
    @Path("{staffId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a Staff Member")
    public StaffData retrieveOne(
            @PathParam("staffId") @Parameter(description = "staffId") final Long staffId) {
        return readPlatformService.retrieveStaff(staffId);
    }

    // ─── WRITE (migrated to typed command bus) ────────────────────────────────

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create a Staff Member",
            description = "Creates a Staff Member. Note: A Staff Member may be linked to a Loan Officer.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
                content = @Content(schema = @Schema(implementation = StaffApiResourceSwagger.PostStaffResponse.class)))
    })
    public CommandProcessingResult create(
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        // FINERACT-2169: Parse JSON into typed CreateStaffCommand and dispatch via CommandBus
        // Previously: commandsSourceWritePlatformService.logCommandSource(
        //     new CommandWrapperBuilder().createStaff().withJson(apiRequestBodyAsJson).build())
        final JsonElement root = fromJsonHelper.parse(apiRequestBodyAsJson);

        final CreateStaffCommand command = new CreateStaffCommand(
                fromJsonHelper.extractLongNamed("officeId", root),
                fromJsonHelper.extractStringNamed("firstname", root),
                fromJsonHelper.extractStringNamed("lastname", root),
                fromJsonHelper.extractStringNamed("externalId", root),
                fromJsonHelper.extractBooleanNamed("isLoanOfficer", root, false),
                fromJsonHelper.extractStringNamed("mobileNo", root),
                fromJsonHelper.extractLocalDateNamed("joiningDate", root),
                fromJsonHelper.extractBooleanNamed("isActive", root, true)
        );

        return commandBus.dispatch(command);
    }

    @PUT
    @Path("{staffId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update a Staff Member")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
                content = @Content(schema = @Schema(implementation = StaffApiResourceSwagger.PutStaffStaffIdResponse.class)))
    })
    public CommandProcessingResult update(
            @PathParam("staffId") @Parameter(description = "staffId") final Long staffId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        // FINERACT-2169: Parse JSON into typed UpdateStaffCommand and dispatch via CommandBus
        // Previously: commandsSourceWritePlatformService.logCommandSource(
        //     new CommandWrapperBuilder().updateStaff(staffId).withJson(apiRequestBodyAsJson).build())
        final JsonElement root = fromJsonHelper.parse(apiRequestBodyAsJson);

        final UpdateStaffCommand command = new UpdateStaffCommand(
                staffId,
                fromJsonHelper.extractLongNamed("officeId", root),
                fromJsonHelper.extractStringNamed("firstname", root),
                fromJsonHelper.extractStringNamed("lastname", root),
                fromJsonHelper.extractStringNamed("externalId", root),
                fromJsonHelper.extractBooleanNamed("isLoanOfficer", root),
                fromJsonHelper.extractStringNamed("mobileNo", root),
                fromJsonHelper.extractLocalDateNamed("joiningDate", root),
                fromJsonHelper.extractBooleanNamed("isActive", root)
        );

        return commandBus.dispatch(command);
    }
}
