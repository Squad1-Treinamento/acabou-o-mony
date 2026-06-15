---
id: task-004
status: planned
links:
  - spec/tasks/index.md
  - spec/specs/spec-004-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-004-cache-and-idempotency-layer.md
  - spec/tasks/task-003-implement-redis-config.md
---

# Implement MerchantService with Cache Annotations

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Modify:**
- `src/main/java/com/acabouomony/core/service/MerchantService.java` (existing service class)

**Dependencies:**
- `MerchantRepository` (existing repository, assumed to exist)
- Spring Cache annotations: `@Cacheable`, `@CacheEvict`
- Reactive types: `Mono<Merchant>` (from Project Reactor)

**Cache Configuration:**
- Cache name: `"merchants"` (configured in `RedisConfig` from task-003)
- Cache key: `#merchantId` (SpEL expression)
- TTL: 24 hours (configured in `application.yml`)

**Namespace in Redis:**
- Keys will be stored as: `core:merchant:{merchantId}`

## Scope

1. Open existing `MerchantService.java` class
2. Add `@Cacheable` annotation to `findById(String merchantId)` method:
   - Set `value = "merchants"`
   - Set `key = "#merchantId"`
   - Add logging for cache miss (optional but recommended)
3. Add `@CacheEvict` annotation to `update(String merchantId, Merchant data)` mevalue = "merchants"`
   - Set `key = "#merchantId"`
   - Add logging for cache eviction (optional but recommended)
4. Add `@CacheEvict`thod:
   - Set ` annotation to `delete(String **merchantId)` method:
   - Set `value = "merchants"`
   - Set `key =
- Use SpEL (Sprinks correctly

**Implemen "#merchantId"`
   - Add logging for cache eviction (optional but recommended)
5. Verify cache hit/miss behavior wortation Notes:g Expression Language) for cache keys: `#merchantId`
- Return type must be `Mono<Merchant>` (reactive)
- Repository calls should remain unchanged (cache is transparent)
- Logging is optional but helps debugging cache behavior

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: findById() Method Cached**
- Method `findById(String merchantId)` has `@Cacheable(value = "merchants", key = "#merchantId")` annotation
- First call queries database (cache miss)
- Second call with same `merchantId` returns from cache (cache hit)
- Cache hit latency < 10ms (p95)
- Cache miss latency < 100ms (p95)

**AC-2: update() Method Evicts Cache**
- Method `update(String merchantId, Merchant data)` has `@CacheEvict(value = "merchants", key = "#merchantId")` annotation
- After update, cache entry for `merchantId` is removed
- Next `findById()` call queries database (cache miss)
- Updated data is cached after query

**AC-3: delete() Method Evicts Cache**
- Method `delete(String merchantId)` has `@CacheEvict(value = "merchants", key = "#merchantId")` annotation
- After delete, cache entry for `merchantId` is removed
- Next `findById()` call returns empty `Mono` (merchant not found)

**AC-4: Redis Key Format**
- Cache keys in Redis follow format: `core:merchant:{merchantId}`
- Example: `core:merchant:m_123`
- Keys are human-readable (not binary)

**AC-5: TTL Configured**
- Cached entries expire after 24 hours (86400 seconds)
- TTL can be verified with `TTL` command in Redis CLI

### Failure Cases

- Cache miss if Redis is unavailable (fallback to database via circuit breaker)
- Cache eviction fails silently if Redis is unavailable (database update still succeeds)
- Invalid `merchantId` returns empty `Mono` (not cached)

### Validation Commands

```bash
# Start application and Redis
docker-compose up -d redis
mvn spring-boot:run

# Query merchant (cache miss)
curl http://localhost:8080/api/v1/merchants/m_123

# Check Redis for cached entry
docker exec redis redis-cli GET "core:merchant:m_123"
# Expected: JSON representation of merchant

# Check TTL
docker exec redis redis-cli TTL "core:merchant:m_123"
# Expected: ~86400 (24 hours in seconds)

# Query same merchant again (cache hit)
curl http://localhost:8080/api/v1/merchants/m_123
# Expected: Faster response (< 10ms)

# Update merchant
curl -X PUT http://localhost:8080/api/v1/merchants/m_123 -d '{"name":"Updated Name"}'

# Verify cache evicted
docker exec redis redis-cli GET "core:merchant:m_123"
# Expected: (nil) - key removed

# Query again (cache miss, then re-cached)
curl http://localhost:8080/api/v1/merchants/m_123
# Expected: Updated data returned and cached
```

### Optional Integration Tests

If integration tests are desired (using Testcontainers):
- Test cache hit: Query twice, verify database called only once
- Test cache miss: Query new merchant, verify database called
- Test cache eviction: Update merchant, verify cache cleared
- Test TTL expiration: Wait 24h (or mock time), verify cache expired

## Constraints and Negative Instructions

**DO:**
- Use Spring Cache annotations (`@Cacheable`, `@CacheEvict`)
- Use SpEL for cache keys: `#merchantId`
- Keep method signatures unchanged (reactive `Mono<Merchant>`)
- Add logging for cache operations (optional but recommended)
- Use cache name `"merchants"` (matches `RedisConfig`)

**DO NOT:**
- Implement manual Redis operations (use annotations only)
- Change repository method signatures
- Cache sensitive data (PAN, CVV, API keys) - only merchant config
- Add `@CachePut` annotation (not needed for this use case)
- Implement custom cache key generators (use SpEL)

**Out of Scope:**
- Risk thresholds cache - task-005
- BIN lookup cache - task-005
- Circuit breaker implementation - task-006
- Idempotency logic - task-007, task-008, task-009
- Integration tests - optional
