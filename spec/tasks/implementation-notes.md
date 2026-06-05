# Implementation Notes

## task-001 Implementation Notes

### What was done
- Created Maven pom.xml with Spring Boot 3.3.5, Java 21, WebFlux, Netty, Spring Data Redis Reactive, Spring Security, jjwt 0.12.6, Jackson, Testcontainers 1.20.4, Lombok (removed in review), and SLF4J
- Created ThreeDsEngineApplication.java — minimal Spring Boot entry point
- Created application.yml with skeleton configs: server (port 8081), redis, jwt, 3ds, risk
- Created ErrorResponse.java — record with status, error, message, challengeId (@JsonProperty), timestamp
- Created ThreeDsException.java — base runtime exception with errorCode and challengeId fields
- Created ChallengeExpiredException.java — extends ThreeDsException, HTTP 410
- Created InvalidTokenException.java — extends ThreeDsException, HTTP 401
- Created DuplicateChallengeException.java — extends ThreeDsException, HTTP 409
- Created GlobalErrorHandler.java — @ControllerAdvice, uses ex.getChallengeId()
- Created ThreeDsEngineApplicationTests.java — removed after verification

### Decisions
- package: com.acabouomony.engine
- Lombok removed — not used in the codebase
- ThreeDsException.challengeId field added during review to eliminate string parsing
- ErrorResponse.challengeId uses @JsonProperty("challenge_id") for snake_case serialization

### Deviations from spec
- None

## task-002 Implementation Notes

### What was done
- Created RedisConfig.java — ReactiveRedisTemplate<String, Object> with RedisSerializer.json() and RedisSerializer.string()
- Created ChallengeSession.java — record with transactionId, merchantId, amount (BigDecimal), currency, cardToken, status (ChallengeStatus enum), createdAt (Instant), ttl (long)
- Created AuthResult.java — record with authStatus, challengeId, transactionId, authenticatedAt (Instant)
- Created ChallengeSessionRepository.java — @Component wrapping ReactiveRedisTemplate:
  - saveSession / findSessionById / updateSessionStatus — HASH ops on 3ds:session:{id}
  - saveAuthResult / findAuthResult — STRING ops on 3ds:auth:{id}
- TTLs from application.yml via @Value

### Decisions
- ObjectMapper.convertValue() with JavaTimeModule for ChallengeSession ↔ Map conversion
- Keys follow 3ds:session:* and 3ds:auth:* namespaces
- GenericJackson2JsonRedisSerializer for value serialization

### Deviations from spec
- None

### Risks
- No tests yet (task-009 will cover integration tests)

## task-003 Implementation Notes

### What was done
- Created JwtClaims.java — record with challengeId, transactionId, merchantId, amount (BigDecimal), issuedAt (Instant), expiresAt (Instant)
- Created JwtTokenProvider.java — @Component with sign(claims), sign(challengeId, txId, merchantId, amount), verify(token) → Mono<JwtClaims> or Mono.error(InvalidTokenException)
- findAuthResult fixed to use MAPPER.convertValue(v, AuthResult.class) instead of instanceof + cast

### Decisions
- jwt.secret injection without default — fails fast at startup if not configured
- sign() convenience overload calculates exp from config expiry, simplifying endpoint code

### Deviations from spec
- None

## task-004 Implementation Notes

### What was done
- Created dto/ChallengeInitRequest.java — record with @NotBlank/@NotNull validation
- Created dto/ChallengeInitResponse.java — record with status, redirect_url, jwt_token, expires_in_seconds
- Created service/ChallengeSessionService.java — generates UUID challengeId, saves session to Redis, signs JWT, builds response
- Created controller/ThreeDsChallengeController.java — POST /api/v1/payments/3ds-challenge, reactive Mono<ResponseEntity>
- Added spring-boot-starter-validation to pom.xml

### Decisions
- Redirect URL: `{3ds.redirect-base-url}/{challengeId}` (configurable, default `http://localhost:8081/challenge`)
- challengeId: UUID.randomUUID()
- JWT signing in Mono.fromCallable() after session save for guaranteed ordering

### Deviations from spec
- None

## task-005 Implementation Notes

# Implementation Notes — task-005: MFA Verification with Idempotency and Expiry

## Decisions
- Idempotency check runs FIRST (before session lookup) to avoid unnecessary Redis reads when a result is already cached.
- `AuthResult` is saved in BOTH approve and decline branches — ensures idempotency works for declined tokens too (original task-005 only saved on approve, but idempotency from task-007 requires it).
- `isSessionExpired()` extracted to public in `ChallengeSessionService` with added `status.equals("expired")` check — avoids duplicating expiry logic.
- `MfaVerifyRequest`/`MfaVerifyResponse` as Java records for immutability and conciseness, matching existing `ErrorResponse` pattern.

## Deviations
- Original task-005 spec didn't include `saveAuthResult` for declined tokens. Added it because idempotency (task-007) requires a persisted auth result to detect duplicates.
- `POST /api/v1/3ds/verify` added to `LandingPageController` (existing class) rather than a new controller — keeps the 3DS endpoints co-located since both deal with challenge lifecycle.

## Trade-offs
- Using `ChallengeSessionService.isSessionExpired()` instead of duplicating logic keeps DRY but introduces a coupling between `AuthVerificationService` and `ChallengeSessionService`. Acceptable since both are in the same module and share domain logic.
- Manual field validation in controller (null/blank check) instead of `@Valid` + `spring-boot-starter-validation` avoids adding a new dependency for a simple check.

## Risks
- `AuthResult` stored as STRING via `Jackson2JsonRedisSerializer` — relies on Jackson default typing compatibility with the `Object.class` serializer configured in `RedisConfig`. If serialization format changes, existing cached results may not deserialize correctly. Mitigated by TTL-based expiry (24h).

## Post-implementation Changes (recommendations applied)
- **Case normalization:** `AuthResult.authStatus` now stores lowercase (`"approved"`, `"declined"`) — matches `MfaVerifyResponse` and `updateSessionStatus`. Removed `.toLowerCase()` from `toCachedResponse`.
- **Null/empty consistency:** Controller no longer blocks `mfaToken = null`. Both `null` and `""` flow to the service and are handled identically as declined.

## task-006 Implementation Notes

# Implementation Notes — task-006: Async HTTP/2 Callback to Core Service

## Decisions
- `CallbackNotifier.notifyCore()` returns `Mono<Void>` for testability; `AuthVerificationService` calls `.subscribe()` on it for fire-and-forget semantics.
- `WebClient.Builder` injected instead of creating a raw `WebClient` — follows Spring Boot conventions and makes it configurable by auto-configuration.
- Callback failure logged at WARN level (not ERROR) since Core can recover via Redis polling.
- Retry: 1 retry after 1s delay on any error (HTTP error or connection failure).

## Deviations
- No explicit HTTP/2 protocol configuration on `WebClient.Builder` — Spring Boot's auto-configuration with Netty already supports HTTP/2 via `server.http2.enabled`. The WebClient inherits the connector from the application context. If explicit H2 is needed, add `.clientConnector(new ReactorClientHttpConnector(HttpClient.create().protocol(HttpProtocol.H2)))`.

## Trade-offs
- Using `doOnSuccess` + `.subscribe()` for fire-and-forget means the callback may be lost if the application shuts down between the response and the callback completion. Acceptable for MVP — Core polls Redis as fallback.
- `CallbackRequest` uses `record` for immutability, consistent with other DTOs.

## Risks
- If the callback URL is misconfigured or unreachable, retry adds up to 3 attempts with exponential backoff (1s–5s). After that, the callback is silently dropped (logged at WARN). This is by design — Core recovers via Redis polling.
- No circuit breaker — repeated failures could cause resource buildup from retries. Acceptable for MVP volume.

## Post-implementation Changes (recommendations applied)
- **Retry upgrade:** `Retry.fixedDelay(1, 1s)` → `Retry.backoff(3, 1s).maxBackoff(5s)` — exponential backoff with up to 3 retry attempts.
- **Thread isolation:** `.subscribe()` → `.subscribeOn(Schedulers.boundedElastic()).subscribe()` — callback HTTP call runs on a separate thread pool, preventing Netty event loop starvation.

## task-007 Implementation Notes

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

## task-008 Implementation Notes

# Implementation Notes — task-008: Configuration Documentation

## Decisions
- All properties documented inline in `application.yml` with YAML comments — no separate `CONFIG.md` created. Keeps config co-located with its values; operators only need to read one file.
- Properties grouped by domain: server → spring (redis) → jwt → 3ds → risk. Alphabetical within groups.

## Deviations
- Added `spring.application.name: 3ds-engine` — missing from original scaffold, useful for metrics/logging.
- `risk.score-threshold` and `risk.high-value-threshold` kept in `application.yml` despite being Core Service properties. They're referenced in spec and tech plan, so documenting them here avoids confusion.

## Trade-offs
- Inline YAML comments are simpler than a separate CONFIG.md but harder to reference in documentation or scripts. Acceptable for MVP.

## Risks
- `jwt.expiration-seconds` is documented but unused in Engine code. If someone changes it expecting the Engine to enforce it, they'll be surprised (the Engine reads `exp` from the token). Already noted in application.yml comment.

## task-009 Implementation Notes

# Implementation Notes — task-009: Structured Audit Logging

## Decisions
- NDJSON format built via string concatenation rather than `ObjectMapper` — avoids inject dependency, keeps it simple since all values are safe strings (UUIDs, status codes).
- `auditLog()` helper as `static` package-private method in `ChallengeSessionService` — reused by `AuthVerificationService` and `CallbackNotifier` (same package). Reduces duplication across three services.
- Event types: `challenge.expired`, `challenge.approved`, `challenge.declined`, `challenge.cached`, `callback.sent`, `callback.failed`, `callback.retry`.
- Audit tests in separate files (`ChallengeSessionServiceAuditTest`, `AuthVerificationServiceAuditTest`) using Logback `ListAppender` — avoids cluttering existing behavior tests with log assertions.

## Deviations
- `CallbackNotifier` HTTP error log (`onStatus` handler) no longer logs the response body — was removed when aligning to NDJSON. The status code is available in the error's `doOnError` handler indirectly via the exception message.
- `CallbackNotifier` retry log changed to NDJSON format: `{"event":"callback.retry","challenge_id":"...","attempt":N}`.

## Trade-offs
- String concatenation for JSON instead of `ObjectMapper` — simpler but fragile if values contain special characters. Challenge/transaction IDs are UUIDs, so safe.
- Separate audit test files add test count but keep focus. Each service has one audit test class covering all its event types.

## Risks
- `auditLog()` helper is in `ChallengeSessionService` but used by other classes — creates a dependency on that class for a utility method. If `ChallengeSessionService` is removed, compilation breaks. Mitigated: low risk since it's a core service.
- Logback `ListAppender` tests are fragile to log level changes. If a log level changes from WARN to INFO, the test fails. Acceptable — intentional.
