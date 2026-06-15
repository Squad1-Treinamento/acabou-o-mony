---
id: task-009
status: completed
links:
  - spec/tech-plans/plan-001-core-payment-processing.md (Phase 2, DB Idempotency Enforcement)
  - spec/specs/spec-001-core-payment-processing.md (Duplicate Request Rules, Idempotency Implementation Model)
---

# 009 - DB Idempotency Enforcement

Implement database-level idempotency enforcement with unique constraints and fallback for duplicate recovery.

## Local Context

**Files created:**
- `src/main/java/com/acabouomony/payment/domain/exception/DuplicatePaymentException.java` (custom exception)
- `src/main/java/com/acabouomony/payment/domain/service/DuplicatePaymentHandler.java` (service)
- `src/main/java/com/acabouomony/payment/web/dto/PaymentResponseDTO.java` (response DTO)
- `src/test/java/com/acabouomony/payment/domain/service/DuplicatePaymentHandlerTest.java` (unit tests)
- `src/test/java/com/acabouomony/payment/web/PaymentControllerDuplicateTest.java` (integration tests)

**Files modified:**
- `src/main/java/com/acabouomony/payment/web/PaymentController.java` (implemented payment endpoint)
- `src/main/java/com/acabouomony/payment/infrastructure/exception/GlobalExceptionHandler.java` (added exception handlers)

**Local dependencies:**
- IdempotencyService (from task-007)
- PayloadHashingService (from task-007)
- TransactionRepository (from task-003)
- Transaction entity (from task-001)
- PaymentStatus enum (from task-002)
- PaymentRequestValidator (from task-005)
- MerchantAuthService (from task-004)

## Scope

1. **Create DuplicatePaymentException:**
   - Custom exception for duplicate payment detection
   - Contains: transactionId, merchantId, idempotencyKey
   - Message format: "Duplicate payment detected: merchant={}, idempotency_key={}, transaction_id={}"

2. **Create DuplicatePaymentHandler:**
   - @Service component
   - Method: `handleDuplicatePayment(UUID merchantId, UUID idempotencyKey, PaymentRequest request)` → Transaction
     - Queries database for existing transaction
     - Validates payload hash matches
     - Returns existing transaction or throws exception
   - Method: `buildDuplicateResponse(Transaction transaction)` → ResponseEntity<PaymentResponseDTO>
     - Terminal states (COMPLETED/DECLINED/FAILED): 200 OK
     - UNKNOWN state: 202 Accepted
     - In-progress states: 409 Conflict
   - Method: `buildNewPaymentResponse(Transaction transaction)` → ResponseEntity<PaymentResponseDTO>
     - Terminal states: 200 OK
     - In-progress/UNKNOWN: 202 Accepted

3. **Create PaymentResponseDTO:**
   - Fields: transactionId, status, amount, currency, maskedCard, message, createdAt, updatedAt, idempotencyKey
   - Used for all payment response bodies
   - Never contains raw PAN or sensitive data

4. **Implement PaymentController:**
   - @PostMapping("/api/v1/payments")
   - Flow:
     1. Authenticate merchant (placeholder for now)
     2. Validate request payload
     3. Check for duplicate (IdempotencyService.checkDuplicate)
     4. If duplicate found: return cached response
     5. If new request: create transaction with payload hash
     6. Handle DataIntegrityViolationException: fallback recovery
   - Exception handling: PaymentValidationException → 400, DuplicatePaymentException → 409

5. **Enhance GlobalExceptionHandler:**
   - Add handler for DuplicatePaymentException → 409 Conflict
   - Add handler for PaymentValidationException → 400 Bad Request
   - Standardized error response format

6. **Database UNIQUE Constraint:**
   - Already exists from task-001: UNIQUE(merchant_id, idempotency_key)
   - Enforces idempotency at database level
   - Fallback recovery handles constraint violations

## Acceptance Criteria & Tests

**Success cases:**
- ✓ New payment request creates transaction with payload hash
- ✓ Duplicate request with same payload returns cached response (200 OK)
- ✓ Duplicate request with different payload returns 400 Bad Request
- ✓ Duplicate COMPLETED transaction returns 200 OK
- ✓ Duplicate UNKNOWN transaction returns 202 Accepted
- ✓ Duplicate PROCESSING transaction returns 409 Conflict
- ✓ Payload hash persisted correctly (64-char hex)
- ✓ Different amounts produce different payload hashes
- ✓ Transaction version initialized to 0
- ✓ Transaction timestamps set correctly
- ✓ Merchant isolation enforced
- ✓ Idempotency key isolation enforced
- ✓ DataIntegrityViolationException handled gracefully

**Failure cases:**
- ✗ Duplicate with different payload rejected (400 Bad Request)
- ✗ Constraint violation without transaction found (exception thrown)
- ✗ Payload hash mismatch detected (validation error)

**Required tests:**
- Unit test: DuplicatePaymentHandlerTest (10 tests)
  - Handle duplicate payment recovery
  - Build response for COMPLETED/DECLINED/FAILED (200 OK)
  - Build response for UNKNOWN (202 Accepted)
  - Build response for in-progress states (409 Conflict)
  - Response contains transaction details
  - Exception handling for missing transaction
  - Exception handling for payload hash mismatch
- Integration test: PaymentControllerDuplicateTest (15 tests)
  - New payment creates transaction
  - Duplicate same payload returns cached response
  - Duplicate different payload returns 400
  - Duplicate UNKNOWN returns 202
  - Duplicate PROCESSING returns 409
  - Payload hash validation
  - Merchant isolation
  - Transaction persistence
  - Timestamps and version

**Verification:**
```bash
mvn test -Dtest="*DuplicatePaymentHandler*,*PaymentControllerDuplicate*"
```

## Implementation Details

### Payment Flow (Task-009)

```
POST /api/v1/payments
├─ Authenticate merchant
├─ Validate request payload
├─ Check for duplicate (IdempotencyService.checkDuplicate)
│  ├─ Query: findByMerchantIdAndIdempotencyKey
│  ├─ If found: validate payload hash
│  │  ├─ Hash matches: return existing transaction
│  │  └─ Hash differs: throw PaymentValidationException (400)
│  └─ If not found: proceed to new transaction
├─ Create new transaction
│  ├─ Prepare payload hash (IdempotencyService.preparePayloadHash)
│  ├─ Build Transaction object
│  └─ Persist to database
│     ├─ Success: return response
│     └─ DataIntegrityViolationException: fallback recovery
│        ├─ DuplicatePaymentHandler.handleDuplicatePayment
│        ├─ Query database for existing transaction
│        ├─ Validate payload hash
│        └─ Return cached response
└─ Build response based on transaction status
   ├─ Terminal states: 200 OK
   ├─ UNKNOWN: 202 Accepted
   └─ In-progress: 409 Conflict
```

### Duplicate Detection Rules

| Scenario | Behavior | HTTP Status |
|----------|----------|-------------|
| New request | Create transaction | 202 Accepted |
| Duplicate + hash matches + COMPLETED | Return cached | 200 OK |
| Duplicate + hash matches + UNKNOWN | Return uncertain | 202 Accepted |
| Duplicate + hash matches + PROCESSING | Return conflict | 409 Conflict |
| Duplicate + hash differs | Validation error | 400 Bad Request |
| Constraint violation + no transaction | Exception | 500 Error |

### HTTP Response Status Codes

- **200 OK:** Payment completed (COMPLETED/DECLINED/FAILED)
- **202 Accepted:** Payment processing (UNKNOWN/in-progress states)
- **400 Bad Request:** Validation error or payload mismatch
- **409 Conflict:** Duplicate with in-progress payment
- **500 Internal Server Error:** Unexpected error

## Constraints & Negative Instructions

- Do NOT include raw PAN in response (use masked card only)
- Do NOT log sensitive data (API keys, card numbers)
- Do NOT auto-retry on constraint violation (fallback recovery only)
- Do NOT create new transaction if duplicate detected (return existing)
- Do NOT allow cross-merchant idempotency collision (merchant-scoped queries)
- Do NOT modify transaction after duplicate detected (return as-is)
- Do NOT implement Redis caching (deferred to task-008)
- Do NOT implement payment processing (deferred to phase 3)
