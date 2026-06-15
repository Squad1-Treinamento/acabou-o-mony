---
id: task-011
status: completed
links:
  - spec/tech-plans/plan-001-core-payment-processing.md (Phase 3, Core Payment Processing)
  - spec/specs/spec-001-core-payment-processing.md (Mercado Pago Integration, Timeout Rules)
---

# 011 - Mercado Pago Integration Client

Develop robust outbound Mercado Pago client integration with strict timeout, error mapping, and wire format support.

## Local Context

**Files created:**
- `src/main/java/com/acabouomony/payment/domain/service/PaymentAcquirerClient.java` (interface)
- `src/main/java/com/acabouomony/payment/domain/dto/PaymentResult.java` (DTO)
- `src/main/java/com/acabouomony/payment/infrastructure/client/dto/MercadoPagoRequest.java` (DTO)
- `src/main/java/com/acabouomony/payment/infrastructure/client/dto/MercadoPagoResponse.java` (DTO)
- `src/main/java/com/acabouomony/payment/infrastructure/client/exception/MercadoPagoException.java` (exception)
- `src/main/java/com/acabouomony/payment/infrastructure/client/MercadoPagoClient.java` (implementation)
- `src/test/java/com/acabouomony/payment/infrastructure/client/MercadoPagoClientIntegrationTest.java` (integration tests)

**Local dependencies:**
- Transaction entity (from task-001)
- PaymentStatus enum (from task-002)
- TransactionRepository (from task-003)
- PaymentResponseDTO (from task-009)
- RestTemplate (Spring Framework)

## Scope

1. **Create PaymentAcquirerClient Interface:**
   - Abstraction for payment acquirer integration
   - Methods:
     - `submitPayment(Transaction transaction)` → PaymentResult
     - `queryPaymentStatus(String acquirerReference)` → PaymentStatus
   - Isolates Mercado Pago details behind stable interface
   - Allows future acquirer implementations

2. **Create PaymentResult DTO:**
   - Result of payment submission
   - Fields: acquirerReference, status, message, timestamp
   - Contains acquirer's payment ID and final status

3. **Create Mercado Pago DTOs:**
   - MercadoPagoRequest: Wire format for payment submission
     - Fields: amount, currencyId, description, payer, paymentMethodId, token, externalReference, notificationUrl
   - MercadoPagoResponse: Wire format for payment response
     - Fields: id, status, statusDetail, transactionAmount, currencyId, dateCreated, dateLastUpdated, externalReference, authorizationCode

4. **Create MercadoPagoException:**
   - Custom exception for Mercado Pago API errors
   - Contains: httpStatus, errorCode, retryable flag
   - Distinguishes retryable (5xx, timeout) from non-retryable (4xx) errors

5. **Create MercadoPagoClient Implementation:**
   - @Component implementation of PaymentAcquirerClient
   - HTTP client configuration:
     - Connect timeout: 500ms
     - Read timeout: 2000ms
     - Max retries: 3 with exponential backoff (100ms, 200ms, 400ms)
   - Methods:
     - `submitPayment()`: Maps transaction to request, submits, handles timeout
     - `queryPaymentStatus()`: Queries Mercado Pago for payment status
   - Error handling:
     - Timeout → PaymentStatus.UNKNOWN
     - 4xx errors → PaymentStatus.FAILED
     - 5xx errors → PaymentStatus.UNKNOWN (retryable)
   - Status mapping:
     - "approved" → COMPLETED
     - "rejected" → DECLINED
     - "pending" → PROCESSING
     - Other → UNKNOWN

6. **Write Integration Tests:**
   - Test payment submission
   - Test timeout handling
   - Test error handling
   - Test status query
   - Test null validation

## Acceptance Criteria & Tests

**Success cases:**
- ✓ Mercado Pago requests honor timeouts (500ms connect, 2000ms read)
- ✓ Request format maps transaction correctly
- ✓ Response format parses correctly
- ✓ Error codes map to appropriate PaymentStatus
- ✓ Timeout transitions to UNKNOWN
- ✓ Acquirer reference persisted
- ✓ Retry logic works with exponential backoff
- ✓ Status query works correctly

**Failure cases:**
- ✗ Timeout returns UNKNOWN (not FAILED)
- ✗ 4xx errors return FAILED (not UNKNOWN)
- ✗ 5xx errors return UNKNOWN (retryable)
- ✗ Null transaction throws IllegalArgumentException
- ✗ Null acquirer reference throws IllegalArgumentException

**Required tests:**
- Integration test: MercadoPagoClientIntegrationTest (8+ tests)
  - Payment submission
  - Timeout handling
  - Error handling
  - Status query
  - Null validation

**Verification:**
```bash
mvn test -Dtest="*MercadoPagoClient*"
```

## Implementation Details

### Timeout Configuration

- Connect timeout: 500ms (network establishment)
- Read timeout: 2000ms (waiting for response)
- Total timeout: 2500ms

### Retry Logic

- Max retries: 3
- Backoff: 100ms, 200ms, 400ms
- Retryable: timeout, 5xx errors
- Non-retryable: 4xx errors

### Error Handling

| HTTP Status | Error Code | PaymentStatus | Retryable |
|-------------|-----------|---|---|
| Timeout | TIMEOUT | UNKNOWN | Yes |
| 4xx | CLIENT_ERROR | FAILED | No |
| 5xx | SERVER_ERROR | UNKNOWN | Yes |
| Other | UNKNOWN | UNKNOWN | No |

### Status Mapping

| Mercado Pago Status | PaymentStatus |
|---|---|
| approved | COMPLETED |
| rejected | DECLINED |
| pending | PROCESSING |
| cancelled | DECLINED |
| refunded | DECLINED |
| Other | UNKNOWN |

### Request Mapping

Transaction → MercadoPagoRequest:
- amount → amount (cents)
- currency → currencyId
- id → externalReference
- cardTokenId → token
- maskedCard → payer info

### Response Mapping

MercadoPagoResponse → PaymentResult:
- id → acquirerReference
- status → status (mapped)
- statusDetail → message

## Constraints & Negative Instructions

- Do NOT implement actual HTTP calls (placeholder for now)
- Do NOT include raw PAN in requests
- Do NOT log sensitive data (API keys, tokens)
- Do NOT retry on 4xx errors (validation/auth errors)
- Do NOT modify transaction in client (caller's responsibility)
- Do NOT implement 3DS in this task (deferred to phase 6)
- Do NOT implement reconciliation in this task (deferred to phase 4a)
- Do NOT cache responses in cbility)
