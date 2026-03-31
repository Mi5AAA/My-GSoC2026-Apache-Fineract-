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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.staff.command.UpdateStaffCommand;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepository;
import org.apache.fineract.organisation.staff.exception.StaffHasLoansException;
import org.apache.fineract.organisation.staff.exception.StaffNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link UpdateStaffCommandHandler}.
 */
@ExtendWith(MockitoExtension.class)
class UpdateStaffCommandHandlerTest {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private UpdateStaffCommandHandler handler;

    private Staff existingStaff;

    @BeforeEach
    void setUp() {
        final Office office = new Office();
        office.setId(1L);

        existingStaff = new Staff();
        existingStaff.setId(10L);
        existingStaff.setFirstname("Amina");
        existingStaff.setLastname("Hassan");
        existingStaff.setLoanOfficer(true);
        existingStaff.setActive(true);
        existingStaff.setOffice(office);
    }

    @Test
    @DisplayName("Partial update: only changed fields are applied")
    void handle_partialUpdate_onlyChangedFieldsPersisted() {
        // arrange — only lastname changes
        final UpdateStaffCommand command = new UpdateStaffCommand(
                10L,
                null,          // officeId — no change
                null,          // firstname — no change
                "Hassan-Osei", // lastname — changed
                null, null, null, null, null
        );
        given(staffRepository.findById(10L)).willReturn(Optional.of(existingStaff));
        given(staffRepository.saveAndFlush(any())).willReturn(existingStaff);

        // act
        final CommandProcessingResult result = handler.handle(command);

        // assert
        assertThat(existingStaff.getLastname()).isEqualTo("Hassan-Osei");
        assertThat(existingStaff.getFirstname()).isEqualTo("Amina"); // unchanged
        assertThat(result.getChanges()).containsKey("lastname");
        assertThat(result.getChanges()).doesNotContainKey("firstname");
    }

    @Test
    @DisplayName("No-op when command contains no changed values")
    void handle_noChanges_doesNotCallSave() {
        // arrange — command values identical to existing entity
        final UpdateStaffCommand command = new UpdateStaffCommand(
                10L, null, "Amina", "Hassan", null, true, null, null, true
        );
        given(staffRepository.findById(10L)).willReturn(Optional.of(existingStaff));

        // act
        final CommandProcessingResult result = handler.handle(command);

        // assert — save must NOT be called for a no-op update
        verify(staffRepository, never()).saveAndFlush(any());
        assertThat(result.getChanges()).isEmpty();
    }

    @Test
    @DisplayName("Throws StaffNotFoundException for unknown staffId")
    void handle_unknownStaffId_throwsNotFoundException() {
        given(staffRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(UpdateStaffCommand.setActiveStatus(999L, false)))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    @DisplayName("Throws StaffHasLoansException when deactivating staff with active loans")
    void handle_deactivateStaffWithLoans_throwsException() {
        // arrange
        given(staffRepository.findById(10L)).willReturn(Optional.of(existingStaff));
        given(staffRepository.countActiveLoanAssignments(10L)).willReturn(3L);

        final UpdateStaffCommand command = UpdateStaffCommand.setActiveStatus(10L, false);

        // act & assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(StaffHasLoansException.class);

        // entity must not have been mutated or saved
        assertThat(existingStaff.isActive()).isTrue();
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deactivates staff with no active loan assignments successfully")
    void handle_deactivateStaffWithNoLoans_succeeds() {
        // arrange
        given(staffRepository.findById(10L)).willReturn(Optional.of(existingStaff));
        given(staffRepository.countActiveLoanAssignments(10L)).willReturn(0L);
        given(staffRepository.saveAndFlush(any())).willReturn(existingStaff);

        // act
        handler.handle(UpdateStaffCommand.setActiveStatus(10L, false));

        // assert
        assertThat(existingStaff.isActive()).isFalse();
        verify(staffRepository).saveAndFlush(existingStaff);
    }
}
