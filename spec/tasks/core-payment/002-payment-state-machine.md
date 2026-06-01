---
id: task-002
status: complete
links:
  - spec/tech-plans/plan-001-core-payment-processing.md (Phase 1, Payment Lifecycle State Machine)
  - spec/specs/spec-001-core-payment-processing.md (Payment Lifecycle Rules, Allowed State Transitions)
---

# Implement Payment Lifecycle State Machine & Validation

Create deterministic payment state machine with validation rules and allowed transitions.

## Local Context

**Files to create/modify:**
- `src/main/java/com/acabouomony/payment/domain/model/PaymentStatus.java` (enum)
- `src/main/java/com/acabouomony/payment/domain/model/StateTransition.java` (value object)
- `src/main/java/com/acabouomony/payment/domain/service/PaymentStateMachine.java` (service)
- `src/main/java/com/acabouomony/payment/domain/exception/InvalidStateTransitionException.java`

**Local dependencies:**
- Transaction entity (from task-001)
- Spring component/service annotations
- Java enums and validation

## Scope

1. **Create PaymentStatus enum:**
   - CREATED, VALIDATED, CHALLENGE_PENDING, AUTHENTICATED, PROCESSING, UNKNOWN, COMPLETED, DECLINED, FAILED
   - Immutable, no business logic

2. **Create StateTransition value object:**
   - Encapsulates: from (PaymentStatus), to (PaymentStatus)
   - Immutable

3. **Create PaymentStateMachine service:**
   - @Service component
   - Method: `validateTransition(PaymentStatus from, PaymentStatus to)`
   - Allowed transitions per spec (23 allowed transitions listed in spec Section 1.2):
     - CREATED → VALIDATED
     - VALIDATED → CHALLENGE_PENDING | PROCESSING
     - CHALLENGE_PENDING → AUTHENTICATED | DECLINED | FAILED
     - AUTHENTICATED → PROCESSING
     - PROCESSING → COMPLETED | DECLINED | UNKNOWN | FAILED
     - UNKNOWN → COMPLETED | DECLINED | FAILED
   - Invalid transitions throw InvalidStateTransitionException with clear message
   - No external dependencies (pure business logic)

4. **Create InvalidStateTransitionException:**
   - Custom exception class
   - Constructor: InvalidStateTransitionException(from, to, reason)
   - Message format: "Invalid transition: {from} -> {to}. Reason: {reason}"

## Acceptance Criteria & Tests

**Success cases:**
- ✓ CREATED → VALIDATED allowed
- ✓ VALIDATED → PROCESSING allowed
- ✓ VALIDATED → CHALLENGE_PENDING allowed
- ✓ PROCESSING → COMPLETED allowed
- ✓ PROCESSING → UNKNOWN allowed
- ✓ UNKNOWN → COMPLETED allowed

**Failure cases:**
- ✗ CREATED → COMPLETED rejected (invalid transition)
- ✗ COMPLETED → PROCESSING rejected (terminal state)
- ✗ UNKNOWN → PROCESSING rejected (invalid)
- ✗ DECLINED → FAILED rejected (invalid)

**Required tests:**
- Unit test: Verify all 23 allowed transitions pass validation
- Unit test: Verify 50+ invalid transitions throw exception
- Unit test: Exception message contains from state, to state, and reason
- Unit test: PaymentStatus enum has correct values (9 states)

**Verification:**
```bash
mvn test -Dtest="*StateMachineTest"
```

## Constraints & Negative Instructions

- Do NOT add database calls to state machine (pure business logic)
- Do NOT create transition handlers (state machine only validates; handlers in Phase 3)
- Do NOT hardcode transitions as string comparisons (use enum-based rules)
- Do NOT allow state transitions not explicitly listed in spec Section 1.2
- Do NOT add side effects to validateTransition (must be idempotent)
