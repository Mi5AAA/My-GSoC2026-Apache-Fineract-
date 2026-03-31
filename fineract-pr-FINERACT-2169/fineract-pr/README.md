# Apache Fineract — PR Files (FINERACT-2169)
## Staff Module → Typed Command Infrastructure Migration

This package contains all the files needed to open a real GitHub Pull Request
against `apache/fineract` on the `develop` branch.

---

## File structure

```
fineract-pr/
│
├── PULL_REQUEST.md                          ← Paste this as your GitHub PR description
├── COMMIT_MESSAGE.txt                       ← Use this as your git commit message
├── build.gradle                             ← Changed: adds fineract-command dependency
│
├── src/main/java/org/apache/fineract/
│   └── organisation/staff/
│       ├── command/
│       │   ├── CreateStaffCommand.java      ← NEW: typed Java record for create
│       │   └── UpdateStaffCommand.java      ← NEW: typed Java record for update
│       ├── handler/
│       │   ├── CreateStaffCommandHandler.java  ← NEW: @CommandHandler for create
│       │   └── UpdateStaffCommandHandler.java  ← NEW: @CommandHandler for update
│       └── api/
│           └── StaffApiResource.java        ← CHANGED: routes POST/PUT through CommandBus
│
└── src/test/java/org/apache/fineract/
    └── organisation/staff/
        ├── CreateStaffCommandHandlerTest.java  ← NEW: unit test (Mockito)
        ├── UpdateStaffCommandHandlerTest.java  ← NEW: unit test (Mockito)
        └── StaffCommandIntegrationTest.java    ← NEW: Testcontainers integration test
```

---

## How to open the PR

```bash
# 1. Fork the repo on GitHub, then clone your fork
git clone https://github.com/YOUR_USERNAME/fineract.git
cd fineract

# 2. Create a feature branch named after the JIRA ticket
git checkout -b FINERACT-2169-staff-command-migration

# 3. Copy the new source files into the correct locations
#    (adjust the path prefix to match the actual module layout)
cp CreateStaffCommand.java \
   fineract-provider/src/main/java/org/apache/fineract/organisation/staff/command/

cp UpdateStaffCommand.java \
   fineract-provider/src/main/java/org/apache/fineract/organisation/staff/command/

cp CreateStaffCommandHandler.java \
   fineract-provider/src/main/java/org/apache/fineract/organisation/staff/handler/

cp UpdateStaffCommandHandler.java \
   fineract-provider/src/main/java/org/apache/fineract/organisation/staff/handler/

# 4. Apply the StaffApiResource.java changes manually
#    (open the existing file and replace the create/update methods)

# 5. Add the fineract-command dependency to build.gradle

# 6. Run CI checks locally before pushing
./gradlew :fineract-provider:test --tests "*.staff.*"
./gradlew build -x integrationTest

# 7. GPG-sign and commit
git add .
git commit -S -m "$(cat COMMIT_MESSAGE.txt)"

# 8. Push and open PR against apache/fineract:develop
git push origin FINERACT-2169-staff-command-migration
```

Then open the PR on GitHub and paste the contents of `PULL_REQUEST.md`
into the PR description box.

---

## Checklist before pushing

- [ ] `./gradlew build` passes (44/44 CI checks)
- [ ] `./gradlew checkstyleMain` passes
- [ ] `./gradlew spotbugsMain` passes
- [ ] No `System.out.println` in any file
- [ ] Apache License header present in every new `.java` file
- [ ] Commit is GPG-signed (`git commit -S`)
- [ ] PR is opened against the `develop` branch (not `master`)
- [ ] JIRA ticket FINERACT-2169 is mentioned in PR title

---

## Reviewer / mentor contacts

- Mailing list: dev@fineract.apache.org
- GSoC Matrix room: https://matrix.to/#/#apache-fineract-gsoc:matrix.org
- JIRA: https://issues.apache.org/jira/browse/FINERACT-2169
