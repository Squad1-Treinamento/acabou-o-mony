---
id: task-006
status: planned
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-001-3ds-mfa-auth-engine.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
---
# Implement Async HTTP/2 Callback to Core Service

## Local Context

- **Directory:** `3ds-engine/`
- **Files to create:**
  - `3ds-engine/src/main/java/com/acabouomony/engine/service/CallbackNotifier.java` — Async WebClient callback
  - `3ds-engine/src/main/java/com/acabouomony/engine/dto/CallbackRequest.java` — Callback payload DTO
- **Files to modify:** `AuthVerificationService.java` (task-005) — invoke `CallbackNotifier` after verification
- **Local dependencies:** `WebClient` (configured with HTTP/2), `application.yml` (`3ds.callback-url`)

## Scope

1. Create `CallbackRequest` record: `challenge_id`, `transaction_id`, `auth_status` (APPROVED|DECLINED), `authenticated_at` (Instant).
2. Create `CallbackNotifier` as `@Component`:
   - Configure `WebClient` with HTTP/2 support, reading base URL from `3ds.callback-url`.
   - `notifyCore(challengeId, transactionId, authStatus)` — sends `POST` to `/api/v1/payments/3ds-callback` with `CallbackRequest` JSON body.
   - Fire-and-forget semantics: the callback must NOT delay the 3DS verify response to the cardholder bank.
   - Implement retry: 1 retry after 1s delay on failure.
   - Log callback result (success or failure) with `challenge_id` and `transaction_id` for audit. Never log the failure as an error (callback failure is recoverable via Core polling Redis).
3. Modify `AuthVerificationService.verifyMfa()` (task-005): after persisting auth result, call `CallbackNotifier.notifyCore()` in a non-blocking way (subscribe separately, do not chain into the response Mono).

## Acceptance Criteria and Tests

- **Success:** After MFA verification, a POST request with correct payload is sent to Core Service's callback URL. Response to cardholder bank is returned before callback completes.
- **Failure:** If Core is unreachable, callback is logged and retried once. The 3DS Engine response is not affected.
- **Tests:** Unit test with mocked `WebClient` verifying callback is invoked after verification. Test that callback failure does not propagate to the caller's Mono chain.

## Constraints and Negative Instructions

- Callback MUST be async/fire-and-forget. Do NOT block the verification response on callback completion.
- Callback URL must be configurable via `application.yml` (`3ds.callback-url`).
- Do not fail the transaction if callback fails — Core can poll Redis as fallback.
- Use structured audit logging (JSON) for all callback attempts and results.
