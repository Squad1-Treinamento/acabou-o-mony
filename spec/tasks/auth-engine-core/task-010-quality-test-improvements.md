---
id: task-010
status: in_progress
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-002-3ds-mfa-auth-engine.md
  - spec/specs/spec-002-3ds-mfa-auth-engine.md
---
# Quality Improvements in Test Infrastructure

## Objective

Improve test quality by fixing fragile patterns identified in the review report (M-009, m-008, m-009): replace `Thread.sleep` for TTL-based expiry tests, remove `.block()` from test setup, and create `TestRedisConfig`.

## Local Context

- **Directory:** `3ds-engine/`
- **Files to modify:**
  - `3ds-engine/src/test/java/com/acabouomony/engine/controller/ThreeDsChallengeControllerIntegrationTest.java`
  - `3ds-engine/src/test/java/com/acabouomony/engine/config/TestRedisConfig.java` (create)
- **Dependencies:** Testcontainers, JUnit 5, Spring Boot test

## Scope

1. **Replace `Thread.sleep(2500ms)`** in `ThreeDsChallengeControllerIntegrationTest.expiredSession_shouldReturn410()`:
   - Replace with a polling approach: check session expiry by polling Redis/challenge status in a loop with a short interval (e.g., 200ms) and a timeout (e.g., 5s).
   - Preserve the same behavioral assertion (410 GONE for expired session).

2. **Remove `.block()` calls from test lifecycle:**
   - In `cleanRedis()` `@BeforeEach`, chain delete operations reactively using `Mono.when()` or `Flux.concat()` and subscribe via `StepVerifier` or `Mono.block()` only as last resort.
   - If `.block()` cannot be fully eliminated, wrap in a helper method with clear documentation.

3. **Create `TestRedisConfig.java`:**
   - New class in `src/test/java/com/acabouomony/engine/config/TestRedisConfig.java`
   - Configure `ReactiveRedisTemplate<String, Object>` with `GenericJackson2JsonRedisSerializer` matching the production `RedisConfig` but using test-specific connection details.
   - Use `@TestConfiguration` and `@DynamicPropertySource` pattern.

## Acceptance Criteria

- `Thread.sleep` is removed from the test file.
- Expiry test still reliably produces 410 GONE.
- `cleanRedis()` uses reactive chaining (no `.block()`).
- `TestRedisConfig` exists and is usable by integration tests.
- All tests continue to pass.

## Dependencies

- None (standalone quality pass)
