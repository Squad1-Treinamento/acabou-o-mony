---
id: task-012
status: completed
links:
  - spec/tech-plans/plan-001-core-payment-processing.md (Phase 3, Core Payment Processing)
  - spec/specs/spec-001-core-payment-processing.md (Payment Lifecycle Rules, State Transitions)
---

# 012 - Payment Orchestration

Implement payment processing orchestration: call sequence, conditional branching, state update, error handling per spec.

## Local Context

**Files created:**
- `src/main/java/com/acabouomony/payment/domain/service/StateTransitionValidator.java` (service)
- `src/main/java/com/acabouomony/payment/domain/service/RiskEvaluationService.java` (service)
- `src/main/java/com/acabouomony/payment/domain/service/AuditLogService.java` (service)
- `src/main/java/com/acabouomony/payment/domain/service/OutboxEventService.java` (service)
- `src/main/java/com/acabouomony/payment/domain/service/PaymentOrchestrationService.java` (main service)
- `src/test/java/com/acabouomony/payment/domain/service/PaymentOrchestrationServiceTest.java` (unit tests)
- `src/test/java/com/acabouomony/payment/web/PaymentOrchestrationIntegrationTest.java` (integration tests)

**Local dependencies:**
- Transaction entity (from task-001)
- PaymentStatus enum (from task-002)
- TransactionRepository (from task-003)
- AuditLogRepository (from task-001)
- OutboxEventRepository (from task-001)
- PaymentAcquirerClient (from task-011)
- MercadoPagoClient (from task-011)
- PaymentResult (from task-011)

## Scope

1. **Create StateTransitionValidator:**
   - Validates state machine rules
   - Enforces allowed transitions
   - Rejects invalid transitions with exception
   - Valid transitions defined in spec

2. **Create RiskEvaluationService:**
   - Evaluates transaction risk level
   - Returns true if high-risk (requires 3DS)
   - Returns false if low-risk (skip 3DS)
   - Simple rule-based evaluation:
     - Amount > 500 BRL → high-risk
     - No card token → high-risk
     - Otherwise → low-risk

3. **Create AuditLogService:**
   - Creates audit log entries
   - Computes SHA256 checksum
   - Persists to database
   - Checksum = SHA256(transactionId + oldStatus + newStatus + actor)

4. **Create OutboxEventService:**
   - Creates outbox events for webhooks
   - Methods for each event type:
     - createPaymentCompletedEvent()
     - createPaymentDeclinedEvent()
     - createPaymentFailedEvent()
     - createPaymentUnknownEvent()
     - createPaymentReconciledEvent()
   - Serializes payload to JSON
   - Persists with status PENDING

5. **Create PaymentOrchestrationService:**
   - Main orchestration service
   - Orchestrates complete payment flow
   - Methods:
     - processPayment(Transaction transaction)
     - handlePaymentResult(Transaction tx, PaymentResult result)
     - transitionState(Transaction tx, PaymentStatus newStatus, String actor)
   - Implements optimistic locking retry:
     - Max 3 retries
     - Exponential backoff: 100ms, 200ms, 300ms
     - Fail fast after 3 attempts
   - Handles all scenarios:
     - Low-risk: CREATED → VALIDATED → PROCESSING → COMPLETED/DECLINED/FAILED/UNKNOWN
     - High-risk: CREATED → VALIDATED → CHALLENGE_PENDING
     - Timeout: PROCESSING → UNKNOWN

6. **Write Unit Tests:**
   - PaymentOrchestrationServiceTest (15 tests)
   - Test all payment flows
   - Test state transitions
   - Test error handling
   - Test optimistic locking retry

7. **Write Integration Tests:**
   - PaymentOrchestrationIntegrationTest (10+ tests)
   - End-to-end payment flows
   - Real database
   - Concurrent scenarios

## Acceptance Criteria & Tests

**Success cases:**
- ✓ Orchestration covers success path (PROCESSING → COMPLETED)
- ✓ Orchestration covers decline path (PROCESSING → DECLINED)
- ✓ Orchestration covers error path (PROCESSING → FAILED)
- ✓ Orchestration covers timeout path (PROCESSING → UNKNOWN)
- ✓ Orchestration covers low-risk path (VALIDATED → PROCESSING)
- ✓ Orchestration covers high-risk path (VALIDATED → CHALLENGE_PENDING)
- ✓ State transitions validated
- ✓ Audit logs created
- ✓ Outbox events created
- ✓ Optimistic locking enforced
- ✓ Service isolated for testing

**Failure cases:**
- ✗ Invalid state transition rejected
- ✗ Null transaction throws exception
- ✗ Version conflict retried (max 3 times)
- ✗ Audit log failure rolls back transaction

**Required tests:**
- Unit test: PaymentOrchestrationServiceTest (15 tests)
  - Payment processing flows
  - State transitions
  - Risk evaluation
  - Audit log creation
  - Outbox event creation
  - Optimistic locking retry
- Unit test: StateTransitionValidatorTest (5 tests)
  - Valid transitions allowed
  - Invalid transitions rejected
- Unit test: RiskEvaluationServiceTest (5 tests)
  - High-risk detection
  - Low-risk detection
- Integration test: PaymentOrchestrationIntegrationTest (10+ tests)
  - End-to-end payment flows
  - State transitions
  - Audit trail
  - Outbox events

**Verification:**
```bash
mvn test -Dtest="*PaymentOrchestration*,*StateTransition*,*RiskEvaluation*"
```

## Implementation Details

### State Transitions

**Valid Transitions:**
```
CREATED → VALIDATED
VALIDATED → CHALLENGE_PENDING (high-risk)
VALIDATED → PROCESSING (low-risk)
CHALLENGE_PENDING → AUTHENTICATED
CHALLENGE_PENDING → DECLINED
CHALLENGE_PENDING → FAILED
AUTHENTICATED → PROCESSING
PROCESSING → COMPLETED
PROCESSING → DECLINED
PROCESSING → FAILED
PROCESSING → UNKNOWN
UNKNOWN → COMPLETED
UNKNOWN → DECLINED
UNKNOWN → FAILED
```

**Invalid Transitions:** All others rejected with exception

### Risk Evaluation Rules

- High-risk: amount > 500 BRL, no card token, etc.
- Low-risk: amount ≤ 500 BRL, has card token, etc.
- High-risk → CHALLENGE_PENDING (3DS required)
- Low-risk → PROCESSING (skip 3DS, preserve <1s SLA)

### Optimistic Locking Retry

- Max retries: 3
- Backoff: 100ms, 200ms, 300ms
- Fail fast after 3 attempts
- Operator intervention required on failure

### Audit Log Checksum

- Checksum = SHA256(transactionId + oldStatus + newStatus + actor)
- Computed at write time
- Never modified
- Prevents tampering

### Outbox Event Payload

- Contains transaction details
- Serialized to JSON
- Status: PENDING (awaiting webhook dispatch)
- Webhook worker polls and dispatches asynchronously

## Constraints & Negative Instructions

- Do NOT implement 3DS challenge handling (deferred to phase 6)
- Do NOT implement reconciliation sdeferred to phase 4a)
- Do NOT implement webhook dispatch (deferred to phase 4b)
- Do NOT modify transaction state without version check
- Do NOT auto-retry on invalid transitions
- Do NOT cache payment results in orchestration
- Do NOT implement rate limiting (handled by Nginx)
