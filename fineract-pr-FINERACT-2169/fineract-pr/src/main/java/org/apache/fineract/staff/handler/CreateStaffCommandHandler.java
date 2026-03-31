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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.command.CommandHandler;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.staff.command.CreateStaffCommand;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepository;
import org.apache.fineract.organisation.staff.exception.StaffExternalIdAlreadyExistsException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles {@link CreateStaffCommand} — creates a new Staff member for an office.
 *
 * <p>This handler replaces the legacy {@code StaffWritePlatformServiceJpaRepositoryImpl#createStaff}
 * path that was previously invoked via {@code CommandWrapperBuilder.createStaff()} +
 * {@code JsonCommand}. The business logic (duplicate external ID check, office lookup,
 * entity construction) is unchanged; only the command dispatch mechanism has been migrated.
 *
 * <p>Part of the FINERACT-2169 command infrastructure migration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateStaffCommandHandler implements CommandHandler<CreateStaffCommand> {

    private final OfficeRepositoryWrapper officeRepository;
    private final StaffRepository staffRepository;

    /**
     * {@inheritDoc}
     *
     * @throws StaffExternalIdAlreadyExistsException if the {@code externalId} is already in use
     *     within the tenant.
     * @throws org.apache.fineract.organisation.office.exception.OfficeNotFoundException if
     *     {@code officeId} does not exist.
     */
    @Override
    @Transactional
    public CommandProcessingResult handle(@Valid CreateStaffCommand command) {
        log.debug("Handling CreateStaffCommand for office={}", command.officeId());

        // 1. Validate external ID uniqueness (if provided)
        if (command.externalId() != null && !command.externalId().isBlank()) {
            staffRepository.findByExternalId(command.externalId()).ifPresent(existing -> {
                throw new StaffExternalIdAlreadyExistsException(command.externalId());
            });
        }

        // 2. Resolve the parent office — throws OfficeNotFoundException if missing
        final Office office = officeRepository.findOneWithNotFoundDetection(command.officeId());

        // 3. Build and persist the Staff entity
        final Staff staff = Staff.createNew(
                office,
                command.firstname(),
                command.lastname(),
                command.isLoanOfficer(),
                command.externalId(),
                command.mobileNo(),
                command.joiningDate(),
                command.isActive()
        );

        staffRepository.saveAndFlush(staff);

        log.info("Staff created: id={}, name={} {}, officeId={}",
                staff.getId(), staff.getFirstname(), staff.getLastname(), command.officeId());

        return new CommandProcessingResultBuilder()
                .withCommandId(null)
                .withEntityId(staff.getId())
                .withOfficeId(office.getId())
                .build();
    }
}
