---
id: task-005
status: completed
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-002-3ds-mfa-auth-engine.md
  - spec/specs/spec-002-3ds-mfa-auth-engine.md
---
# Implement MFA Token Verification with Idempotency and Session Expiry Checks

## Local Context

- **Directory:** `3ds-engine/`
- **Files to create:**
  - `3ds-engine/src/main/java/com/acabouomony/engine/service/AuthVerificationService.java` — MFA validation, idempotency, and expiry logic
  - `3ds-engine/src/main/java/com/acabouomony/engine/dto/MfaVerifyRequest.java` — Request DTO
  - `3ds-engine/src/main/java/com/acabouomony/engine/dto/MfaVerifyResponse.java` — Response DTO
- **Files to modify:** `LandingPageController.java` (task-004) — add verify endpoint
- **Local dependencies:** `ChallengeSessionRepository` (task-002), `ChallengeSessionService` (task-004)

## Scope

1. Create `MfaVerifyRequest` record: `challenge_id`, `mfa_token`.
2. Create `MfaVerifyResponse` record: `status` (APPROVED|DECLINED), `challenge_id`, `transaction_id`.
3. Create `AuthVerificationService` as `@Component`:
   - **Expiry check (fail fast):** After reading the session from Redis, check if the session is expired via `isSessionExpired()` (checks both `status` field and `created_at + ttl`). If expired, call `ChallengeSessionRepository.updateSessionStatus(challengeId, EXPIRED)` and throw `ChallengeExpiredException`.
   - **Idempotency check:** Before validating the MFA token, check Redis key `3ds:auth:{challenge_id}` via `ChallengeSessionRepository.findAuthResult()`. If an existing `AuthResult` is found, return the cached result immediately — do NOT reprocess the MFA token, update Redis, or send a callback. Log detection of duplicate `challenge_id` at WARN level.
   - **MFA validation (MVP):** Session must exist and `mfa_token` must be non-empty — no cryptographic token validation.
   - On success: calls `ChallengeSessionRepository.updateSessionStatus(challengeId, APPROVED)` and `ChallengeSessionRepository.saveAuthResult(challengeId, authResult)`.
   - On failure: calls `ChallengeSessionRepository.updateSessionStatus(challengeId, DECLINED)`.
   - Returns `Mono<MfaVerifyResponse>`.
4. Add `POST /api/v1/3ds/verify` endpoint to `LandingPageController`:
   - Accepts `MfaVerifyRequest` as JSON body.
   - Validates required fields (`challenge_id` and `mfa_token`).
   - Calls `AuthVerificationService.verifyMfa()`.
   - Returns HTTP 200 with `MfaVerifyResponse`.

## Acceptance Criteria and Tests

- **Success:** Valid MFA token updates session to `APPROVED`, persists auth result in Redis, returns HTTP 200 with `status: "approved"`.
- **Failure:** Invalid MFA token updates session to `DECLINED` and returns `status: "declined"`.
- **Expired session:** Expired session returns error with `3DS_CHALLENGE_EXPIRED` and HTTP 410 Gone.
- **Idempotency:** First call with `challenge_id=X` processes normally. Second call with same `challenge_id=X` returns cached auth result without updating Redis or sending a new callback.
- **Tests:** `WebTestClient` integration test for valid token (200 with approved). Test for invalid token (200 with declined). Unit test with expired session data verifies `ChallengeExpiredException` is thrown. Unit test: mock `findAuthResult()` to return cached result, verify `verifyMfa()` returns cached data without calling token validation.

## Constraints and Negative Instructions

- Session must exist in Redis before verification is attempted.
- Token validation must be non-blocking (reactive).
- Auth result must be persisted BEFORE responding to the cardholder bank.
- Do not store the raw MFA token in logs.
- Expiry check must check BOTH the `status` field AND the `created_at + ttl` calculation (defense-in-depth).
- Idempotency check must happen BEFORE any state mutation.
- Use the existing `3ds:auth:{challenge_id}` key for idempotency — do not create a separate deduplication key.
- If cached result exists, skip the callback to Core as well.
- Do not auto-delete expired sessions from Redis — let Redis TTL handle eviction.
