---
id: task-010
status: completed
links:
  - spec/tech-plans/plan-001-core-payment-processing.md (Phase 2, Duplicate Request Recovery)
  - spec/specs/spec-001-core-payment-processing.md (Duplicate Request Rules, Idempotency Implementation Model)
---

# 010 - Duplicate Request Recovery

Implement logic to cache responses and handle recovery for duplicate and/or mismatched payment requests.

## Local Context

**Files created:**
- `src/main/java/com/acabouomony/payment/domain/service/PaymentResponseCache.java` (interface)
- `src/main/java/com/acabouomony/payment/domain/service/InMemoryPaymentResponseCache.java` (implementation)
- `src/main/java/com/acabouomony/payment/domain/service/UnknownStateRecoveryHandler.java` (service)
- `src/main/java/com/acabouomony/payment/domain/service/DuplicateRequestRecoveryService.java` (service)
- `src/test/java/com/acabouomony/payment/domain/service/PaymentResponseCacheTest.java` (unit tests)
- `src/test/java/com/acabouomony/payment/domain/service/DuplicateRequestRecoveryServiceTest.java` (unit tests)
- `src/test/java/com/acabouomony/payment/web/DuplicateRequestRecoveryIntegrationTest.java` (integration tests)

**Files modified:**
- `src/main/java/com/acabouomony/payment/web/PaymentController.java` (integrated response caching)

**Local dependencies:**
- IdempotencyService (from task-007)
- DuplicatePaymentHandler (from task-009)
- TransactionRepository (from task-003)
- Transaction entity (from task-001)
- PaymentStatus enum (from task-002)
- PaymentResponseDTO (from task-009)

## Scope

1. **Create PaymentResponseCache Interface:**
   - Abstraction for response caching mechanism
   - Methods: cache(), retrieve(), invalidate(), clear()
   - Supports multiple implementations (in-memory, Redis, etc.)

2. **Create InMemoryPaymentResponseCache Implementation:**
   - Thread-safe in-memory cache using ConcurrentHashMap
   - TTL-based expiration (24 hours per spec)
   - Automatic cleanup of expired entries
   - Merchant-scoped cache keys
   - Suitable for single-instance deployments or fallback

3. **Create UnknownStateRecoveryHandler:**
   - Handles UNKNOWN state transactions
   - Methods:
     - `canRecoverFromUnknown()`: Determines if recovery possible
     - `buildUnknownRecoveryResponse()`: Builds 202 Accepted response
     - `scheduleReconciliation()`: Schedules reconciliation (Phase 4)

4. **Create DuplicateRequestRecoveryService:**
   - Orchestrates duplicate request recovery
   - Methods:
     - `recoverFromCache()`: Fast-path cache retrieval
     - `cacheResponse()`: Caches successful responses
     - `handleMismatchedPayload()`: Handles payload mismatch (sets UNKNOWN)
     - `handleUnknownStateRecovery()`: Handles UNKNOWN state
     - `invalidateCache()`: Invalidates cache on error

5. **Enhance PaymentController:**
   - Integrate response caching (fast-path)
   - Check cache before database queries
   - Cache responses after successful processing
   - Graceful fallback to database when cache unavailable

## Acceptance Criteria & Tests

**Success cases:**
- ✓ Duplicate requests with matching payload return cached response
- ✓ Duplicate requests with mismatched payload return 400 Bad Request
- ✓ Mismatched payload sets transaction to UNKNOWN state
- ✓ UNKNOWN transactions return 202 Accepted
- ✓ Response caching works with PaymentController
- ✓ Cache TTL enforced (24 hours)
- ✓ Graceful degradation when cache unavailable
- ✓ Merchant isolation enforced
- ✓ Concurrent duplicate requests handled correctly

**Failure cases:**
- ✗ Mismatched payload rejected (400 Bad Request)
- ✗ UNKNOWN state transitions occur
- ✗ Cache miss falls back to database

**Required tests:**
- Unit test: PaymentResponseCacheTest (8 tests)
  - Cache storage and retrieval
  - Cache invalidation
  - TTL expiration
  - Concurrent access
  - Cache size tracking
- Unit test: DuplicateRequestRecoveryServiceTest (10 tests)
  - Cache recovery
  - Response caching
  - Mismatched payload handling
  - UNKNOWN state recovery
  - Cache invalidation
  - Reconciliation scheduling
- Integration test: DuplicateRequestRecoveryIntegrationTest (12+ tests)
  - Response caching with PaymentController
  - Duplicate request handling
  - Cache TTL expiration
  - Mismatched payload handling
  - UNKNOWN state recovery
  - Cache behavior and isolation

**Verification:**
```bash
mvn test -Dtest="*PaymentResponseCache*,*DuplicateRequestRecovery*"
```

## Implementation Details

### Cache Key Format
```
payment_response:{merchant_id}:{idempotency_key}
```

### Cache TTL
- 24 hours per spec
- Automatic expiration and cleanup
- Configurable via Duration parameter

### Duplicate Request Flow

#### Flow 1: Cache Hit (Fast-Path)
```
POST /api/v1/payments (duplicate, same payload)
├─ Check response cache
│  └─ Cache HIT: return cached response immediately
└─ Return response
```

#### Flow 2: Cache Miss, Database Hit (Slow-Path)
```
POST /api/v1/payments (duplicate, same payload)
├─ Check response cache
│  └─ Cache MISS: proceed to database
├─ Query database (IdempotencyService.checkDuplicate)
│  └─ Found + hash matches: return existing transaction
├─ Cache response
└─ Return response
```

#### Flow 3: Mismatched Payload
```
POST /api/v1/payments (duplicate, different payload)
├─ Check response cache
│  └─ Cache MISS: proceed to database
├─ Query database (IdempotencyService.checkDuplicate)
│  └─ Found + hash differs: throw PaymentValidationException
├─ DuplicateRequestRecoveryService.handleMismatchedPayload()
│  ├─ Set transaction status to UNKNOWN
│  ├─ Persist audit entry
│  ├─ Schedule reconciliation
│  └─ Return 400 Bad Request
└─ Return error response
```

#### Flow 4: UNKNOWN State
```
POST /api/v1/payments (duplicate, existing UNKNOWN transaction)
├─ Check response cache
│  └─ Cache MISS: proceed to database
├─ Query database (IdempotencyService.checkDuplicate)
│  └─ Found + status UNKNOWN: UnknownStateRecoveryHandler
│     ├─ Check if recovery possible
│     ├─ Schedule reconciliation
│     └─ Return 202 Accepted
├─ Cache response
└─ Return 202 Accepted
```

### HTTP Response Status Codes

| Status | Scenario |
|--------|----------|
| 200 OK | Terminal states (COMPLETED/DECLINED/FAILED) |
| 202 Accepted | UNKNOWN or in-progress states |
| 400 Bad Request | Validation error or payload mismatch |
| 409 Conflict | Duplicate with in-progress payment |

### Error Handling

**Cache Unavailable:**
- Fallback to database queries
- No performance degradation
- Log warning

**Mismatched Payload:**
- Transition transaction to UNKNOWN
- Return 400 Bad Request
- Persist audit entry
- Schedule reconciliation

**UNKNOWN State:**
- Return 202 Accepted
- Indicate outcome uncertain
- Schedule reconciliation
- Provide polling endpoint

## Constraints & Negative Instructions

- Do NOT include raw PAN in cached responses
- Do NOT cache error responses
- Do NOT cache responses with sensitive data
- Do NOT implement Redis caching in this task (deferred to task-008)
- Do NOT implement reconciliation in this task (deferred to task-004a)
- Do NOT modify transaction after duplicate detected (return as-is)
- Do NOT allow cross-merchant cache collision (merchant-scoped keys)
- Do NOT cache responses longer than 24 hours (TTL enforcement)
