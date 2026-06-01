---
id: task-003
status: planned
links:
  - spec/tech-plans/plan-001-core-payment-processing.md (Phase 1, optimistic locking)
  - spec/specs/spec-001-core-payment-processing.md (Optimistic Locking Semantics, Version Increment Strategy)
---

# Implement Optimistic Locking & Version Management

Create version-based conflict detection and retry logic for concurrent state transitions.

## Local Context

**Files to create/modify:**
- `src/main/java/com/acabouomony/payment/infrastructure/persistence/TransactionRepository.java`
- `src/main/java/com/acabouomony/payment/domain/service/TransactionVersionService.java`
- `src/main/java/com/acabouomony/payment/domain/exception/OptimisticLockException.java` (custom)
- `src/main/java/com/acabouomony/payment/infrastructure/persistence/OptimisticLockRetryHandler.java`

**Local dependencies:**
- Transaction entity (from task-001)
- Spring Data JPA
- Spring Retry framework (optional, or implement manual retry loop)
- PaymentStateMachine (from task-002)

## Scope

1. **Create TransactionRepository (Spring Data JPA):**
   - Extends JpaRepository<Transaction, UUID>
   - No custom methods needed (use built-in save())
   - JPA/Hibernate handles @Version annotation automatically

2. **Create TransactionVersionService:**
   - @Service component
   - Method: `updateTransactionState(UUID transactionId, PaymentStatus newStatus, String actor)`
   - Implements retry loop with bounded retries (3 attempts):
     - Attempt 1: Immediate
     - Attempt 2: After 100ms backoff
     - Attempt 3: After 200ms backoff
   - Logic:
     - Load transaction by ID
     - Validate state transition (PaymentStateMachine)
     - Increment version: `transaction.setVersion(transaction.getVersion() + 1)`
     - Set new status: `transaction.setStatus(newStatus)`
     - Call repository.save(transaction)
     - On OptimisticLockException: retry with backoff (max 3 attempts)
     - After 3 failures: throw exception, log to monitoring, do NOT retry further
   - Atomically update transaction + audit log in same DB transaction

3. **Create OptimisticLockException (custom):**
   - Extends RuntimeException
   - Constructor: OptimisticLockException(transactionId, attempt, originalCause)
   - Message: "Optimistic lock conflict on transaction {id} after {attempt} attempts"

4. **Create OptimisticLockRetryHandler:**
   - Utility class for retry logic
   - Static method: `executeWithRetry(Supplier<T> operation, int maxAttempts)`
   - Returns T on success
   - Throws OptimisticLockException on all attempts exhausted

5. **Configure exception handling:**
   - Map JPA OptimisticLockException to custom exception with retry guidance

## Acceptance Criteria & Tests

**Success cases:**
- ✓ Version incremented on successful state transition (0 → 1 → 2...)
- ✓ Concurrent update succeeds on first thread, second thread retries and detects stale version
- ✓ Retry backoff: 100ms, 200ms delays applied
- ✓ After 3 failed attempts, exception thrown with attempt count

**Failure cases:**
- ✗ Update with stale version fails on first attempt (no automatic retry)
- ✗ 4th attempt never executed (max 3)
- ✗ OptimisticLockException message does not contain transaction ID (verification fails)

**Required tests:**
- Unit test: Version incremented on successful save
- Integration test: Concurrent transaction updates, one wins, other gets OptimisticLockException
- Integration test: Retry loop executes up to 3 times
- Integration test: Backoff delays applied correctly (100ms, 200ms)
- Integration test: After 3 failures, exception propagated (not retried further)
- Integration test: Audit log created atomically with transaction update (both persist or both rollback)

**Verification:**
```bash
mvn test -Dtest="*OptimisticLock*,*VersionManagement*"
```

## Constraints & Negative Instructions

- Do NOT use application-level locking (pessimistic) - use optimistic only
- Do NOT auto-retry indefinitely (max 3 attempts enforced)
- Do NOT silently ignore OptimisticLockException (must log and expose to monitoring)
- Do NOT update version field outside of state transitions
- Do NOT add retry logic at HTTP controller layer (only at service layer)
- Version field MUST be EVERY state transition, not selective
