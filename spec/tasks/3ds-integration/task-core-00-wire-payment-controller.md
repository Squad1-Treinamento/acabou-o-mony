---
id: task-core-00
status: planned
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-002-3ds-core-payment-integration.md
  - spec/specs/spec-002-3ds-core-payment-integration.md
  - spec/tasks/3ds-integration/task-core-01-three-ds-client.md
---

# Wire PaymentController to call processPayment()

## Local Context

- **Directory:** `core-payment/`
- **Files to create:** none
- **Files to modify:**
  - `core-payment/src/main/java/com/acabouomony/payment/web/PaymentController.java` — inject `PaymentOrchestrationService`, call `processPayment()` after save, add conditional 202 response for `CHALLENGE_PENDING`
  - `core-payment/src/test/java/com/acabouomony/payment/web/PaymentControllerTest.java` — add unit/integration tests for wiring
- **Local dependencies:**
  - `PaymentOrchestrationService` — already exists; `processPayment(Transaction)` already exists
  - `PaymentResponseDTO` — already exists with Lombok `@Builder`; `challengeId` and `acsUrl` fields added in task-core-01
  - `PaymentStatus.CHALLENGE_PENDING` — already in enum
  - `TransactionRepository` — already injected; `save()` method available

## Scope

1. Inject `PaymentOrchestrationService` into `PaymentController` via `@Autowired` field injection (follow existing pattern in the controller — it uses `@Autowired` fields, not constructor injection)

2. After `Transaction saved = transactionRepository.save(transaction)`, add the call:
   ```java
   orchestrationService.processPayment(saved);
   ```
   This call mutates `saved` in place (its status will be updated by the service).

3. Replace the existing `ResponseEntity<PaymentResponseDTO> response = duplicatePaymentHandler.buildNewPaymentResponse(saved);` block with conditional logic:
   - If `saved.getStatus() == PaymentStatus.CHALLENGE_PENDING`: build a 202 response using `PaymentResponseDTO.builder()` with `transactionId`, `status`, `challengeId` (from `saved.getChallengeId()` — may be null until task-core-01), `acsUrl` (from `saved.getChallengeAcsUrl()` — may be null until task-core-01)
   - Otherwise: delegate to existing `duplicatePaymentHandler.buildNewPaymentResponse(saved)` as before

4. Add unit/integration tests for the wiring:
   - When orchestration transitions to `CHALLENGE_PENDING`: controller returns 202 with that status
   - When orchestration transitions to `COMPLETED`: controller returns 200 with existing response shape
   - Mock `PaymentOrchestrationService` to control the transition behavior

## Acceptance Criteria and Tests

- **Success:** `POST /api/v1/payments` calls `orchestrationService.processPayment(transaction)` exactly once
- **HIGH-risk path (mocked):** `processPayment` transitions to `CHALLENGE_PENDING` → controller returns 202 with `transactionId`, `status`, `challengeId`, and `acsUrl`
- **LOW-risk path (mocked):** `processPayment` transitions to `COMPLETED` → controller returns 200 with existing response shape
- **Response caching behavior unchanged:** existing tests still pass
- **Duplicate detection behavior unchanged:** existing tests still pass
- **Transaction persisted before processing:** `processPayment()` called only after `transactionRepository.save()`

**Verification:**
```bash
cd core-payment && mvn test -Dtest="PaymentControllerTest"
```

## Constraints and Negative Instructions

- Do NOT use constructor injection — follow the existing `@Autowired` field injection pattern in `PaymentController`
- Do NOT return `Mono<ResponseEntity<T>>` — this is Spring MVC blocking; return `ResponseEntity<T>`
- Do NOT call `processPayment()` before `transactionRepository.save()` — the transaction must be persisted first
- Do NOT call `processPayment()` on the duplicate-detection code paths (when `existingTx.isPresent()`) — only on new transactions
- The `challengeId` and `acsUrl` fields on `PaymentResponseDTO` do not exist yet (added in task-core-01); use `saved.getChallengeId()` and `saved.getChallengeAcsUrl()` as placeholders in the builder, knowing they will be null until task-core-01 completes
