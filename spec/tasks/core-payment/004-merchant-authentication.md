---
id: task-004
status: in-progress
links:
  - spec/tech-plans/plan-001-core-payment-processing.md (Phase 2, merchant authentication)
  - spec/specs/spec-001-core-payment-processing.md (Security Rules)
---

# Implement Merchant Authentication & API Key Validation

Create secure merchant authentication with hashed API keys and timing-safe comparison.

## Local Context

**Files to create/modify:**
- `src/main/java/com/acabouomony/payment/domain/entity/Merchant.java` (JPA entity)
- `src/main/java/com/acabouomony/payment/domain/repository/MerchantRepository.java`
- `src/main/java/com/acabouomony/payment/domain/service/MerchantAuthService.java`
- `src/main/java/com/acabouomony/payment/infrastructure/security/ApiKeyAuthenticationFilter.java`
- `src/main/resources/db/migration/V2__create_merchants_schema.sql` (Flyway)

**Local dependencies:**
- Spring Security (optional but recommended)
- Argon2 or BCrypt for key hashing
- Transaction entity (from task-001)

## Scope

1. **Create Merchant JPA entity (V2 migration):**
   - merchants table: id (UUID PK), merchant_id (UUID unique), api_key_hash (VARCHAR hashed), webhook_url (VARCHAR), created_at (TIMESTAMP)
   - No plaintext API keys stored

2. **Create MerchantRepository:**
   - Extends JpaRepository<Merchant, UUID>
   - Method: `findByMerchantId(UUID merchantId)`
   - No method to find by plaintext API key (security)

3. **Create MerchantAuthService:**
   - @Service component
   - Method: `authenticate(String apiKey)` → Optional<Merchant>
   - Logic:
     - Hash incoming API key with Argon2/BCrypt
     - Query database for matching api_key_hash
     - Never log plaintext API key
     - Log only: "Authentication attempt for merchant {merchant_id}"
     - Return Merchant if found, null otherwise
   - Method: `hashApiKey(String plaintext)` → String (private, idempotent)

4. **Create ApiKeyAuthenticationFilter:**
   - Spring Security filter or custom @Controller method interceptor
   - Extract API key from request header: `Authorization: Bearer {api_key}`
   - Validate key using MerchantAuthService
   - On success: set SecurityContext or request attribute with Merchant
   - On failure: return 401 Unauthorized

5. **Security constraints:**
   - API key hash computed using Argon2 with high cost factor (2^16 iterations minimum)
   - Timing-safe comparison (prevent timing attacks): use equals(hash1, hash2) with constant-time logic
   - Never compare plaintext keys directly

## Acceptance Criteria & Tests

**Success cases:**
- ✓ Valid API key authenticated, returns Merchant with correct merchant_id
- ✓ API key hashed correctly (different hashes for same input with salt)
- ✓ Request header parsed correctly: `Authorization: Bearer {api_key}`

**Failure cases:**
- ✗ Invalid API key rejected (returns null)
- ✗ Plaintext API key logged (security violation)
- ✗ Plaintext API key stored in database (returns hashed value only)
- ✗ Authentication with wrong merchant_id rejected
- ✗ Request without Authorization header returns 401

**Required tests:**
- Unit test: API key hash generated with salt, different hashes for same input
- Unit test: Timing-safe comparison prevents timing attacks (all comparisons same duration)
- Integration test: Authenticate with valid API key, returns correct Merchant
- Integration test: Authenticate with invalid API key, returns null
- Integration test: Request with valid key succeeds (200 OK)
- Integration test: Request with invalid key fails (401 Unauthorized)
- Integration test: Request without Authorization header fails (401)
- Security test: Plaintext API key never appears in logs

**Verification:**
```bash
mvn test -Dtest="*AuthService,*AuthFilter,*MerchantAuth*"
```

## Constraints & Negative Instructions

- Do NOT store plaintext API keys
- Do NOT log API keys (log only merchant_id)
- Do NOT use simple MD5/SHA1 hashing (use Argon2 or BCrypt)
- Do NOT implement custom timing-safe comparison (use tested library)
- Do NOT create user/password authentication (API key only)
- Do NOT cache authentication for extended period (validate each request)
