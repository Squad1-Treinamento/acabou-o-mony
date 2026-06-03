# Implementation Notes — task-007: Integration Tests with Testcontainers Redis

## Decisions
- Single integration test class covering all scenarios (happy path, declined, expired, missing fields, not found) — keeps Redis container lifecycle simple: one container per test class.
- `@MockBean` for `CallbackNotifier` — avoids real HTTP calls during integration tests while still verifying the callback was invoked.
- Standalone Redis via `spring.redis.host`/`port` overrides instead of cluster mode — Testcontainers single container doesn't support cluster without extra setup. The serialization config (`RedisConfig`) works identically.

## Deviations
- `TestRedisConfig` class not created — `@DynamicPropertySource` on the test class itself is sufficient to reconfigure Redis connection. No custom `ReactiveRedisTemplate` override needed; the existing `RedisConfig` works with the standalone Redis.
- Session cleanup via `.block()` in `@BeforeEach` instead of reactive — acceptable for test setup, simplifies code.

## Trade-offs
- `Thread.sleep(2500)` for expiry test is conservative (1s TTL + 2.5s wait) but avoids flakiness under CI load.
- Idempotency test verifies Redis state (`AuthResult` unchanged) and callback call count (exactly 1).

## Risks
- Tests require Docker — will be skipped if Docker is not available. This is expected for Testcontainers.
- `Thread.sleep` in expiry test adds 2.5s to test execution time. Acceptable for integration test suite.

## Post-implementation Changes (recommendations applied)
- **Expiry sleep increased:** 1500ms → 2500ms for reliability under CI load.
- **Idempotency test added:** `idempotency_secondCallReturnsCachedResult` — sends same challenge_id twice, verifies `notifyCore` called exactly once and `AuthResult` unchanged.
