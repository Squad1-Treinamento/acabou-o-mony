---
id: task-007
status: planned
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-001-3ds-mfa-auth-engine.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
---
# Implement Idempotency Check for Duplicate Challenge Processing

## Local Context

- **Directory:** `3ds-engine/`
- **Files to create:**
  - `3ds-engine/src/main/java/com/acabouomony/engine/filter/IdempotencyFilter.java` — Idempotency check component (or integrate into `AuthVerificationService`)
- **Files to modify:** `AuthVerificationService.java` (task-005) — add idempotency check before processing
- **Local dependencies:** `ChallengeSessionRepository.findAuthResult()` (task-002)

## Scope

1. Implement idempotency logic in `AuthVerificationService` (or as a separate `@Component` invoked before verification):
   - Before validating the MFA token, check Redis key `3ds:auth:{challenge_id}` via `ChallengeSessionRepository.findAuthResult()`.
   - If an existing `AuthResult` is found, return the cached result immediately — do NOT reprocess the MFA token.
   - If no existing result is found, proceed with normal MFA verification.
2. Log detection of duplicate `challenge_id` for audit purposes (level: WARN, with `challenge_id` and `transaction_id`).
3. The idempotency window is 24 hours (matching the TTL of `3ds:auth:{challenge_id}`).

## Acceptance Criteria and Tests

- **Success:** First call with `challenge_id=X` processes normally. Second call with same `challenge_id=X` returns cached auth result without updating Redis or sending a new callback.
- **Failure:** N/A — idempotency is transparent to the caller (same response format as first call).
- **Tests:** Unit test: mock `findAuthResult()` to return cached result, verify `verifyMfa()` returns cached data without calling token validation. Integration test: send same `challenge_id` twice, assert both return same status but Redis state is unchanged after second call.

## Constraints and Negative Instructions

- Idempotency check must happen BEFORE any state mutation (before updating session status or writing auth result).
- Must use the existing `3ds:auth:{challenge_id}` key — do not create a separate deduplication key.
- The cached response must exactly match the original response format.
- If the cached result exists, skip the callback to Core as well.
