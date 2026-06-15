---
id: task-006
status: completed
links:
  - spec/tech-plans/plan-001-core-payment-processing.md (Phase 2, Card Validation)
  - spec/specs/spec-001-core-payment-processing.md (Card Validation Rules)
---

# 006 - Card Validation

## Description
Implement card validation, including Luhn check, expiry date, CVV requirements, and supported card type enforcement.

## Acceptance Criteria
- ✓ Card number passes Luhn check and conforms to supported types.
- ✓ Expiry and CVV are present, valid, and meet all reards are rejected with detailed erroquirements.
- ✓ Invalid cr codes.

## Implementation Steps
- ✓ Implement utility or validator for isolating card validation responsibility.

## Implementation Summary

**Files Created:**
- `src/main/java/com/acabouomony/payment/domain/model/CardType.java` - Card type enum with BIN detection
- `src/main/java/com/acabouomony/payment/domain/service/CardValidator.java` - Card validation service
- `src/main/java/com/acabouomony/payment/domain/exception/CardValidationException.java` - Custom exception
- `src/test/java/com/acabouomony/payment/domain/service/CardValidatorTest.java` - 40+ unit tests

**Files Modified:**
- `src/main/java/com/acabouomony/payment/infrastructure/exception/GlobalExceptionHandler.java` - Added CardValidationException handler

**Validation Rules Implemented:**
- Card Number: Luhn check, card type detection, length vaes (Visa, Malidation, supports 8 card typstercard, Amex, Discover, Diners, JCB, Elo, Hipercard)
- Expiry Date: format validation (MM/yy or MM/yyyy), expiration check (not expired)
- CVV: length validation (3 or 4 digits based on card type), digits only

**Card Types Supported:**
- Visa: 13, 16, or 19 digits, 3-digit CVV
- Mastercard: 16 digits, 3-digit CVV
- American Express: 15 digits, 4-digit CVV
- Discover: 16 digits, 3-digit CVV
- Diners Club: 14 digits, 3-digit CVV
- JCB: 16 digits, 3-digit CVV
- Elo: 16 digits, 3-digit CVV
- Hipercard: 16 digits, 3-digit CVV

**Test Coverage:**
- 40+ unit tests for validator
- Luhn check validation
- Card type detectEdge cases (spaces, dashes, multiple errors)
