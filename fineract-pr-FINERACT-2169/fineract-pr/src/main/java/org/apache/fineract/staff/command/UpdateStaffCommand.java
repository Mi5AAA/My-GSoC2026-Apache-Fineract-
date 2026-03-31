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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.apache.fineract.infrastructure.core.command.CommandRequest;

/**
 * Typed command for updating an existing Staff member.
 *
 * <p>All fields except {@code staffId} are optional (nullable). The handler applies
 * only the non-null fields — equivalent to a JSON PATCH semantics. This avoids the
 * "set everything on every update" anti-pattern that existed in the legacy service.
 *
 * <p>Part of the FINERACT-2169 command infrastructure migration.
 */
public record UpdateStaffCommand(

        @NotNull(message = "staffId is mandatory for update")
        Long staffId,

        /** New office to transfer the staff member to. Null = no change. */
        Long officeId,

        /** Updated first name. Null = no change. */
        @Size(max = 50, message = "firstname must not exceed 50 characters")
        String firstname,

        /** Updated last name. Null = no change. */
        @Size(max = 50, message = "lastname must not exceed 50 characters")
        String lastname,

        /** Updated external identifier. Null = no change. Empty string = clear the value. */
        @Size(max = 100, message = "externalId must not exceed 100 characters")
        String externalId,

        /** Change loan officer designation. Null = no change. */
        Boolean isLoanOfficer,

        /** Updated mobile number. Null = no change. */
        @Size(max = 50, message = "mobileNo must not exceed 50 characters")
        String mobileNo,

        /** Updated joining date. Null = no change. */
        LocalDate joiningDate,

        /**
         * Activate or deactivate the staff member. Null = no change.
         *
         * <p>Deactivating a staff member who has active loan assignments will throw
         * {@code StaffHasLoansException} in the handler.
         */
        Boolean isActive

) implements CommandRequest {

    /**
     * Convenience factory: update only the active flag (e.g. for soft-delete / reinstatement
     * flows).
     */
    public static UpdateStaffCommand setActiveStatus(Long staffId, boolean active) {
        return new UpdateStaffCommand(
                staffId, null, null, null, null, null, null, null, active
        );
    }
}
