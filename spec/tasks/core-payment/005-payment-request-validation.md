---
id: task-005
status: complete
links:
  - spec/tech-plans/plan-001-core-payment-processing.md (Phase 2, Request Validation)
  - spec/specs/spec-001-core-payment-processing.md (Request Validation Rules)
---

# 005 - Payment Request Validation

## Description
Implement input validation for payment requests, including schema validation, mandatory field checks, type validation, and boundary (min/max) enforcement.

## Acceptance Criteria
- ✓ All mandatory fields are present and validated.
- ✓ Type and format checks pass for each request.
- ✓ Detailed validation errors are returned for malformed or incomplete requests.

## Implementation Steps
- ✓ Implement value object or DTO validation logic (Spring Validator, etc).

## Implementation Summary

**Files Created:**
- `src/main/java/com/acabouomony/payment/domain/dto/PaymentRequest.java` - DTO with validation annotations
- `src/main/java/com/acabouomony/payment/domain/service/PaymentRequestValidator.java` - Validation service
- `src/main/java/com/acabouomony/payment/domain/exception/PaymentValidationException.java` - Custom exception
- `src/main/java/com/acabouomony/payment/infrastructure/exception/GlobalExceptionHandler.java` - REST exception handler
- `src/test/java/com/acabouomony/payment/domain/service/PaymentRequestValidatorTest.java` - 30+ unit tests
- `src/test/java/com/acabouomony/payment/infrastructure/exception/PaymentRequestValidationIntegrationTest.java` - 15+ integration tests

**Validation Rules Implemented:**
- Amount: positive (> 0), max 1 billion cents ($10M)
- Currency: ISO 4217, exactly 3 uppercase letters, 20 supported currencies
- Idempotency Key: UUID format required
- Payment Method: card_token_id required, non-blank, max 100 chars
- Masked Card: optional, format validation (6 digits + X's + 4 digits)
- Customer ID: optional UUID
- Customer Email: optional, valid email format

**Test Coverage:**
- 30+ unit tests for validator
- 15+ integration tests for API
- 45+ total test cases
- All validation scenarios covered