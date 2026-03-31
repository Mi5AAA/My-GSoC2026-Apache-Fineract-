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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.staff.command.CreateStaffCommand;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepository;
import org.apache.fineract.organisation.staff.exception.StaffExternalIdAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CreateStaffCommandHandler}.
 *
 * <p>No Spring context — pure Mockito. Fast, isolated, no DB required.
 */
@ExtendWith(MockitoExtension.class)
class CreateStaffCommandHandlerTest {

    @Mock
    private OfficeRepositoryWrapper officeRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private CreateStaffCommandHandler handler;

    private Office mockOffice;
    private Staff savedStaff;

    @BeforeEach
    void setUp() {
        mockOffice = new Office();
        mockOffice.setId(1L);
        mockOffice.setName("Head Office");

        savedStaff = new Staff();
        savedStaff.setId(42L);
        savedStaff.setFirstname("Amina");
        savedStaff.setLastname("Hassan");
        savedStaff.setOffice(mockOffice);
    }

    @Test
    @DisplayName("Creates staff when all fields are valid")
    void handle_validCommand_returnsResultWithStaffId() {
        // arrange
        final CreateStaffCommand command = new CreateStaffCommand(
                1L, "Amina", "Hassan", null, true, null, LocalDate.of(2026, 1, 1), true
        );
        given(officeRepository.findOneWithNotFoundDetection(1L)).willReturn(mockOffice);
        given(staffRepository.saveAndFlush(any(Staff.class))).willReturn(savedStaff);

        // act
        final CommandProcessingResult result = handler.handle(command);

        // assert
        assertThat(result.resourceId()).isEqualTo(42L);
        assertThat(result.officeId()).isEqualTo(1L);
        verify(staffRepository).saveAndFlush(any(Staff.class));
    }

    @Test
    @DisplayName("Rejects duplicate externalId before touching DB")
    void handle_duplicateExternalId_throwsException() {
        // arrange
        final CreateStaffCommand command = new CreateStaffCommand(
                1L, "Omar", "Said", "EXT-001", false, null, null, true
        );
        final Staff existingStaff = new Staff();
        existingStaff.setId(99L);
        given(staffRepository.findByExternalId("EXT-001")).willReturn(Optional.of(existingStaff));

        // act & assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(StaffExternalIdAlreadyExistsException.class);

        // The office lookup and save must NOT be called when externalId collision is detected
        verify(officeRepository, never()).findOneWithNotFoundDetection(any());
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Skips externalId check when externalId is null")
    void handle_nullExternalId_doesNotCheckDuplicates() {
        // arrange
        final CreateStaffCommand command = CreateStaffCommand.loanOfficer(1L, "Fatima", "Ali");
        given(officeRepository.findOneWithNotFoundDetection(1L)).willReturn(mockOffice);
        given(staffRepository.saveAndFlush(any(Staff.class))).willReturn(savedStaff);

        // act
        handler.handle(command);

        // assert: findByExternalId must never be called when externalId is null
        verify(staffRepository, never()).findByExternalId(anyString());
    }

    @Test
    @DisplayName("Skips externalId check when externalId is blank string")
    void handle_blankExternalId_doesNotCheckDuplicates() {
        // arrange
        final CreateStaffCommand command = new CreateStaffCommand(
                1L, "Yusuf", "Ibrahim", "   ", true, null, null, true
        );
        given(officeRepository.findOneWithNotFoundDetection(1L)).willReturn(mockOffice);
        given(staffRepository.saveAndFlush(any(Staff.class))).willReturn(savedStaff);

        // act
        handler.handle(command);

        // assert
        verify(staffRepository, never()).findByExternalId(anyString());
    }
}
