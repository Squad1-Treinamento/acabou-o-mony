---
id: task-006
status: completed
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-005-3ds-core-payment-integration.md
  - spec/specs/spec-005-3ds-core-payment-integration.md
  - spec/tasks/3ds-integration/task-005-three-ds-client.md
---

# ThreeDsCompletedEvent, callback handler, and orchestration methods

## Local Context

- **Directory:** `core-payment/`
- **Files to create:**
  - `core-payment/src/main/java/com/acabouomony/payment/domain/event/ThreeDsCompletedEvent.java` — Spring application event record
  - `core-payment/src/main/java/com/acabouomony/payment/web/dto/ThreeDsCallbackRequest.java` — inbound callback DTO
  - `core-payment/src/main/java/com/acabouomony/payment/web/ThreeDsCallbackController.java` — REST controller for callback endpoint
  - `core-payment/src/test/java/com/acabouomony/payment/web/ThreeDsCallbackControllerTest.java` — `@WebMvcTest` tests
  - `core-payment/src/test/java/com/acabouomony/payment/domain/service/PaymentOrchestrationServiceThreeDsTest.java` — unit tests for new methods
- **Files to modify:**
  - `core-payment/src/main/java/com/acabouomony/payment/domain/service/PaymentOrchestrationService.java` — inject `ApplicationEventPublisher`, add `completeThreeDsAuthentication()` and `resumePaymentAfterAuth()` methods
- **Local dependencies:**
  - `TransactionRepository.findById(UUID)` — already exists
  - `transitionState(Transaction, PaymentStatus, String)` — private method, already exists
  - `handlePaymentResult(Transaction, PaymentResult)` — private method, already exists
  - `PaymentAcquirerClient.submitPayment(Transaction)` — already injected
  - `ApplicationEventPublisher` — Spring built-in, inject via constructor
  - `PaymentStatus` enum — CHALLENGE_PENDING, AUTHENTICATED, DECLINED all present
  - `AuditLogService` — already injected

## Scope

1. Create `ThreeDsCompletedEvent` as a Java record in `com.acabouomony.payment.domain.event`:
   - `public record ThreeDsCompletedEvent(UUID txId, boolean approved) {}`
   - Plain record — no Spring annotations on the event itself

2. Create `ThreeDsCallbackRequest` DTO in `com.acabouomony.payment.web.dto`:
   - Lombok `@Data @NoArgsConstructor @AllArgsConstructor`
   - Fields with `@JsonProperty`: 
     - `challengeId` (`"challenge_id"`)
     - `transactionId` (`"transaction_id"`)
     - `merchantId` (`"merchant_id"`)
     - `authStatus` (`"auth_status"` — value: `"approved"` or `"declined"`)
     - `authenticatedAt` (`"authenticated_at"`, type `Instant`)

3. Add `ApplicationEventPublisher` to `PaymentOrchestrationService` constructor:
   - Add as final field, inject via the existing constructor (last parameter)
   - Do NOT use `@Autowired` field injection

4. Add `public void completeThreeDsAuthentication(UUID txId, boolean approved)` with `@Transactional`:
   - Load transaction: `transactionRepository.findById(txId).orElseThrow(() -> new RuntimeException("Transaction not found: " + txId))`
   - **Idempotency guard:** if `tx.getStatus() != PaymentStatus.CHALLENGE_PENDING` → log and return immediately (handles `CallbackNotifier` retries)
   - Transition: call private `transitionState(tx, approved ? PaymentStatus.AUTHENTICATED : PaymentStatus.DECLINED, "3ds-engine")`
   - Publish event: `eventPublisher.publishEvent(new ThreeDsCompletedEvent(txId, approved))`

5. Add `public void resumePaymentAfterAuth(UUID txId)` with `@Transactional`:
   - Load transaction: `transactionRepository.findById(txId).orElseThrow(...)`
   - Transition: `transitionState(tx, PaymentStatus.PROCESSING, "system")`
   - Submit: `PaymentResult result = paymentAcquirerClient.submitPayment(tx)`
   - Delegate: call private `handlePaymentResult(tx, result)`
   - Catches `PaymentTimeoutException` and `PaymentAcquirerException` — delegate to `unknownStateTransitionHandler` (same pattern as existing `processPayment()`)

6. Create `ThreeDsCallbackController` as `@RestController`:
   - Endpoint: `POST /api/v1/payments/3ds-callback`
   - Accepts `@RequestBody ThreeDsCallbackRequest request`
   - Determines `approved = "approved".equals(request.getAuthStatus())`
   - Calls `orchestrationService.completeThreeDsAuthentication(UUID.fromString(request.getTransactionId()), approved)`
   - Returns `ResponseEntity<Void>` with HTTP 200 OK
   - Pure transport layer — no status checking, no idempotency logic (service handles that)

7. Tests in `ThreeDsCallbackControllerTest` using `@WebMvcTest(ThreeDsCallbackController.class)`:
   - **approved callback:** POST with `auth_status: "approved"` → 200 OK; verify service called with `approved=true`
   - **declined callback:** POST with `auth_status: "declined"` → 200 OK; verify service called with `approved=false`
   - **idempotent retry:** service called twice with same txId → 200 both times
   - Use `@MockBean PaymentOrchestrationService`

8. Tests in `PaymentOrchestrationServiceThreeDsTest`:
   - **completeThreeDsAuthentication approved:** tx in CHALLENGE_PENDING → transitions to AUTHENTICATED, event published
   - **completeThreeDsAuthentication declined:** tx in CHALLENGE_PENDING → transitions to DECLINED, event published
   - **completeThreeDsAuthentication idempotency (AUTHENTICATED):** tx already in AUTHENTICATED → returns immediately, no transition, no event
   - **completeThreeDsAuthentication idempotency (DECLINED):** tx already in DECLINED → returns immediately, no transition, no event
   - **resumePaymentAfterAuth success:** tx in AUTHENTICATED → PROCESSING → COMPLETED (mock acquirer returns success)
   - **resumePaymentAfterAuth timeout:** PaymentTimeoutException → UNKNOWN (via unknownStateTransitionHandler)

## Acceptance Criteria and Tests

- **Callback endpoint:** `POST /api/v1/payments/3ds-callback` with `auth_status: "approved"` → 200 OK, `completeThreeDsAuthentication(txId, true)` called
- **Callback endpoint:** `POST /api/v1/payments/3ds-callback` with `auth_status: "declined"` → 200 OK, `completeThreeDsAuthentication(txId, false)` called
- **Authentication completion:** `completeThreeDsAuthentication` with CHALLENGE_PENDING tx → transitions to AUTHENTICATED/DECLINED, publishes ThreeDsCompletedEvent
- **Idempotency:** `completeThreeDsAuthentication` with non-CHALLENGE_PENDING tx → no-op (logged, returns immediately)
- **Resume payment:** `resumePaymentAfterAuth` transitions AUTHENTICATED → PROCESSING → final state via Mercado Pago
- **Callback durability:** Callback endpoint always returns 200 OK (duplicate callbacks handled in service, not via HTTP error)

**Verification:**
```bash
cd core-payment && mvn test -Dtest="ThreeDsCallbackControllerTest,PaymentOrchestrationServiceThreeDsTest"
```

## Constraints and Negative Instructions

- Do NOT share DTO classes between `3ds-engine` and `core-payment` — create `ThreeDsCallbackRequest` independently in core-payment
- Do NOT make `transitionState()` public — it must remain private
- Do NOT put idempotency logic in the controller — it belongs in `completeThreeDsAuthentication()`
- Do NOT return non-200 to the 3DS Engine callback (even for duplicate callbacks) — always 200 OK
- Do NOT use `@Autowired` field injection in `PaymentOrchestrationService` — it uses constructor injection
- Do NOT use reactive types (`Mono`, `Flux`) — this is blocking Spring MVC
- `resumePaymentAfterAuth` must NOT call Mercado Pago directly — use the existing private `handlePaymentResult()` for consistent result handling
