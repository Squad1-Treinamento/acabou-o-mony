---
id: task-005
status: planned
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-001-3ds-mfa-auth-engine.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
---
# Implement MFA Token Verification and Auth Result Persistence

## Local Context

- **Directory:** `3ds-engine/`
- **Files to create:**
  - `3ds-engine/src/main/java/com/acabouomony/engine/service/AuthVerificationService.java` — MFA validation logic
  - `3ds-engine/src/main/java/com/acabouomony/engine/dto/MfaVerifyRequest.java` — Request DTO
  - `3ds-engine/src/main/java/com/acabouomony/engine/dto/MfaVerifyResponse.java` — Response DTO
- **Files to modify:** `LandingPageController.java` (task-004) — add verify endpoint
- **Local dependencies:** `ChallengeSessionRepository` (task-002), `ChallengeSessionService` (task-004)

## Scope

1. Create `MfaVerifyRequest` record: `challenge_id`, `mfa_token`.
2. Create `MfaVerifyResponse` record: `status` (APPROVED|DECLINED), `challenge_id`, `transaction_id`.
3. Create `AuthVerificationService` as `@Component`:
   - `verifyMfa(request)` — reads session from Redis by `challenge_id`.
   - Validates `mfa_token` (MVP: session must exist and token must be non-empty — no cryptographic token validation).
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
- **Tests:** `WebTestClient` integration test for valid token (200 with approved). Test for invalid token (200 with declined). Verify Redis state after each call.

## Constraints and Negative Instructions

- Session must exist in Redis before verification is attempted.
- Token validation must be non-blocking (reactive).
- Auth result must be persisted BEFORE responding to the cardholder bank.
- Do not store the raw MFA token in logs.
