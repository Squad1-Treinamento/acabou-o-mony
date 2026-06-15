---
id: task-012
status: completed
links:
  - spec/tasks/index.md
  - spec/specs/spec-004-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-004-cache-and-idempotency-layer.md
  - spec/tasks/task-001-add-cache-maven-dependencies.md
---

# Add Bouncy Castle Encryption Dependency to Core Service

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Modify:**
- `pom.xml` (Maven dependencies file in Core Service root)

**Dependency to Add:**
- `bcprov-jdk18on` (Bouncy Castle Provider for JDK 18+)
- Version: 1.77 (latest stable as of 2024)

**Purpose:**
- Prepare encryption infrastructure for future use (AES-256-GCM)
- Not used in MVP (flag `CACHE_ENCRYPT_SENSITIVE_DATA=false` by default)
- Required for `AESEncryptor` class (task-013)

## Scope

1. Open `pom.xml` in Core Service module
2. Add Bouncy Castle dependency:
   - Group ID: `org.bouncycastle`
   - Artifact ID: `bcprov-jdk18on`
   - Version: `1.77`
3. Verify Maven build succeeds with `mvn clean install`
4. Verify dependency is in classpath

**Implementation Notes:**
- Bouncy Castle provides advanced cryptographic algorithms (AES-GCM)
- Required for `AESEncryptor` class (task-013)
- Not used in MVP (infrastructure preparation only)
- Version 1.77 is compatible with JDK 18+ (adjust if using older JDK)

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: Dependency Added**
- `pom.xml` contains `bcprov-jdk18on` dependency
- Group ID: `org.bouncycastle`
- Artifact ID: `bcprov-jdk18on`
- Version: `1.77`

**AC-2: Build Success**
- Maven build completes without errors: `mvn clean install`
- No dependency conflicts reported
- Bouncy Castle JAR is in classpath

**AC-3: IDE Recognition**
- IDE (IntelliJ/Eclipse) recognizes Bouncy Castle dependency
- No red underlines in import statements for `org.bouncycastle.*` classes

### Failure Cases

- Maven build fails with dependency resolution errors
- Version conflict with existing dependencies
- Missing repository configuration (Bouncy Castle is in Maven Central)

### Validation Commands

```bash
# Build project
mvn clean install

# Verify dependency is in classpath
mvn dependency:tree | grep bouncycastle

# Expected output:
# [INFO] +- org.bouncycastle:bcprov-jdk18on:jar:1.77:compile
```

## Constraints and Negative Instructions

**DO:**
- Use explicit version `1.77` (latest stable)
- Add dependency in `<dependencies>` section (not `<dependencyManagement>`)
- Verify build succeeds after adding dependency

**DO NOT:**
- Use older Bouncy Castle versions (e.g., `bcprov-jdk15on`)
- Add test dependencies in this task (handled separately)
- Implement encryption logic yet (task-013)
- Enable encryption in configuration (task-014)

**Out of Scope:**
- `AESEncryptor` implementation - task-013
- Encryption configuration - task-014
- Encryption usage in cache layer (not used in MVP)
- Integration tests - optional
