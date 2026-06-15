---
id: task-001
status: in_progress
links:
  - spec/tasks/index.md
  - spec/specs/spec-004-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-004-cache-and-idempotency-layer.md
---

# Add Cache Maven Dependencies to Core Service

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Modify:**
- `pom.xml` (Maven dependencies file in Core Service root)

**Dependencies to Add:**
- `spring-boot-starter-cache` (Spring Cache Abstraction)
- `spring-boot-starter-data-redis-reactive` (Redis Reactive support)
- `resilience4j-spring-boot3` (Circuit Breaker)
- `resilience4j-reactor` (Circuit Breaker for Reactive streams)

**Local Dependencies:**
- Existing Spring Boot parent POM
- Existing Spring Boot version management

## Scope

1. Open `pom.xml` in Core Service module
2. Add Spring Cache Abstraction dependency (no version needed, managed by Spring Boot)
3. Add Spring Data Redis Reactive dependency (no version needed)
4. Add Resilience4j Spring Boot 3 dependency (version: 2.1.0)
5. Add Resilience4j Reactor dependency (version: 2.1.0)
6. Verify Maven build succeeds with `mvn clean install`

**Implementation Notes:**
- All Spring Boot starters inherit version from parent POM
- Resilience4j requires explicit version (2.1.0)
- No configuration changes needed in this task (handled in task-002)

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: Dependencies Added**
- `pom.xml` contains `spring-boot-starter-cache` dependency
- `pom.xml` contains `spring-boot-starter-data-redis-reactive` dependency
- `pom.xml` contains `resilience4j-spring-boot3` dependency with version 2.1.0
- `pom.xml` contains `resilience4j-reactor` dependency with version 2.1.0

**AC-2: Build Success**
- Maven build completes without errors: `mvn clean install`
- No dependency conflicts reported
- All transitive dependencies resolve correctly

**AC-3: IDE Recognition**
- IDE (IntelliJ/Eclipse) recognizes new dependencies
- No red underlines in import statements for cache/redis/resilience4j classes

### Failure Cases

- Maven build fails with dependency resolution errors
- Version conflicts with existing Spring Boot dependencies
- Missing repository configuration for Resilience4j

### Validation Commands

```bash
# Build project
mvn clean install

# Verify dependencies are in classpath
mvn dependency:tree | grep -E "spring-boot-starter-cache|redis-reactive|resilience4j"

# Expected output should include:
# [INFO] +- org.springframework.boot:spring-boot-starter-cache:jar:3.x.x
# [INFO] +- org.springframework.boot:spring-boot-starter-data-redis-reactive:jar:3.x.x
# [INFO] +- io.github.resilience4j:resilience4j-spring-boot3:jar:2.1.0
# [INFO] +- io.github.resilience4j:resilience4j-reactor:jar:2.1.0
```

## Constraints and Negative Instructions

**DO:**
- Use Spring Boot managed versions for Spring dependencies
- Use explicit version 2.1.0 for Resilience4j dependencies
- Keep dependencies in `<dependencies>` section (not `<dependencyManagement>`)

**DO NOT:**
- Add version tags to Spring Boot starters (managed by parent POM)
- Add test dependencies in this task (handled separately)
- Modify any Java code or configuration files
- Add Bouncy Castle encryption dependency yet (task-013)

**Out of Scope:**
- Configuration of Redis connection (task-002)
- Implementation of cache logic (task-003, task-004, task-005)
- Circuit breaker configuration (task-006)
- Integration tests (optional, not required for this task)
