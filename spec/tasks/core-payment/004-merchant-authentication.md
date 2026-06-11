---
id: task-004
status: complete
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

## Authentication Strategy & Design Rationale

### Why "Fetch All & Verify" Instead of "Hash & Query"?

Argon2 (and other salted hashing algorithms) generate a **different hash for the same input every time** due to random salt. This makes direct database queries by hash impossible.

**Incorrect approach (WILL NOT WORK):**
```
1. Receive API key from request
2. Hash it: hashedKey = argon2.encode(apiKey)
3. Query: SELECT * FROM merchants WHERE api_key_hash = hashedKey
4. Result: ALWAYS EMPTY (because the hash is different from stored hash)
```

**Correct approach (IMPLEMENTED):**
```
1. Receive API key from request
2. Fetch all merchants: List<Merchant> all = repository.findAll()
3. For each merchant:
   - Use timing-safe comparison: argon2.matches(apiKey, merchant.getApiKeyHash())
   - If matches, return merchant
4. If no match found, return empty Optional
```

The `matches()` method extracts the salt from the stored hash and re-hashes the input for comparison, making it work correctly with salted hashing.

**Trade-off:** This approach queries all merchants (inefficient for large counts) but is the simplest **correct and secure** implementation. For production systems with many merchants, consider:
- Adding a merchant_id index and requiring it in the request
- Using a dedicated API key table with indexed lookups
- Implementing caching with short TTL

### Timing-Safe Comparison

The `Argon2PasswordEncoder.matches()` method implements constant-time comparison, preventing timing attacks where an attacker could measure comparison duration to infer correct characters.

## Scope

1. **Create Merchant JPA entity (V2 migration):**
   - merchants table: id (UUID PK), merchant_id (UUID unique), api_key_hash (VARCHAR hashed), webhook_url (VARCHAR), created_at (TIMESTAMP)
   - No plaintext API keys stored

2. **Create MerchantRepository:**
   - Extends JpaRepository<Merchant, UUID>
   - Method: `findByMerchantId(UUID merchantId)`
   - **IMPORTANT**: No `findByApiKeyHash()` method - incompatible with salted hashing (Argon2 generates different hashes for same input)

3. **Create MerchantAuthService:**
   - @Service component
   - Method: `authenticate(String apiKey)` → Optional<Merchant>
   - Logic:
     - Fetch ALL merchants from database (simple approach, necessary due to salted hashing)
     - For each merchant, use timing-safe comparison: `passwordEncoder.matches(apiKey, merchant.getApiKeyHash())`
     - Return first merchant that matches, or empty Optional if none match
     - Never log plaintext API key
     - Log only: "Authentication attempt initiated" and "Authentication successful for merchant {merchant_id}" or "Authentication failed"
   - Method: `verifyApiKey(String plaintext, String hash)` → boolean (public, uses Argon2's timing-safe comparison)
   - **IMPORTANT**: Do NOT hash incoming key and query by hash - Argon2 generates different hashes each time due to random salt

4. **Create ApiKeyAuthenticationFilter:**
   - Spring Security filter or custom @Controller method interceptor
   - Extract API key from request header: `Authorization: Bearer {api_key}`
   - Validate key using MerchantAuthService
   - On success: set SecurityContext or request attribute with Merchant
   - On failure: return 401 Unauthorized

5. **Security constraints:**
   - API key hash computed using Argon2 with secure parameters: saltLength=16, hashLength=32, memory=65536 KB (64 MB), iterations=3, parallelism=1
   - **Note**: Argon2's security comes from the memory parameter (65536 KB), not just iterations. The combination of memory + iterations + parallelism provides strong protection.
   - Timing-safe comparison (prevent timing attacks): use `Argon2PasswordEncoder.matches()` which implements constant-time comparison
   - Never compare plaintext keys directly
   - Never hash incoming key and query database by hash (impossible with salted hashing)

## Acceptance Criteria & Tests

**Success cases:**
- ✓ Valid API key authenticated, returns Merchant with correct merchant_id
- ✓ API key hashed correctly (different hashes for same input with salt)
- ✓ Request header parsed correctly: `Authorization: Bearer {api_key}`
- ✓ Timing-safe comparison used (Argon2PasswordEncoder.matches())

**Failure cases:**
- ✗ Invalid API key rejected (returns empty Optional)
- ✗ Plaintext API key logged (security violation)
- ✗ Plaintext API key stored in database (returns hashed value only)
- ✗ Authentication with wrong merchant_id rejected
- ✗ Request without Authorization header returns 401
- ✗ Request to protected endpoint without valid API key returns 401

**Required tests:**
- Unit test: API key hash generated with salt, different hashes for same input
- Unit test: Timing-safe comparison prevents timing attacks (Argon2PasswordEncoder.matches() used)
- Unit test: Authenticate with valid API key, returns correct Merchant
- Unit test: Authenticate with invalid API key, returns empty Optional
- Integration test: Request to PROTECTED endpoint with valid key succeeds (200 OK)
- Integration test: Request to PROTECTED endpoint with invalid key fails (401 Unauthorized)
- Integration test: Request to PROTECTED endpoint without Authorization header fails (401)
- Integration test: Request to PROTECTED endpoint with missing Bearer prefix fails (401)
- Integration test: Request to PROTECTED endpoint with invalid Bearer format fails (401)
- Security test: Plaintext API key never appears in logs
- Security test: All merchants are queried and verified (no direct hash lookup)

**Verification:**
```bash
mvn test -Dtest="*AuthService,*AuthFilter,*MerchantAuth*"
```

## Constraints & Negative Instructions

- Do NOT store plaintext API keys
- Do NOT log API keys (log only merchant_id)
- Do NOT use simple MD5/SHA1 hashing (use Argon2 or BCrypt)
- Do NOT implement custom timing-safe comparison (use tested library like Argon2PasswordEncoder.matches())
- Do NOT create user/password authentication (API key only)
- Do NOT cache authentication for extended period (validate each request)
- **Do NOT hash incoming API key and query database by hash** - Argon2 generates different hashes each time due to random salt
- **Do NOT use findByApiKeyHash() method** - incompatible with salted hashing approach
- **MUST fetch all merchants and verify each one** using timing-safe comparison (simple but correct approach)
