# FINERACT-2169: Migrate Staff module to new typed command infrastructure

## Description

This PR migrates the **Staff module** from the legacy `JsonCommand` / `CommandWrapperBuilder` 
pipeline to the new typed command bus introduced in the `fineract-command` module 
(tracked under FINERACT-2169).

The Staff module was identified as one of the unassigned, self-contained modules — 
making it an ideal next migration candidate after the completed Client and Loan modules.

---

## What changed

### New files

| File | Purpose |
|------|---------|
| `CreateStaffCommand.java` | Immutable Java record — typed command for creating a staff member |
| `UpdateStaffCommand.java` | Immutable Java record — typed command for updating a staff member |
| `CreateStaffCommandHandler.java` | `@CommandHandler` wired to the command bus for create |
| `UpdateStaffCommandHandler.java` | `@CommandHandler` wired to the command bus for update |
| `CreateStaffCommandHandlerTest.java` | Unit test — handler logic in isolation |
| `UpdateStaffCommandHandlerTest.java` | Unit test — handler logic in isolation |
| `StaffCommandIntegrationTest.java` | Testcontainers integration test — full HTTP → DB round-trip |

### Modified files

| File | Change |
|------|--------|
| `StaffApiResource.java` | Route `POST /staff` and `PUT /staff/{staffId}` through new typed handlers |
| `build.gradle` (staff module) | Add `fineract-command` module dependency |

### NOT changed (intentional)

The legacy `StaffWritePlatformServiceJpaRepositoryImpl` is **still in place** as the 
delegate. The new handlers call it internally. This preserves backward compatibility 
and allows a clean rollback path. The legacy `CommandWrapperBuilder` path in 
`StaffApiResource` has been removed — the new typed path replaces it.

---

## Checklist

- [x] Follows the migration pattern established by Client/Loan modules  
- [x] All new command records use Java 21 `record` syntax with Bean Validation annotations  
- [x] Unit tests use Mockito — no Spring context loaded  
- [x] Integration test uses Testcontainers (MariaDB) + `@SpringBootTest`  
- [x] No raw `JsonCommand` usage in new code  
- [x] `@SuppressWarnings("unused")` removed from handlers — all fields used  
- [x] Checkstyle and SpotBugs pass locally  
- [x] SonarQube: no new issues introduced  

---

## Testing

```bash
# Run unit tests only
./gradlew :fineract-provider:test --tests "*.staff.*"

# Run integration test (requires Docker for Testcontainers)
./gradlew :fineract-provider:test --tests "*.StaffCommandIntegrationTest"

# Full CI check
./gradlew build
```

---

## Related

- JIRA: https://issues.apache.org/jira/browse/FINERACT-2169  
- Parent epic: New command processing infrastructure  
- Follows pattern from: PR #4821 (Client module), PR #4876 (Loan module)

---

## Reviewer notes

The `@CommandHandler` annotation wiring is registered in `StaffCommandConfiguration.java` 
(new file). The command bus discovers handlers automatically via component scan — 
no manual registration needed in `ApplicationContext`.

If you want to test the full flow manually:

```bash
# Create a staff member via the new typed path
curl -k -X POST https://localhost:8443/fineract-provider/api/v1/staff \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -H "Authorization: Basic bWlmb3M6cGFzc3dvcmQ=" \
  -d '{
    "firstname": "Amina",
    "lastname": "Hassan",
    "officeId": 1,
    "isLoanOfficer": true,
    "isActive": true
  }'
```
