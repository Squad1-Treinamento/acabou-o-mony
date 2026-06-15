---
id: task-002
status: completed
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-005-3ds-core-payment-integration.md
  - spec/specs/spec-005-3ds-core-payment-integration.md
  - spec/tasks/3ds-integration/task-001-jwt-generate-token.md
---

# Add ThreeDsSession DTOs and ChallengeSessionService.createSession

## Local Context

- **Directory:** `3ds-engine/`
- **Files to create:**
  - `3ds-engine/src/main/java/com/acabouomony/engine/dto/ThreeDsSessionRequest.java` — inbound request record
  - `3ds-engine/src/main/java/com/acabouomony/engine/dto/ThreeDsSessionResponse.java` — outbound response record
- **Files to modify:**
  - `3ds-engine/src/main/java/com/acabouomony/engine/service/ChallengeSessionService.java` — add constructor args and createSession method
  - `3ds-engine/src/test/java/com/acabouomony/engine/service/ChallengeSessionServiceTest.java` — update setUp() constructor args and add createSession tests
- **Local dependencies:**
  - `JwtTokenProvider.generateToken` (from task-001)
  - `ChallengeSessionRepository` — already exists
  - `ChallengeSession` entity — already exists, check existing fields before building the session object
  - `@Value("${3ds.redirect-base-url}")` — already in `application.properties.example` as `http://localhost:8081/challenge`

## Scope

1. Create `ThreeDsSessionRequest` record in `com.acabouomony.engine.dto`:
   - Fields (all with `@JsonProperty` snake_case): `transactionId`, `merchantId`, `amount` (`long`), `currency`, `cardToken`
   - `@NotBlank` on all String fields, `@NotNull` on `amount`

2. Create `ThreeDsSessionResponse` record in `com.acabouomony.engine.dto`:
   - Fields: `challengeId` (`@JsonProperty("challenge_id")`), `acsUrl` (`@JsonProperty("acs_url")`), `jwt`

3. Update `ChallengeSessionService` constructor:
   - Add `JwtTokenProvider jwtTokenProvider` parameter
   - Add `@Value("${3ds.redirect-base-url}") String acsBaseUrl` parameter
   - Store both as final fields

4. Add `public Mono<ThreeDsSessionResponse> createSession(ThreeDsSessionRequest request)` to `ChallengeSessionService`:
   - Generate `challengeId = UUID.randomUUID().toString()`
   - Build `acsUrl = acsBaseUrl + "/" + challengeId` — IMPORTANT: `acsBaseUrl` already contains `/challenge` (e.g., `http://localhost:8081/challenge`), so the result is `http://localhost:8081/challenge/<uuid>`. Do NOT append `/challenge/` again.
   - Call `jwtTokenProvider.generateToken(challengeId, request.transactionId(), request.merchantId(), request.amount())`
   - Build and persist `ChallengeSession` via `repository.saveSession(challengeId, session)`
   - Return `Mono<ThreeDsSessionResponse>` with challengeId, acsUrl, jwt

5. Fix `ChallengeSessionServiceTest.setUp()`: pass `jwtTokenProvider` (mocked) and `acsBaseUrl` (e.g., `"http://localhost:8081/challenge"`) to constructor

6. Add unit tests for `createSession`:
   - Returns `ThreeDsSessionResponse` with non-null challengeId, acsUrl, jwt
   - `acsUrl` = `acsBaseUrl + "/" + challengeId` (no double `/challenge/`)
   - `repository.saveSession` called once with the generated `challengeId`
   - `jwtTokenProvider.generateToken` called with correct arguments including `amount` as `long`
   - Each call generates a distinct `challengeId` (UUID uniqueness)

## Acceptance Criteria and Tests

- `createSession` returns non-null `ThreeDsSessionResponse` with all three fields populated
- `acsUrl` format: `{acsBaseUrl}/{challengeId}` — never `{acsBaseUrl}/challenge/{challengeId}`
- Session persisted in Redis via `repository.saveSession`
- `JwtTokenProvider.generateToken` called with correct args
- Existing `resolveChallenge` and `isSessionExpired` tests still pass after constructor change

**Verification:**
```bash
cd 3ds-engine && mvn test -Dtest="ChallengeSessionServiceTest"
```

## Constraints and Negative Instructions

- Do NOT modify `resolveChallenge()` or `isSessionExpired()` — additive changes only
- Do NOT append `/challenge/` to `acsBaseUrl` — the base URL already contains it
- Do NOT use `BigDecimal` for `amount` in the DTO — use `long`
- Do NOT put UUID generation, URL building, or JWT signing in the controller (task-003) — all of it lives in this service
- Do NOT break existing `ChallengeSessionServiceTest` tests — only update the constructor call in `setUp()`
