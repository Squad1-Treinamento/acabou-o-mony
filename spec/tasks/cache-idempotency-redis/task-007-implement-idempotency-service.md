---
id: task-007
status: planned
links:
  - spec/tasks/index.md
  - spec/specs/spec-002-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-002-cache-and-idempotency-layer.md
  - spec/tasks/task-003-implement-redis-config.md
---

# Implement IdempotencyService for Transaction Deduplication

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Create:**
- `src/main/java/com/acabouomony/core/service/IdempotencyService.java` (new service class)

**Dependencies:**
- `ReactiveRedisTemplate<String, Object>` (from task-003)
- `TransactionResult` (domain model, assumed to exist)
- Reactive types: `Mono<TransactionResult>`, `Mono<Void>`
- `Duration` (from java.time)

**Configuration Values:**
- Idempotency TTL: `${CACHE_TTL_IDEMPOTENCY_KEY:86400}` seconds (24 hours)
- Injected via `@Value` annotation

**Redis Namespace:**
- Keys: `core:idempotency:{idempotency_key}`
- Example: `core:idempotency:req_abc123`

## Scope

1. Create package `com.acabouomony.core.service` (if not exists)
2. Create `IdempotencyService.java` class annotated with `@Service`
3. Inject `ReactiveRedisTemplate<String, Object>` via constructor
4. Inject idempotency TTL via `@Value("${cache.ttl.idempotency-key}") long ttlSeconds`
5. Implement `checkIdempotency(String idempotencyKey)` method:
   - Build Redis key: `"core:idempotency:" + idempotencyKey`
   - Query Redis with `redisTemplate.opsForValue().get(key)`
   - Return `Mono<TransactionResult>` (empty if key not found)
   - Add logging for cache hit/miss (optional)
6. Implement `saveIdempotency(String idempotencyKey, TransactionResult result)` method:
   - Build Redis key: `"core:idempotency:" + idempotencyKey`
   - Save to Redis with TTL: `redisTemplate.opsForValue().set(key, result, Duration.ofSeconds(ttlSeconds))`
   - Return `Mono<Void>`
   - Add logging for cache write (optional)
7. Verify idempotency check and save work correctly

**Implementation Notes:**
- Use `ReactiveRedisTemplate` for non-blocking operations
- Use `opsForValue()` for simple key-value operations
- TTL is applied automatically on write (no separate `EXPIRE` command needed)
- `TransactionResult` should be serializable to JSON (Jackson)

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: Service Class Structure**
- Class `IdempotencyService` exists in package `com.acabouomony.core.service`
- Class is annotated with `@Service`
- Constructor injects `ReactiveRedisTemplate<String, Object>`
- Field `ttlSeconds` is injected via `@Value("${cache.ttl.idempotency-key}")`

**AC-2: checkIdempotency() Method**
- Method signature: `Mono<TransactionResult> checkIdempotency(String idempotencyKey)`
- Builds Redis key: `"core:idempotency:" + idempotencyKey`
- Queries Redis with `redisTemplate.opsForValue().get(key)`
- Returns `Mono<TransactionResult>` if key exists (cache hit)
- Returns empty `Mono` if key does not exist (cache miss)
- Latency < 5ms (p95)

**AC-3: saveIdempotency() Method**
- Method signature: `Mono<Void> saveIdempotency(String idempotencyKey, TransactionResult result)`
- Builds Redis key: `"core:idempotency:" + idempotencyKey`
- Saves to Redis with `redisTemplate.opsForValue().set(key, result, Duration.ofSeconds(ttlSeconds))`
- TTL is 24 hours (86400 seconds)
- Returns `Mono<Void>` on success

**AC-4: Redis Key Format**
- Keys follow format: `core:idempotency:{idempotency_key}`
- Example: `core:idempotency:req_abc123`
- Keys are human-readable (not binary)

**AC-5: TTL Applied**
- Cached entries expire after 24 hours
- TTL can be verified with `TTL` command in Redis CLI

**AC-6: JSON Serialization**
- `TransactionResult` is serialized to JSON (via Jackson)
- Deserialization works correctly on read

### Failure Cases

- Empty `Mono` returned if idempotency key not found (expected behavior)
- Redis unavailable → fallback to processing transaction (no cached result)
- Invalid `TransactionResult` (null) → should not be saved (validation in caller)

### Validation Commands

```bash
# Start application and Redis
docker-compose up -d redis
mvn spring-boot:run

# Manually test idempotency service (via REST endpoint or unit test)
# First request: Save idempotency key
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Idempotency-Key: req_test123" \
  -d '{"amount":1500,"currency":"BRL"}'

# Check Redis for saved key
docker exec redis redis-cli GET "core:idempotency:req_test123"
# Expected: JSON representation of TransactionResult

# Check TTL
docker exec redis redis-cli TTL "core:idempotency:req_test123"
# Expected: ~86400 (24 hours in seconds)

# Second request: Check idempotency (should return cached result)
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Idempotency-Key: req_test123" \
  -d '{"amount":1500,"currency":"BRL"}'
# Expected: Same response as first request, header "X-Idempotent-Replayed: true"
```

### Optional Unit Tests

If unit tests are desired:
- Test `checkIdempotency()` returns empty `Mono` when key not found
- Test `checkIdempotency()` returns `TransactionResult` when key exists
- Test `saveIdempotency()` saves to Redis with correct TTL
- Test JSON serialization/deserialization of `TransactionResult`
- Mock `ReactiveRedisTemplate` for isolated testing

## Constraints and Negative Instructions

**DO:**
- Use `ReactiveRedisTemplate` for non-blocking operations
- Use `opsForValue()` for simple key-value operations
- Inject TTL via `@Value` annotation (do not hard-code)
- Build Redis keys with prefix `"core:idempotency:"`
- Use `Duration.ofSeconds(ttlSeconds)` for TTL
- Return empty `Mono` when key not found (not an error)

**DO NOT:**
- Use blocking Redis operations (use reactive only)
- Hard-code TTL values (always inject from configuration)
- Throw exceptions when key not found (return empty `Mono`)
- Implement validation logic (handled in interceptor - task-008)
- Cache null values (validate in caller)

**Out of Scope:**
- Idempotency key validation (format, length) - task-008
- Interceptor implementation - task-008
- Endpoint integration - task-009
- Error caching logic (handled in caller)
- Integration tests - optional
