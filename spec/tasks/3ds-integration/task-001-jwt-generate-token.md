---
id: task-001
status: completed
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-005-3ds-core-payment-integration.md
  - spec/specs/spec-005-3ds-core-payment-integration.md
  - spec/tasks/auth-engine-core/task-003-jwt-utility.md
---

# Add JwtTokenProvider.generateToken

## Local Context

- **Directory:** `3ds-engine/`
- **Files to create:** none
- **Files to modify:**
  - `3ds-engine/src/main/java/com/acabouomony/engine/security/JwtTokenProvider.java` — add `generateToken` method
  - `3ds-engine/src/test/java/com/acabouomony/engine/security/JwtTokenProviderTest.java` — add unit tests
- **Local dependencies:** 
  - Existing `key` (HMAC SecretKey) and `expirationSeconds` fields already configured in `JwtTokenProvider` for the `verify()` method — no new config needed
  - JJWT 0.12.6 already on classpath

## Scope

1. Add `public String generateToken(String challengeId, String transactionId, String merchantId, long amount)` to `JwtTokenProvider`:
   - Claims: `challenge_id`, `transaction_id`, `merchant_id`, `amount` (stored as `long`)
   - `issuedAt` = `Instant.now()`; `expiration` = `now + expirationSeconds`
   - Sign with `signWith(key)` and return `compact()` string
   - Uses the same `key` and `expirationSeconds` already configured for `verify()`
2. Add unit tests to `JwtTokenProviderTest`:
   - Round-trip: `verify(generateToken(...))` succeeds and returns all claims correctly
   - `challenge_id` claim matches input
   - `transaction_id` claim matches input
   - `merchant_id` claim matches input
   - `amount` stored as `long` (not String, not BigDecimal)
   - Token expiration is `expirationSeconds` seconds from creation (±2s tolerance)

## Acceptance Criteria and Tests

- **Success:** `generateToken(...)` returns a non-null, non-empty JWT string
- **Round-trip:** Round-trip with `verify()` succeeds and all claims match
- **Amount type:** `amount` is stored as `long` in the JWT claim
- **Key reuse:** Token uses the same signing key as `verify()` — no new key material
- **Expiration:** Expiration = `expirationSeconds` from creation
- **Tests:** `JwtTokenProviderTest` unit tests verify all scenarios above

**Verification:**
```bash
cd 3ds-engine && mvn test -Dtest="JwtTokenProviderTest"
```

## Constraints and Negative Instructions

- Do NOT change the `verify()` method signature or behavior
- Do NOT introduce new `@Value` properties — reuse existing `key` and `expirationSeconds`
- Do NOT store `amount` as `BigDecimal` or `String` — use `long`
- Do NOT add a separate overloaded method for `BigDecimal` amount
- Do NOT generate UUID or build URLs in this method — it only signs claims
