---
id: task-009
status: planned
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-001-3ds-mfa-auth-engine.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
---
# Add Structured Audit Logging for All State Transitions

## Local Context

- **Directory:** `3ds-engine/`
- **Files to modify:**
  - `3ds-engine/src/main/java/com/acabouomony/engine/service/ChallengeSessionService.java` (task-004) — log challenge creation
  - `3ds-engine/src/main/java/com/acabouomony/engine/service/AuthVerificationService.java` (task-005) — log approval/decline
  - `3ds-engine/src/main/java/com/acabouomony/engine/service/CallbackNotifier.java` (task-006) — already partially done, align format
  - `3ds-engine/src/main/java/com/acabouomony/engine/service/ChallengeSessionService.java` (task-005) — log expiry events
- **No new files needed** — logging is added inline to existing services
- **Dependencies:** `LoggerFactory` (SLF4J), `Jackson` (or `ObjectMapper`) for JSON formatting

## Scope

1. Define a consistent structured JSON log format for all state transitions:
   ```json
   {
     "event": "challenge.created" | "challenge.approved" | "challenge.declined" | "challenge.expired" | "callback.sent" | "callback.failed",
     "challenge_id": "ch_001",
     "transaction_id": "tx_002",
     "merchant_id": "m_456",
     "timestamp": "2026-06-01T12:00:00Z",
     "details": {}
   }
   ```
2. Add audit logging to `ChallengeSessionService.initiateChallenge()` — log `challenge.created` after session is persisted to Redis.
3. Add audit logging to `AuthVerificationService.verifyMfa()` — log `challenge.approved` or `challenge.declined` after auth result is written to Redis.
4. Add audit logging to `ChallengeSessionService.isSessionExpired()` / `AuthVerificationService` — log `challenge.expired` when an expired session is detected and rejected.
5. Align `CallbackNotifier` logging to the same structured JSON format (currently logs callback result; ensure it uses `callback.sent` / `callback.failed` event types).
6. Use a single `Logger` per class with `LoggerFactory.getLogger()` — log at INFO level for normal transitions, WARN for failures/expiry, ERROR for unexpected errors.

## Acceptance Criteria and Tests

- **Success:** Every state transition produces a JSON log line containing `event`, `challenge_id`, `transaction_id`, and `timestamp`. Logs are readable and parsable by log aggregators (e.g., ELK, Datadog).
- **Failure:** Missing or malformed log lines for any transition.
- **Tests:** Unit test per service verifies that `Logger` is called with expected JSON fields on each transition. Use a mocked `Logger` / `Appender` (e.g., with `ch.qos.logback:logback-classic` testing utilities) to capture and assert log output.

## Constraints and Negative Instructions

- Do not log PII (PAN, card_token, raw MFA token) in audit logs.
- Do not change business logic — logging is additive only.
- Do not use `System.out` or `System.err` — always use SLF4J `Logger`.
- Do not log at ERROR level for callback failures (these are recoverable via Redis polling).
- Log format must be a single-line JSON per event (newline-delimited JSON — NDJSON) for compatibility with log shippers.
