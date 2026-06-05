---
id: task-007
status: complete
links:
  - spec/tech-plans/plan-001-core-payment-processing.md (Phase 2, Payload Hashing)
  - spec/specs/spec-001-core-payment-processing.md (Idempotency Rules, Payload Hash Collision Detection)
---

# 007 - Payload Hashing

Implement deterministic hashing of payment request payloads to support idempotency and duplicate detection.

## Local Context

**Files created:**
- `src/main/java/com/acabouomony/payment/domain/service/PayloadHashingService.java` (service)
- `src/main/java/com/acabouomony/payment/domain/service/IdempotencyService.java` (service)
- `src/test/java/com/acabouomony/payment/domain/service/PayloadHashingServiceTest.java` (unit tests)
- `src/test/java/com/acabouomony/payment/domain/service/IdempotencyServiceTest.java` (unit tests)
- `src/test/java/com/acabouomony/payment/domain/service/PayloadHashingIntegrationTest.java` (integration tests)

**Files modified:**
- `src/main/java/com/acabouomony/payment/infrastructure/persistence/TransactionRepository.java` (added query method)

**Local dependencies:**
- PaymentRequest DTO (from task-003)
- Transaction entity (from task-001)
- PaymentStatus enum (from task-002)
- Spring component/service annotations
- Jackson ObjectMapper for JSON serialization

## Scope

1. **Create PayloadHashingService:**
   - @Service component
   - Method: `computePayloadHash(PaymentRequest request)` → SHA-256 hex string (64 chars)
   - Method: `validatePayloadHash(PaymentRequest request, String storedHash)` → boolean
   - Canonical payload includes: amount, currency, payment_method.card_token_id, payment_method.masked_card, customer_id
   - Canonical payload excludes: idempotency_key, customer_email, timestamps
   - Deterministic: identical payloads always produce identical hashes
   - Uses SHA-256 algorithm with canonical JSON representation (sorted keys)

2. **Create IdempotencyService:**
   - @Service component
   - Method: `checkDuplicate(UUID merchantId, UUID idempotencyKey, PaymentRequest request)` → Optional<Transaction>
     - Queries database for existing transaction by merchant + idempotency_key
     - Validates payload hash matches
     - Throws PaymentValidationException if payload differs
     - Returns existing transaction if found and hash matches
   - Method: `isSafeToReturnCachedResponse(Transaction transaction)` → boolean
     - Terminal states (COMPLETED/DECLINED/FAILED): true
     - UNKNOWN state: false (return 202 Accepted)
     - In-progress states: false (return 409 Conflict)
   - Method: `preparePayloadHash(PaymentRequest request)` → String
     - Computes and returns payload hash for new transaction

3. **Enhance TransactionRepository:**
   - Add query method: `findByMerchantIdAndIdempotencyKey(UUID merchantId, UUID idempotencyKey)` → Optional<Transaction>
   - Merchant-scoped query prevents cross-merchant interference
   - Backed by database UNIQUE constraint

4. **Duplicate Detection Rules:**
   - No existing transaction → Return empty (new request)
   - Existing + hash matches → Return transaction (identical retry, safe to cache)
   - Existing + hash differs → Throw PaymentValidationException (different request, 400 Bad Request)

## Acceptance Criteria & Tests

**Success cases:**
- ✓ Identical payloads produce identical hashes
- ✓ Different amounts produce different hashes
- ✓ Different currencies produce different hashes
- ✓ Different card tokens produce different hashes
- ✓ Different masked cards produce different hashes
- ✓ Different customer IDs produce different hashes
- ✓ Hash is 64-character hexadecimal (SHA-256)
- ✓ Idempotency key excluded from hash (different keys, same hash)
- ✓ Customer email excluded from hash (different emails, same hash)
- ✓ Payload hash validation succeeds for matching hashes
- ✓ Duplicate detection finds existing transaction by merchant + idempotency_key
- ✓ Duplicate detection rejects different payload with same idempotency_key
- ✓ Different merchants can use same idempotency_key independently
- ✓ Payload hash persisted correctly in database
- ✓ Idempotency key isolation prevents cross-merchant interference
- ✓ Safe to return cached response for COMPLETED/DECLINED/FAILED
- ✓ NOT safe to return cached response for UNKNOWN/in-progress states

**Failure cases:**
- ✗ Different payloads produce different hashes
- ✗ Payload hash mismatch throws PaymentValidationException
- ✗ Same idempotency_key with different payload rejected

**Required tests:**
- Unit test: PayloadHashingServiceTest (12 tests)
  - DeterministicHashingTests: 5 tests
  - HashFormatTests: 1 test
  - PayloadHashValidationTests: 2 tests
  - FieldInclusionExclusionTests: 2 tests
  - EdgeCasesTests: 2 tests
- Unit test: IdempotencyServiceTest (13 tests)
  - DuplicateDetectionTests: 4 tests
  - CachedResponseSafetyTests: 8 tests
  - PayloadHashPreparationTests: 1 test
- Integration test: PayloadHashingIntegrationTest (6 tests)
  - Payload hash persisted correctly
  - Duplicate detection with database queries
  - Payload hash validation with persisted data
  - Merchant isolation

**Verification:**
```bash
mvn test -Dtest="*PayloadHashingServiceTest"
mvn test -Dtest="*IdempotencyServiceTest"
mvn test -Dtest="*PayloadHashingIntegrationTest"
```

## Constraints & Negative Instructions

- Do NOT include idempotency_key in payload hash (varies per request)
- Do NOT include customer_email in payload hash (not part of payment identity)
- Do NOT include timestamps in payload hash (not part of payment identity)
- Do NOT use non-deterministic hashing (must be SHA-256 with canonical JSON)
- Do NOT allow cross-merchant idempotency key collision (merchant-scoped queries)
- Do NOT add Redis caching to this task (fast-path optimization deferred)
- Do NOT add webhook dispatch to this task (Phase 4 responsibility)
- Do NOT add reconciliation logic to this task (Phase 4 responsibility)
