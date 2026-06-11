---
id: task-013
status: planned
links:
  - spec/tasks/index.md
  - spec/specs/spec-002-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-002-cache-and-idempotency-layer.md
  - spec/tasks/task-012-add-encryption-maven-dependency.md
---

# Implement AESEncryptor Class for Cache Encryption

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Create:**
- `src/main/java/com/acabouomony/core/security/AESEncryptor.java` (new utility class)

**Dependencies:**
- `Cipher` (from javax.crypto)
- `SecretKey`, `SecretKeySpec` (from javax.crypto.spec)
- `GCMParameterSpec` (from javax.crypto.spec)
- `SecureRandom` (from java.security)
- `Base64` (from java.util)

**Configuration Values:**
- Encryption key: `${CACHE_ENCRYPTION_KEY}` (base64-encoded, 256 bits)
- Injected via `@Value` annotation

**Algorithm:**
- AES-256-GCM (Galois/Counter Mode)
- IV size: 12 bytes (GCM standard)
- Tag size: 128 bits (authentication tag)

## Scope

1. Create package `com.acabouomony.core.security` (if not exists)
2. Create `AESEncryptor.java` class annotated with `@Component`
3. Inject encryption key via `@Value("${cache.encryption.key}") String base64Key`
4. Implement constructor:
   - Decode base64 key to byte array
   - Create `SecretKeySpec` with algorithm "AES"
   - Validate key length is 256 bits (32 bytes)
   - Throw exception if key is invalid (fail-fast)
5. Implement `encrypt(String plaintext)` method:
   - Generate random 12-byte IV (Initialization Vector)
   - Create `GCMParameterSpec` with 128-bit tag size and IV
   - Initialize `Cipher` with `ENCRYPT_MODE`, secret key, and GCM spec
   - Encrypt plaintext to ciphertext
   - Combine IV + ciphertext into single byte array
   - Encode to base64 and return
6. Implement `decrypt(String encryptedBase64)` method:
   - Decode base64 to byte array
   - Extract IV (first 12 bytes) and ciphertext (remaining bytes)
   - Create `GCMParameterSpec` with 128-bit tag size and IVCipher` with `DECRYPT_MODE`, secret key, and GCM spec
   - Decrypt ciphertext to plaintext
   - Return plaintext string
7. Add error handling for invalid keys, encryption/decryption
   - Initialize ` failures

**Implementation Notes:**
- Use `SecureRandom` for IV generation (cryptographically secure)
- GCM mode provides both encryption and authentication (no separate HMAC needed)
- IV must be unique for each encryption (never reuse)
- Store IV with ciphertext (prepend to encrypted data)
- Base64 encoding for Redis storage (human-readable)

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: Class Structure**
- Class `AESEncryptor` exists in package `com.acabouomony.core.security`
- Class is annotated with `@Component`
- Constructor injects `@Value("${cache.encryption.key}") String base64Key`
- Constructor validates key length is 256 bits (32 bytes)
- Constructor throws `IllegalArgumentException` if key is invalid

**AC-2: encrypt() Method**
- Method signature: `String encrypt(String plaintext) throws Exception`
- Generates random 12-byte IV for each encryption
- Uses AES-256-GCM algorithm
- Returns base64-encoded string (IV + ciphertext)
- Different ciphertexts for same plaintext (due to random IV)

**AC-3: decrypt() Method**
- Method signature: `String decrypt(String encryptedBase64) throws Exception`
- Decodes base64 to byte array
- Extracts IV (first 12 bytes) and ciphertext (remaining bytes)
- Uses AES-256-GCM algorithm
- Returns original plaintext

**AC-4: Encrypt/Decrypt Roundtrip**
- Encrypting and then decrypting returns original plaintext
- Works for various plaintext lengths (empty, short, long)
- Works for special characters (UTF-8 encoding)

**AC-5: Error Handling**
- Invalid key length throws `IllegalArgumentException` on initialization
- Invalid base64 throws exception on decrypt
- Tampered ciphertext throws `AEADBadTagException` (GCM authentication failure)

### Failure Cases

- Application fails to start if `CACHE_ENCRYPTION_KEY` is invalid (when encryption enabled)
- Decrypt fails if ciphertext is tampered (GCM authentication)
- Decrypt fails if IV is corrupted
- Encrypt/decrypt fails if key is wrong length

### Validation Commands

```bash
# Generate encryption key (for testing)
openssl rand -base64 32
# Example output: "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6=="

# Set environment variable (for testing)
export CACHE_ENCRYPTION_KEY="a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6=="

# Start application (encryption not used in MVP, but class should load)
mvn spring-boot:run

# Check logs for successful bean creation
# Expected: "Bean 'aesEncryptor' of type [AESEncryptor] is registered"
```

### Optional Unit Tests

If unit tests are desired:
- Test encrypt/decrypt roundtrip with various plaintexts
- Test invalid key length throws exception
- Test tampered ciphertext throws `AEADBadTagException`
- Test empty plaintext handling
- Test special characters (UTF-8)

**Example Unit Test:**
```java
@Test
void testEncryptDecryptRoundtrip() throws Exception {
    String key = Base64.getEncoder().encodeToString(new byte[32]); // 256-bit key
    AESEncryptor encryptor = new AESEncryptor(key);
    
    String plaintext = "sensitive_data_123";
    String encrypted = encryptor.encrypt(plaintext);
    String decrypted = encryptor.decrypt(encrypted);
    
    assertEquals(plaintext, decrypted);
}

@Test
void testInvalidKeyLengthThrowsException() {
    String invalidKey = Base64.getEncoder().encodeToString(new byte[16]); // 128-bit (invalid)
    
    assertThrows(IllegalArgumentException.class, () -> {
        new AESEncryptor(invalidKey);
    });
}

@Test
void testTamperedCiphertextThrowsException() throws Exception {
    String key = Base64.getEncoder().encodeToString(new byte[32]);
    AESEncryptor encryptor = new AESEncryptor(key);
    
    String encrypted = encryptor.encrypt("plaintext");
    String tampered = encrypted.substring(0, encrypted.length() - 5) + "XXXXX";
    
    assertThrows(AEADBadTagException.class, () -> {
        encryptor.decrypt(tampered);
    });
}
```

## Constraints and Negative Instructions

**DO:**
- Use AES-256-GCM algorithm (not AES-CBC or AES-ECB)
- Generate random IV for each encryption (use `SecureRandom`)
- Validate key length is 256 bits (32 bytes)
- Throw exception on invalid key (fail-fast)
- Use base64 encoding for Redis storage
- Prepend IV to ciphertext (store together)

**DO NOT:**
- Reuse IV (must be unique for each encryption)
- Use AES-CBC or AES-ECB (less secure than GCM)
- Hard-code encryption key (always inject from configuration)
- Use 128-bit or 192-bit keys (use 256-bit only)
- Implement custom authentication (GCM provides it)
- Use encryption in MVP (infrastructure preparation only)

**Out of Scope:**
- Encryption configuration validation - task-014
- Encryption usage in cache layer (not used in MVP)
- Key rotation (future work)
- Integration tests - optional
