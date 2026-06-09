---
id: task-001
status: planned
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-001-3ds-mfa-auth-engine.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
---
# Scaffold Spring Boot WebFlux Project for 3DS Engine

## Local Context

- **Directory:** `3ds-engine/` (root-level subproject)
- **Files to create:**
  - `3ds-engine/pom.xml` — Maven project with Java 21
  - `3ds-engine/src/main/java/com/acabouomony/engine/ThreeDsEngineApplication.java` — Main class
  - `3ds-engine/src/main/resources/application.yml` — Skeleton configuration
  - `3ds-engine/src/main/java/com/acabouomony/engine/model/ErrorResponse.java` — Standard error DTO
  - `3ds-engine/src/main/java/com/acabouomony/engine/exception/ThreeDsException.java` — Custom exception base
  - `3ds-engine/src/main/java/com/acabouomony/engine/exception/ChallengeExpiredException.java`
  - `3ds-engine/src/main/java/com/acabouomony/engine/exception/InvalidTokenException.java`
  - `3ds-engine/src/main/java/com/acabouomony/engine/exception/DuplicateChallengeException.java`
  - `3ds-engine/src/main/java/com/acabouomony/engine/handler/GlobalErrorHandler.java` — `@ControllerAdvice`
  - `3ds-engine/src/test/java/com/acabouomony/engine/ThreeDsEngineApplicationTests.java` — Context load test
- **Dependencies to add in pom.xml:**
  - `spring-boot-starter-webflux` (Netty, reactive)
  - `spring-boot-starter-data-redis-reactive` (for Redis session storage)
  - `spring-boot-starter-security` (endpoint protection)
  - `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (JWT sign/verify)
  - `testcontainers` and `testcontainers-redis` (integration tests later)
  - `lombok` (optional, for boilerplate reduction)

## Scope

1. Create `pom.xml` with Spring Boot 3.x parent, Java 21, and all dependencies above.
2. Create `ThreeDsEngineApplication` main class with `@SpringBootApplication`.
3. Create skeleton `application.yml` with placeholder sections for:
   - Redis cluster connection (`spring.redis.cluster.nodes`)
   - JWT config (`jwt.secret`, `jwt.expiration-seconds`)
   - 3DS engine tuning (`3ds.session-ttl-seconds`, `3ds.auth-result-ttl-seconds`, `3ds.callback-url`, `3ds.rate-limit-per-second`)
   - Risk thresholds (`risk.score-threshold`, `risk.high-value-threshold`)
4. Create `ErrorResponse` record with fields: `status` (HTTP int), `error` (code string), `message`, `challenge_id`, `timestamp`.
5. Create `ThreeDsException(String errorCode, String message)` as base. Subclass `ChallengeExpiredException`, `InvalidTokenException`, `DuplicateChallengeException` — each with unique `errorCode`.
6. Create `GlobalErrorHandler` annotated with `@ControllerAdvice` that catches `ThreeDsException` variants and returns standardized `ErrorResponse` JSON with appropriate HTTP status.
7. Create context-load test to verify the application starts.

## Acceptance Criteria and Tests

- **Success:** Application compiles (`mvn compile`) and context loads. Error DTO and exception hierarchy are usable.
- **Failure:** Missing required dependency or configuration causes compilation error.
- **Tests:** `ThreeDsEngineApplicationTests` verifies application context loads. Unit test for `GlobalErrorHandler` verifies each exception variant returns correct HTTP status and error code.

## Constraints and Negative Instructions

- No relational database dependencies (`spring-boot-starter-data-jpa`, `spring-boot-starter-jdbc`, H2, Flyway, etc.).
- No Mercado Pago SDK dependency.
- No hardcoded secrets. JWT secret and Redis credentials come from config/environment.
- Do not add unused starters or libraries.