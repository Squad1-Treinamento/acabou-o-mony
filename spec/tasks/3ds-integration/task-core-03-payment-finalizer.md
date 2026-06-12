---
id: task-core-03
status: planned
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-002-3ds-core-payment-integration.md
  - spec/specs/spec-002-3ds-core-payment-integration.md
  - spec/tasks/3ds-integration/task-core-02-callback-handler.md
---

# ThreeDsPaymentFinalizer and async event listener

## Local Context

- **Directory:** `core-payment/`
- **Files to create:**
  - `core-payment/src/main/java/com/acabouomony/payment/domain/service/ThreeDsPaymentFinalizer.java` — async Spring event listener for post-3DS finalization
  - `core-payment/src/test/java/com/acabouomony/payment/domain/service/ThreeDsPaymentFinalizerTest.java` — unit tests
- **Files to modify:**
  - `core-payment/src/main/java/com/acabouomony/payment/infrastructure/config/AppConfig.java` — add `@EnableAsync` if not already present
- **Local dependencies:**
  - `ThreeDsCompletedEvent` record (from task-core-02): `public record ThreeDsCompletedEvent(UUID txId, boolean approved) {}`
  - `PaymentOrchestrationService.resumePaymentAfterAuth(UUID txId)` (from task-core-02)
  - `AuditLogService` — already exists
  - `ApplicationEventPublisher` / Spring Event infrastructure — already in use from task-core-02

## Scope

1. Check `AppConfig.java` for `@EnableAsync` — if absent, add it to the class annotation

2. Create `ThreeDsPaymentFinalizer` as `@Component`:
   - Constructor inject: `PaymentOrchestrationService orchestrationService`, `AuditLogService auditLogService`
   - Single listener method: `public void onThreeDsCompleted(ThreeDsCompletedEvent event)` annotated with:
     - `@Async` — runs on Spring's async executor, not the caller's thread
     - `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` — fires only after the publishing transaction commits
   - Method logic:
     - If `!event.approved()`: call `auditLogService.logStateTransition(...)` for DECLINED audit; return
     - If `event.approved()`: wrap call to `orchestrationService.resumePaymentAfterAuth(event.txId())` in try/catch; on exception: log error and return (do NOT rethrow — the callback response was already sent)
   - The try/catch is mandatory — an unhandled exception in an `@Async` method is logged by Spring but does NOT propagate to the caller

3. Create `ThreeDsPaymentFinalizerTest`:
   - **approved path:** publish `ThreeDsCompletedEvent(txId, true)` → `resumePaymentAfterAuth(txId)` called
   - **declined path:** publish `ThreeDsCompletedEvent(txId, false)` → `resumePaymentAfterAuth` NOT called, `auditLogService.logStateTransition` called
   - **exception isolation:** `resumePaymentAfterAuth` throws → exception caught and logged; no rethrow; method completes normally
   - Use `@MockBean` / Mockito mocks for `PaymentOrchestrationService` and `AuditLogService`
   - For async verification: inject a real `ApplicationEventPublisher`, publish the event, use `Mockito.verify(timeout(1000))` to give the async executor time to process

## Acceptance Criteria and Tests

- **Approved flow:** `ThreeDsCompletedEvent(txId, true)` → `resumePaymentAfterAuth(txId)` called asynchronously after DB commit
- **Declined flow:** `ThreeDsCompletedEvent(txId, false)` → audit log entry created via `logStateTransition()`, `resumePaymentAfterAuth` NOT called
- **Exception handling:** Exception in `resumePaymentAfterAuth` → caught and logged; no exception propagated; test passes
- **@Async guarantee:** Listener does NOT block the `completeThreeDsAuthentication` caller's thread
- **@TransactionalEventListener(AFTER_COMMIT) guarantee:** Listener fires only after the DB row with `AUTHENTICATED` or `DECLINED` status is durable
- **Tests:** `ThreeDsPaymentFinalizerTest` unit tests verify all scenarios above

**Verification:**
```bash
cd core-payment && mvn test -Dtest="ThreeDsPaymentFinalizerTest"
```

## Constraints and Negative Instructions

- Do NOT add `@Transactional` to the finalizer method itself — it runs in a new thread after the original transaction already committed; starting a new transaction here is not needed for the listener
- Do NOT rethrow exceptions from the listener — exceptions must be caught and logged silently; the ReconciliationWorker handles recovery
- Do NOT call Mercado Pago directly in the finalizer — always delegate to `orchestrationService.resumePaymentAfterAuth()`
- Do NOT use reactive types (`Mono`, `Flux`) — this is blocking Spring MVC
- The `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` annotation must be used (not `@EventListener`) — using `@EventListener` would fire before the DB commit and create a race condition
- `@EnableAsync` must be present somewhere in the configuration — if `AppConfig.java` already has it, do NOT add it again
