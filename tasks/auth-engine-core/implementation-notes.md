# Implementation Notes

## task-001 — Scaffold Spring Boot WebFlux Project for 3DS Engine

- **Decisions:** None. Straightforward scaffold per task spec.
- **Deviations:** Removed `testcontainers-redis` dependency (artifact does not exist on Maven Central) — using `testcontainers-bom` with `testcontainers` and `junit-jupiter` instead. Integration tests will use `GenericContainer` with Redis image directly.
- **Trade-offs:** Used Spring Boot 3.3.5 (latest stable 3.3.x line). Java 21 target, compiled with JDK 25.
- **Risks:** None identified.

## task-002 — Implement Redis Session Repository with Reactive CRUD Operations

- **Decisions:** Used `Jackson2JsonRedisSerializer` for value serialization. ChallengeSession serialized as HASH fields via manual `toSessionHash`/`fromSessionHash` methods (keys stored as flat strings). AuthResult serialized as JSON STRING via Jackson auto-serialization.
- **Deviations:** None.
- **Trade-offs:** Manual HASH serialization gives explicit control over Redis key naming (e.g., `transactionId` camelCase keys) but adds boilerplate vs. `@RedisHash` annotation approach. Chose explicitness for clarity.
- **Risks:** None identified.

## task-003 — Implement JWT Verification Utility for Challenge Tokens

- **Decisions:** Used JJWT library (`io.jsonwebtoken`) for JWT parsing/verification. `JwtClaims` implemented as a Java `record` for immutability. `JwtTokenProvider.verify()` returns `Mono<JwtClaims>` integrating cleanly with WebFlux reactive chain. Added dev fallback secret (256-bit) when `jwt.secret` is empty/not configured — avoids `WeakKeyException` without requiring a hardcoded secret in `application.yml`.
- **Deviations:** `jwt.expiration-seconds` is not directly used in the `JwtTokenProvider` — the JWT itself carries the `exp` claim, so the provider validates expiry from the token, not from configuration. The `application.yml` was not modified (secret remains env-only for production).
- **Trade-offs:** Dev fallback secret enables `@SpringBootTest` context-load tests without requiring `JWT_SECRET` env var. The fallback uses a simple string check (null/blank/short) to distinguish "not configured" from "configured but too short". Production deployments must set `JWT_SECRET` via environment variable.
- **Risks:** Unit tests use a separate signing key (hardcoded in test) — if the dev fallback secret changes, the tests are unaffected since they inject their own secret. No risk.

## task-004 — Implement 3DS Landing Page (Bank Redirect)

- **Decisions:** `ChallengeSessionService.resolveChallenge()` returns `Mono<ChallengeSession>` (empty if not found, error if expired) — controller handles 404 vs 410 via `defaultIfEmpty` vs `GlobalErrorHandler`. Controller tests use `WebTestClient.bindToController()` with `controllerAdvice(new GlobalErrorHandler())` to test error handling without full Spring context.
- **Deviations:** Session not found returns HTTP 404 via `defaultIfEmpty`. Session expired → `ChallengeExpiredException` → `GlobalErrorHandler` → HTTP 410. Invalid JWT → `InvalidTokenException` → `GlobalErrorHandler` → HTTP 400. All match the task spec.
- **Trade-offs:** Separated redirect logic into `redirectToAcs()` helper method to avoid inline lambda type inference issues with `Mono<ResponseEntity<Void>>`.
- **Risks:** No Redis connection needed for tests — all dependencies mocked via Mockito. Service expiry check uses `Instant.now()` making the test time-dependent (acceptable for unit tests with recent session creation).
