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
package org.apache.fineract.organisation.staff.handler;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.command.CommandHandler;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.organisation.staff.command.UpdateStaffCommand;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepository;
import org.apache.fineract.organisation.staff.exception.StaffHasLoansException;
import org.apache.fineract.organisation.staff.exception.StaffNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles {@link UpdateStaffCommand} — applies partial updates to an existing Staff member.
 *
 * <p>Only non-null fields in the command are applied (patch semantics). This is a deliberate
 * improvement over the legacy implementation which required full payloads on every update.
 *
 * <p>Part of the FINERACT-2169 command infrastructure migration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateStaffCommandHandler implements CommandHandler<UpdateStaffCommand> {

    private final StaffRepository staffRepository;

    @Override
    @Transactional
    public CommandProcessingResult handle(@Valid UpdateStaffCommand command) {
        log.debug("Handling UpdateStaffCommand staffId={}", command.staffId());

        final Staff staff = staffRepository.findById(command.staffId())
                .orElseThrow(() -> new StaffNotFoundException(command.staffId()));

        // Guard: cannot deactivate a staff member with active loan assignments
        if (Boolean.FALSE.equals(command.isActive()) && staff.isActive()) {
            long activeLoans = staffRepository.countActiveLoanAssignments(staff.getId());
            if (activeLoans > 0) {
                throw new StaffHasLoansException(staff.getId(), activeLoans);
            }
        }

        // Apply only the non-null fields (patch semantics)
        final Map<String, Object> changes = new HashMap<>();

        if (command.firstname() != null && !command.firstname().equals(staff.getFirstname())) {
            staff.setFirstname(command.firstname());
            changes.put("firstname", command.firstname());
        }
        if (command.lastname() != null && !command.lastname().equals(staff.getLastname())) {
            staff.setLastname(command.lastname());
            changes.put("lastname", command.lastname());
        }
        if (command.mobileNo() != null && !command.mobileNo().equals(staff.getMobileNo())) {
            staff.setMobileNo(command.mobileNo());
            changes.put("mobileNo", command.mobileNo());
        }
        if (command.isLoanOfficer() != null && !command.isLoanOfficer().equals(staff.isLoanOfficer())) {
            staff.setLoanOfficer(command.isLoanOfficer());
            changes.put("isLoanOfficer", command.isLoanOfficer());
        }
        if (command.isActive() != null && !command.isActive().equals(staff.isActive())) {
            staff.setActive(command.isActive());
            changes.put("isActive", command.isActive());
        }
        if (command.joiningDate() != null && !command.joiningDate().equals(staff.getJoiningDate())) {
            staff.setJoiningDate(command.joiningDate());
            changes.put("joiningDate", command.joiningDate());
        }

        if (!changes.isEmpty()) {
            staffRepository.saveAndFlush(staff);
            log.info("Staff updated: id={}, changes={}", staff.getId(), changes.keySet());
        } else {
            log.debug("UpdateStaffCommand: no changes detected for staffId={}", command.staffId());
        }

        return new CommandProcessingResultBuilder()
                .withEntityId(staff.getId())
                .withOfficeId(staff.getOffice().getId())
                .with(changes)
                .build();
    }
}
