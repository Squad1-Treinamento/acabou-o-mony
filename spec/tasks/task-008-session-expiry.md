---
id: task-008
status: planned
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-001-3ds-mfa-auth-engine.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
---
# Add Session Expiry Handling and 3DS_CHALLENGE_EXPIRED Error Flow

## Local Context

- **Directory:** `3ds-engine/`
- **Files to modify:** `ChallengeSessionService.java` (task-004), `AuthVerificationService.java` (task-005)
- **Local dependencies:** `ChallengeSessionRepository` (task-002), `ChallengeExpiredException` (task-001)

## Scope

1. Add method `isSessionExpired(ChallengeSession)` in `ChallengeSessionService`:
   - Check if session `status` field equals `EXPIRED`.
   - Check if `created_at + ttl` has passed (handles edge case where Redis TTL has not yet evicted the key but the session is logically expired).
   - Return `true` if either condition is met.
2. Modify `AuthVerificationService.verifyMfa()`:
   - After reading the session from Redis, call `isSessionExpired()`.
   - If expired, call `ChallengeSessionRepository.updateSessionStatus(challengeId, EXPIRED)` and throw `ChallengeExpiredException`.
   - If valid, proceed with MFA token validation.
3. `GlobalErrorHandler` (task-001) already maps `ChallengeExpiredException` to an error response with status `3DS_CHALLENGE_EXPIRED`.

## Acceptance Criteria and Tests

- **Success:** Valid (non-expired) session proceeds to MFA verification. Expired session returns error with status `3DS_CHALLENGE_EXPIRED` and HTTP 400 (or 410 Gone).
- **Failure:** N/A — clear error response for expired sessions.
- **Tests:** Unit test with expired session data (past `created_at + ttl`) verifies `ChallengeExpiredException` is thrown. Integration test: create session with 1s TTL, wait for expiry, submit MFA token, assert `3DS_CHALLENGE_EXPIRED` response.

## Constraints and Negative Instructions

- Must check BOTH the `status` field AND the `created_at + ttl` calculation (defense-in-depth against Redis TTL edge cases).
- Do not auto-delete expired sessions from Redis — let Redis TTL handle eviction.
- Error response must include `challenge_id` for traceability.
- Expired check must happen BEFORE MFA token validation (fail fast).
