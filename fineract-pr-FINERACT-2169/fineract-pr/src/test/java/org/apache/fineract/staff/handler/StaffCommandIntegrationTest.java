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
package org.apache.fineract.organisation.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for the Staff command migration (FINERACT-2169).
 *
 * <p>Verifies the full HTTP → CommandBus → Handler → DB round-trip using a real MariaDB instance
 * managed by Testcontainers. No mocks — everything runs as in production.
 *
 * <p>The test uses {@code @Transactional} so each test case rolls back, keeping the DB clean.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StaffCommandIntegrationTest {

    @Container
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.5")
            .withDatabaseName("fineract_default")
            .withUsername("root")
            .withPassword("mysql");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffRepository staffRepository;

    private static final String AUTH_HEADER = "Basic bWlmb3M6cGFzc3dvcmQ="; // mifos:password
    private static final String TENANT_HEADER = "default";

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /staff creates staff and returns resourceId")
    void createStaff_validPayload_returns200WithResourceId() throws Exception {
        final String payload = """
                {
                    "officeId": 1,
                    "firstname": "Amina",
                    "lastname": "Hassan",
                    "isLoanOfficer": true,
                    "isActive": true,
                    "joiningDate": "01 January 2026",
                    "dateFormat": "dd MMMM yyyy",
                    "locale": "en"
                }
                """;

        mockMvc.perform(post("/fineract-provider/api/v1/staff")
                        .header("Authorization", AUTH_HEADER)
                        .header("Fineract-Platform-TenantId", TENANT_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").isNumber())
                .andExpect(jsonPath("$.officeId").value(1));
    }

    @Test
    @DisplayName("POST /staff persists staff to DB with correct values")
    void createStaff_validPayload_persistsEntityToDatabase() throws Exception {
        final String payload = """
                {
                    "officeId": 1,
                    "firstname": "Fatima",
                    "lastname": "Yusuf",
                    "isLoanOfficer": false,
                    "isActive": true,
                    "locale": "en"
                }
                """;

        final MvcResult result = mockMvc.perform(post("/fineract-provider/api/v1/staff")
                        .header("Authorization", AUTH_HEADER)
                        .header("Fineract-Platform-TenantId", TENANT_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        // Extract the created staff ID from the response body
        final String body = result.getResponse().getContentAsString();
        final long staffId = Long.parseLong(body.replaceAll(".*\"resourceId\":(\\d+).*", "$1"));

        // Verify the entity was actually written to the DB
        final Staff saved = staffRepository.findById(staffId).orElseThrow();
        assertThat(saved.getFirstname()).isEqualTo("Fatima");
        assertThat(saved.getLastname()).isEqualTo("Yusuf");
        assertThat(saved.isLoanOfficer()).isFalse();
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("POST /staff rejects payload missing required officeId")
    void createStaff_missingOfficeId_returns400() throws Exception {
        final String payload = """
                {
                    "firstname": "Omar",
                    "lastname": "Said"
                }
                """;

        mockMvc.perform(post("/fineract-provider/api/v1/staff")
                        .header("Authorization", AUTH_HEADER)
                        .header("Fineract-Platform-TenantId", TENANT_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /staff rejects duplicate externalId")
    void createStaff_duplicateExternalId_returns409() throws Exception {
        final String payload = """
                {
                    "officeId": 1,
                    "firstname": "Yusuf",
                    "lastname": "Ibrahim",
                    "externalId": "EXT-DUPLICATE-001",
                    "isLoanOfficer": true,
                    "isActive": true,
                    "locale": "en"
                }
                """;

        // First request — should succeed
        mockMvc.perform(post("/fineract-provider/api/v1/staff")
                        .header("Authorization", AUTH_HEADER)
                        .header("Fineract-Platform-TenantId", TENANT_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // Second request with same externalId — should fail
        mockMvc.perform(post("/fineract-provider/api/v1/staff")
                        .header("Authorization", AUTH_HEADER)
                        .header("Fineract-Platform-TenantId", TENANT_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /staff/{staffId} updates only the supplied fields")
    void updateStaff_partialPayload_updatesOnlyChangedFields() throws Exception {
        // First create a staff member
        final String createPayload = """
                {
                    "officeId": 1,
                    "firstname": "Original",
                    "lastname": "Name",
                    "isLoanOfficer": false,
                    "isActive": true,
                    "locale": "en"
                }
                """;

        final MvcResult createResult = mockMvc.perform(post("/fineract-provider/api/v1/staff")
                        .header("Authorization", AUTH_HEADER)
                        .header("Fineract-Platform-TenantId", TENANT_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isOk())
                .andReturn();

        final long staffId = Long.parseLong(
                createResult.getResponse().getContentAsString()
                        .replaceAll(".*\"resourceId\":(\\d+).*", "$1"));

        // Now update only the lastname
        final String updatePayload = """
                {
                    "lastname": "Updated",
                    "locale": "en"
                }
                """;

        mockMvc.perform(put("/fineract-provider/api/v1/staff/" + staffId)
                        .header("Authorization", AUTH_HEADER)
                        .header("Fineract-Platform-TenantId", TENANT_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changes.lastname").value("Updated"));

        // Verify firstname was not changed
        final Staff updated = staffRepository.findById(staffId).orElseThrow();
        assertThat(updated.getFirstname()).isEqualTo("Original");
        assertThat(updated.getLastname()).isEqualTo("Updated");
    }
}
