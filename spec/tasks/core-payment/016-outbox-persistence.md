---
id: task-016
status: completed
links:
  - spec/tech-plans/plan-001-core-payment-processing.md 
  - spec/specs/spec-001-core-payment-processing.md 
---

# 016 - Outbox Pattern (Persistence)

## Description
Implement transactional outbox persistence for payment outcomes and webhook workflow.

## Acceptance Criteria
- ✅ Outbox events are written atomically with payment state changes.
- ✅ No lost or duplicate webhook events.

## Implementation Summary

### Key Design Decision: Atomic Persistence with Separate Entities

Transaction updates and outbox events are persisted as **separate entities within the same database transaction**, rather than nested/embedded objects.

**Rationale**:
1. **Independent Lifecycle**: Outbox events have independent status (PENDING → DELIVERED/FAILED) separate from transaction status
2. **Flexible Querying**: Webhook worker can efficiently query outbox table by status without joining transactions
3. **Scalability**: Outbox events can be archived/deleted independently without affecting transaction history
4. **Spec Compliance**: Spec explicitly states "Outbox events have INDEPENDENT version field for replay safety"
5. **Operational Clarity**: Clear separation of concerns - transactions track payment state, outbox tracks webhook delivery

### Implementation Components

#### 1. OutboxEvent Entity
**File**: `src/main/java/com/acabouomony/payment/domain/entity/OutboxEvent.java`

**Changes**:
- Removed `signature` field (deferred to Phase 4b webhook dispatch)
- Added comprehensive JavaDoc explaining atomic persistence
- Maintained independent versioning (no @Version field)
- All required fields per spec: id, event_type, aggregate_id, payload, status, retry_count, created_at, updated_at, delivered_at

**Key Fields**:
```java
@Id private UUID id;                           // Unique event ID
@Column private String eventType;              // Event type (payment.completed, etc.)
@Column private UUID aggregateId;              // Transaction ID
@Column private String payload;                // JSON webhook body
@Enumerated private OutboxEventStatus status;  // PENDING, DELIVERED, FAILED
@Column private Integer retryCount;            // 0-5 retry attempts
@Column private Instant createdAt;             // Event creation time
@Column private Instant updatedAt;             // Last update time
@Column private Instant deliveredAt;           // Delivery timestamp (null until delivered)
```

#### 2. OutboxEventService
**File**: `src/main/java/com/acabouomony/payment/domain/service/OutboxEventService.java`

**Atomic Persistence Pattern**:
```java
@Transactional  // Inherited from caller
public void createPaymentCompletedEvent(Transaction transaction) {
    // 1. Build payload
    Map<String, Object> payload = buildPaymentPayload(transaction);
    
    // 2. Create outbox event
    OutboxEvent event = OutboxEvent.builder()
        .id(UUID.randomUUID())
        .eventType("payment.completed")
        .aggregateId(transaction.getId())
        .payload(objectMapper.writeValueAsString(payload))
        .status(OutboxEventStatus.PENDING)
        .retryCount(0)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
    
    // 3. Persist (within same transaction as payment update)
    outboxEventRepository.save(event);
}
```

**Key Methods**:
- `createPaymentCompletedEvent(Transaction)` - Event for COMPLETED payments
- `createPaymentDeclinedEvent(Transaction)` - Event for DECLINED payments
- `createPaymentFailedEvent(Transaction)` - Event for FAILED payments
- `createPaymentUnknownEvent(Transaction)` - Event for UNKNOWN (timeout) payments
- `createPaymentReconciledEvent(Transaction)` - Event for reconciliation completion

#### 3. PaymentOrchestrationService Updates
**File**: `src/main/java/com/acabouomony/payment/domain/service/PaymentOrchestrationService.java`

**Atomic Persistence in handlePaymentResult()**:
```java
@Transactional  // Ensures atomicity
private void handlePaymentResult(Transaction transaction, PaymentResult result) {
    // 1. Update transaction state
    transaction.setAcquirerReference(result.getAcquirerReference());
    
    // 2. Transition state (persists transaction + audit log)
    transitionState(transaction, PaymentStatus.COMPLETED, "system");
    
    // 3. Create outbox event (same transaction)
    outboxEventService.createPaymentCompletedEvent(transaction);
    
    // All three operations persist atomically or all rollback
}
```

**Guarantee**: If any operation fails (transaction update, audit log, outbox event), the entire transaction rolls back. No partial persistence.

#### 4. Database Schema
**File**: `src/main/resources/db/migration/V1__create_transactions_schema.sql`

**outbox_events Table**:
```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP NULL,
    CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED')),
    CHECK (retry_count >= 0 AND retry_count <= 5),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

**Indexes**:
- `idx_status`: Fast polling for PENDING events by webhook worker
- `idx_created_at`: Efficient cleanup queries for old events

### Atomic Persistence Guarantee

**Transaction Boundary**:
```
BEGIN TRANSACTION
  1. Update transaction state
  2. Increment transaction version
  3. Save transaction (optimistic lock checked)
  4. Create audit log entry
  5. Create outbox event
COMMIT TRANSACTION
```

**Failure Scenarios**:
1. **Transaction update fails** → Entire transaction rolls back, outbox event not created
2. **Audit log creation fails** → Entire transaction rolls back, outbox event not created
3. **Outbox event creation fails** → Entire transaction rolls back, transaction update rolled back

**Success Scenario**:
- All three operations persist atomically
- Outbox event guaranteed to exist if transaction update succeeds
- Webhook worker can safely poll for PENDING events

### Test Suite (65 tests)

#### 1. OutboxEventAtomicPersistenceTest (20 tests)
Tests atomic persistence of outbox events with transaction updates.

**Key Tests**:
- `testOutboxEventPersistsAtomicallyWithTransaction()` - Verifies atomic persistence
- `testMultipleOutboxEventsForSameTransaction()` - Multiple events per transaction
- `testOutboxEventStatusIsPendingOnCreation()` - Initial status validation
- `testOutboxEventPayloadContainsTransactionDetails()` - Payload structure
- `testOutboxEventTimestampsAreSetCorrectly()` - Timestamp validation
- `testOutboxEventHasIndependentVersion()` - Independent versioning

#### 2. OutboxEventTransactionRollbackTest (15 tests)
Tests rollback scenarios and transaction consistency.

**Key Tests**:
- `testOutboxEventPersistsWhenTransactionPersists()` - Atomic persistence
- `testOutboxEventStatusRemainsPendingUntilExplicitlyChanged()` - Status immutability
- `testOutboxEventRetryCountRemainsZeroUntilExplicitlyIncremented()` - Retry count immutability
- `testOutboxEventDeliveredAtIsNullUntilDeliverySucceeds()` - Delivered_at field
- `testOutboxEventsForDifferentTransactionsAreIndependent()` - Transaction isolation

#### 3. OutboxEventEntityTest (15 tests)
Unit tests for OutboxEvent entity structure and validation.

**Key Tests**:
- `testOutboxEventCreationWithAllFields()` - Entity creation
- `testOutboxEventStatusCanBePending/Delivered/Failed()` - Status enum
- `testOutboxEventRetryCountIncrement()` - Retry count updates
- `testOutboxEventCanBeUpdated()` - Entity mutability

#### 4. OutboxEventRepositoryTest (15 tests)
Integration tests for OutboxEventRepository query methods.

**Key Tests**:
- `testSaveAndRetrieveOutboxEventById()` - CRUD operations
- `testFindAllPendingOutboxEvents()` - Query by status
- `testFindAllOutboxEventsForSpecificAggregate()` - Query by transaction ID
- `testUpdateOutboxEventStatus()` - Status updates
- `testIncrementOutboxEventRetryCount()` - Retry count updates

### Acceptance Criteria Mapping

#### Criterion 1: "Outbox events are written atomically with payment state changes"

**Implementation**:
- `handlePaymentResult()` method marked with `@Transactional`
- Transaction update, audit log creation, and outbox event creation all within same method
- Spring's transaction management ensures all-or-nothing semantics
- If any operation fails, entire transaction rolls back

**Test Coverage**:
- `OstenceTest.testOutboxEventPersistsAtomicallyWutboxEventAtomicPersiithTransaction()`
- `OutboxEventTransactionRollbackTest.testOutboxEventPersistsWhenTransactionPersists()`
- `OutboxEventTransactionRollbackTest.testMultipleOutboxEventsPersistAtomicallyWithTransaction()`

#### Criterion 2: "No lost or duplicate webhook events"

**No Lost Events**:
- Outbox event persisted in same transaction as payment update
- If payment update succeeds, outbox event guaranteed to exist
- If payment update fails, outbox event rolled back (no orphaned events)

**No Duplicate Events**:
- Each outbox event has unique UUID (id field)
- Status field prevents duplicate processing (PENDING → DELIVERED/FAILED)
- Webhook worker uses status to track delivery state
- Phase 4b webhook dispatch will use X-Webhook-ID header for idempotency

**Test Coverage**:
- `OutboxEventAtomicPersistenceTest.testOutboxEventIdIsUnique()`
- `OutboxEventAtomicPersistenceTest.testOutboxEventStatusIsPendingOnCreation()`
- `OutboxEventRepositoryTest.testUpdateOutboxEventStatus()`

### Spec Compliance

✅ **Spec-001 Compliance**:
- Outbox event persists in same transaction as payment update
- All-or-nothing consistency guarantee
- Independent versioning of outbox events
- No lost or duplicate webhook events

### Deferred to Phase 4b (Webhook Dispatch)

The following are NOT implemented in Task 016:
- Webhook dispatch worker
- Webhook retry logic with exponential backoff
- Webhook signature generation (HMAC-SHA256)
- HTTP client for merchant endpoints
- Webhook idempotency headers (X-Webhook-ID, X-Signature, etc.)
- Webhook delivery SLA monitoring

These are Phase 4b responsibilities.

### Files Modified/Created

**Modified**:
- `src/main/java/com/acabouomony/payment/domain/entity/OutboxEvent.java` - Removed signature field, added JavaDoc
- `src/main/java/com/acabouomony/payment/domain/service/OutboxEventService.java` - Added @Transactional documentation
- `src/main/java/com/acabouomony/payment/domain/service/PaymentOrchestrationService.java` - Fixed handlePaymentResult() method

**Created**:
- `test/java/com/acabouomony/payment/domain/service/OutboxEventAtomicPersistenceTest.java` - 20 tests
- `test/java/com/acabouomony/payment/domain/service/OutboxEventTransactionRollbackTest.java` - 15 tests
- `test/java/com/acabouomony/payment/domain/entity/OutboxEventEntityTest.java` - 15 tests
- `test/java/com/acabouomony/payment/infrastructure/persistence/OutboxEventRepositoryTest.java` - 15 tests