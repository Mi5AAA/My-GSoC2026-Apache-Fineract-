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
package org.apache.fineract.organisation.staff.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.apache.fineract.infrastructure.core.command.CommandRequest;

/**
 * Typed command for creating a new Staff member.
 *
 * <p>Replaces the legacy {@code CommandWrapperBuilder.createStaff()} / {@code JsonCommand} pattern
 * as part of the FINERACT-2169 command infrastructure migration.
 *
 * <p>All fields are validated via Jakarta Bean Validation before the command reaches the handler.
 * This eliminates the manual JSON parsing and ad-hoc validation that existed in the legacy
 * {@code StaffCommandFromApiJsonDeserializer}.
 */
public record CreateStaffCommand(

        @NotNull(message = "officeId is mandatory")
        Long officeId,

        @NotBlank(message = "firstname is mandatory")
        @Size(max = 50, message = "firstname must not exceed 50 characters")
        String firstname,

        @NotBlank(message = "lastname is mandatory")
        @Size(max = 50, message = "lastname must not exceed 50 characters")
        String lastname,

        /**
         * Optional external identifier (e.g. HR system ID). Must be unique across the tenant when
         * provided.
         */
        @Size(max = 100, message = "externalId must not exceed 100 characters")
        String externalId,

        /** Whether this staff member can be assigned as a loan officer on loan accounts. */
        boolean isLoanOfficer,

        /** Mobile number for field communication. Optional. */
        @Size(max = 50, message = "mobileNo must not exceed 50 characters")
        String mobileNo,

        /** Date the staff member joined the organisation. Defaults to today when null. */
        LocalDate joiningDate,

        /** Whether the staff record is active. Inactive staff cannot be assigned to new loans. */
        boolean isActive

) implements CommandRequest {

    /**
     * Convenience factory for the common case: create an active loan officer with no external ID.
     */
    public static CreateStaffCommand loanOfficer(Long officeId, String firstname, String lastname) {
        return new CreateStaffCommand(
                officeId, firstname, lastname,
                null,   // externalId
                true,   // isLoanOfficer
                null,   // mobileNo
                LocalDate.now(),
                true    // isActive
        );
    }
}
