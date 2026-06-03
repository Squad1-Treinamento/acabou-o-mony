---
id: task-007
status: planned
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-001-3ds-mfa-auth-engine.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
---
# Write Integration Tests with Testcontainers Redis

## Local Context

- **Directory:** `3ds-engine/`
- **Files to create/modify:**
  - `3ds-engine/src/test/java/com/acabouomony/engine/config/TestRedisConfig.java` — Test container configuration
  - `3ds-engine/src/test/java/com/acabouomony/engine/controller/ThreeDsChallengeControllerIntegrationTest.java` — Full flow test
- **Dependencies:** `testcontainers` (already in `pom.xml` from task-001), `WebTestClient`, `ReactiveRedisTemplate`

## Scope

1. Configure Redis test container in `TestRedisConfig`:
   - Use `GenericContainer` with Redis image (e.g., `redis:7-alpine`).
   - Set dynamic port and configure `ReactiveRedisTemplate` to connect to the container.
   - Use `@DynamicPropertySource` or `@TestConfiguration` to override `spring.redis.*` properties.
2. Write integration test `ThreeDsChallengeControllerIntegrationTest`:
   - Full happy path: initiate challenge → verify MFA → assert auth result in Redis → assert callback was sent.
   - Idempotency test: send same `challenge_id` twice, assert second call returns cached result without reprocessing.
   - Expired session test: create session with very short TTL (e.g., 1s), wait, verify `3DS_CHALLENGE_EXPIRED`.
   - Invalid MFA token test: send invalid token, assert `DECLINED` status.
   - Missing fields test: send malformed request, assert HTTP 400.
3. Ensure tests are self-contained: container starts before test class and stops after.

## Acceptance Criteria and Tests

- **Success:** All integration tests pass against a real Redis container. Coverage includes:
  - FR-2: Challenge initiation creates session in Redis (AC-2).
  - FR-3: MFA verification updates session status and persists auth result (AC-4, AC-5).
  - FR-5: Expired session returns `3DS_CHALLENGE_EXPIRED` (AC-6).
  - FR-6: Duplicate `challenge_id` returns cached result (AC-7).
- **Failure:** Integration tests fail if Redis is unreachable or assertions mismatch.

## Constraints and Negative Instructions

- Tests must be self-contained with Testcontainers — no external Redis dependency.
- Do not reuse containers across test classes (each class manages its own container).
- Tests must clean up Redis keys after each test method (use `@BeforeEach`/`@AfterEach`).
- Keep tests fast — use short TTLs (1-2s) for expiry tests.
