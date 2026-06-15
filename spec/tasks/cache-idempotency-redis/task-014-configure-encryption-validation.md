---
id: task-014
status: planned
links:
  - spec/tasks/index.md
  - spec/specs/spec-004-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-004-cache-and-idempotency-layer.md
  - spec/tasks/task-013-implement-aes-encryptor.md
---

# Configure Encryption Key Validation on Startup

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Create:**
- `src/main/java/com/acabouomony/core/config/EncryptionConfig.java` (new configuration class)

**Dependencies:**
- `@Configuration` (from Spring)
- `@ConditionalOnProperty` (from Spring Boot)
- `@Value` (from Spring)
- `@PostConstruct` (from javax.annotation)

**Configuration Values:**
- Encryption enabled flag: `${CACHE_ENCRYPT_SENSITIVE_DATA:false}`
- Encryption key: `${CACHE_ENCRYPTION_KEY:}`
- Injected via `@Value` annotations

**Validation Rules:**
- If `CACHE_ENCRYPT_SENSITIVE_DATA=true`, `CACHE_ENCRYPTION_KEY` must be defined
- If `CACHE_ENCRYPTION_KEY` is defined, it must be valid base64 and 256 bits (32 bytes)
- If validation fails, application must fail to start (fail-fast)

## Scope

1. Create package `com.acabouomony.core.config` (if not exists)
2. Create `EncryptionConfig.java` class annotated with `@Configuration`
3. Inject configuration values via `@Value`:
   - `@Value("${cache.encrypt-sensitive-data:false}") boolean encryptionEnabled`
   - `@Value("${cache.encryption.key:}") String encryptionKey`
4. Implement `@PostConstruct` method `validateEncryptionConfig()`:
   - If `encryptionEnabled == true`:
     - Validate `encryptionKey` is not empty
     - Validate `encryptionKey` is valid base64
     - Decode base64 and validate length is 32 bytes (256 bits)
     - Throw `IllegalStateException` if validation fails
   - If `encryptionEnabled == false`:
     - Log warning if `encryptionKey` is defined but not used
     - No validation needed (encryption disabled)
5. Add logging for encryption status (enabled/disabled)
6. Verify application fails to start if encryption is enabled but key is invalid

**Implementation Notes:**
- Use `@PostConstruct` to run validation after bean creation
- Throw `IllegalStateException` to fail application startup
- Log at `INFO` level if encryption is disabled (default)
- Log at `WARN` level if encryption key is defined but encryption is disabled
- Log at `INFO` level if encryption is enabled and key is valid

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: Configuration Class Structure**
- Class `EncryptionConfig` exists in package `com.acabouomony.core.config`
- Class is annotated with `@Configuration`
- Fields `encryptionEnabled` and `encryptionKey` are injected via `@Value`

**AC-2: Validation Method**
- Method `validateEncryptionConfig()` is annotated with `@PostConstruct`
- If `encryptionEnabled == true` and `encryptionKey` is empty:
  - Throws `IllegalStateException` with message "CACHE_ENCRYPTION_KEY is required when encryption is enabled"
- If `encryptionEnabled == true` and `encryptionKey` is invalid base64:
  - Throws `IllegalStateException` with message "CACHE_ENCRYPTION_KEY must be valid base64"
- If `encryptionEnabled == true` and `encryptionKey` is not 256 bits:
  - Throws `IllegalStateException` with message "CACHE_ENCRYPTION_KEY must be 256 bits (32 bytes)"

**AC-3: Application Startup Behavior**
- If `CACHE_ENCRYPT_SENSITIVE_DATA=false` (default):
  - Application starts successfully
  - Log: `INFO - Cache encryption is disabled (default)`
- If `CACHE_ENCRYPT_SENSITIVE_DATA=true` and key is valid:
  - Application starts successfully
  - Log: `INFO - Cache encryption is enabled with valid key`
- If `CACHE_ENCRYPT_SENSITIVE_DATA=true` and key is invalid:
  - Application fails to start
  - Log: `ERROR - CACHE_ENCRYPTION_KEY is required when encryption is enabled`

**AC-4: Warning for Unused Key**
- If `CACHE_ENCRYPT_SENSITIVE_DATA=false` and `CACHE_ENCRYPTION_KEY` is defined:
  - Application starts successfully
  - Log: `WARN - CACHE_ENCRYPTION_KEY is defined but encryption is disabled`

### Failure Cases

- Application fails to start if encryption is enabled but key is missing
- Application fails to start if encryption is enabled but key is invalid base64
- Application fails to start if encryption is enabled but key is wrong length

### Validation Commands

```bash
# Test 1: Encryption disabled (default) - should start successfully
export CACHE_ENCRYPT_SENSITIVE_DATA=false
mvn spring-boot:run
# Expected: Application starts, log "Cache encryption is disabled (default)"

# Test 2: Encryption enabled with valid key - should start successfully
export CACHE_ENCRYPT_SENSITIVE_DATA=true
export CACHE_ENCRYPTION_KEY=$(openssl rand -base64 32)
mvn spring-boot:run
# Expected: Application starts, log "Cache encryption is enabled with valid key"

# Test 3: Encryption enabled with missing key - should fail
export CACHE_ENCRYPT_SENSITIVE_DATA=true
unset CACHE_ENCRYPTION_KEY
mvn spring-boot:run
# Expected: Application fails, log "CACHE_ENCRYPTION_KEY is required when encryption is enabled"

# Test 4: Encryption enabled with invalid key - should fail
export CACHE_ENCRYPT_SENSITIVE_DATA=true
export CACHE_ENCRYPTION_KEY="invalid_base64"
mvn spring-boot:run
# Expected: Application fails, log "CACHE_ENCRYPTION_KEY must be valid base64"

# Test 5: Encryption enabled with wrong key length - should fail
export CACHE_ENCRYPT_SENSITIVE_DATA=true
export CACHE_ENCRYPTION_KEY=$(openssl rand -base64 16)  # 128-bit key
mvn spring-boot:run
# Expected: Application fails, log "CACHE_ENCRYPTION_KEY must be 256 bits (32 bytes)"

# Test 6: Encryption disabled but key defined - should start with warning
export CACHE_ENCRYPT_SENSITIVE_DATA=false
export CACHE_ENCRYPTION_KEY=$(openssl rand -base64 32)
mvn spring-boot:run
# Expected: Application starts, log "CACHE_ENCRYPTION_KEY is defined but encryption is disabled"
```

### Optional Unit Tests

If unit tests are desired:
- Test validation passes when encryption is disabled
- Test validation passes when encryption is enabled with valid key
- Test validation fails when encryption is enabled but key is missing
- Test validation fails when encryption is enabled but key is invalid base64
- Test validation fails when encryption is enabled but key is wrong length
- Test warning logged when key is defined but encryption is disabled

## Constraints and Negative Instructions

**DO:**
- Use `@PostConstruct` for validation (runs after bean creation)
- Throw `IllegalStateException` to fail application startup
- Validate base64 encoding with `Base64.getDecoder().decode()`
- Validate key length is exactly 32 bytes (256 bits)
- Log at appropriate levels (INFO, WARN, ERROR)
- Fail fast (do not allow application to start with invalid configuration)

**DO NOT:**
- Skip validation (always validate if encryption is enabled)
- Log encryption key value (security risk)
- Allow application to start with invalid key
- Use `@ConditionalOnProperty` to skip validation (always run)
- Implement encryption logic (handled in `AESEncryptor` - task-013)

**Out of Scope:**
- `AESEncryptor` implementation - task-013
- Encryption usage in cache layer (not used in MVP)
- Key rotation (future work)
- Documentation - task-015
