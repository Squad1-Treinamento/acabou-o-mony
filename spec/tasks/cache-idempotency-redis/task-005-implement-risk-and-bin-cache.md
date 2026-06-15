---
id: task-005
status: planned
links:
  - spec/tasks/index.md
  - spec/specs/spec-004-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-004-cache-and-idempotency-layer.md
  - spec/tasks/task-003-implement-redis-config.md
  - spec/tasks/task-004-implement-merchant-service-cache.md
---

# Implement Risk Thresholds and BIN Lookup Cache

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Modify:**
- `src/main/java/com/acabouomony/core/service/RiskService.java` (existing service class)
- `src/main/java/com/acabouomony/core/service/BinLookupService.java` (existing service class)

**Dependencies:**
- `RiskRepository` (existing repository, assumed to exist)
- `BinLookupRepository` or external BIN API (assumed to exist)
- Spring Cache annotations: `@Cacheable`, `@CacheEvict`
- Reactive types: `Mono<RiskThresholds>`, `Mono<BinLookupResult>`

**Cache Configurations:**
- **Risk Thresholds:**
  - Cache name: `"risk-thresholds"` (configured in `RedisConfig`)
  - Cache key: `#merchantId`
  - TTL: 2 hours (7200 seconds)
  - Namespace: `core:risk:{merchantId}`
- **BIN Lookup:**
  - Cache name: `"bin-lookup"` (configured in `RedisConfig`)
  - Cache key: `#bin`
  - TTL: 7 days (604800 seconds)
  - Namespace: `core:bin:{bin}`

## Scope

### Part 1: RiskService Cache

1. Open existing `RiskService.java` class
2. Add `@Cacheable` annotation to `getRiskThresholds(String merchantId)` method:
   - Set `value = "risk-thresholds"`
   - Set `key = "#merchantId"`
   - Add logging for cache miss (optional)
3. Add `@CacheEvict` annotation to `updateRiskThresholds(String merchantId, RiskThresholds data)` method:
   - Set `value = "risk-thresholds"`
   - Set `key = "#merchantId"`
   - Add logging for cache eviction (optional)

### Part 2: BinLookupService Cache

1. Open existing `BinLookupService.java` class
2. Add `@Cacheable` annotation to `lookupBin(String bin)` method:
   - Set `value = "bin-lookup"`
   - Set `key = "#bin"`
   - Add logging for cache miss (optional)
3. **Note:** No `@CacheEvict` needed for BIN lookup (semi-static data, TTL-based expiration only)

### Part 3: Verification

1. Verify cache hit/miss behavior for both services
2. Verify TTLs are correctly applied (2h for risk, 7d for BIN)
3. Verify Redis key namespaces are correct

**Implementation Notes:**
- Risk thresholds change occasionally (merchant updates fraud rules) → 2h TTL
- BIN data is semi-static (rarely changes) → 7d TTL, no manual eviction
- Use same pattern as `MerchantService` (task-004)

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: RiskService Cache**
- Method `getRiskThresholds(String merchantId)` has `@Cacheable(value = "risk-thresholds", key = "#merchantId")` annotation
- First call queries database/repository (cache miss)
- Second call with same `merchantId` returns from cache (cache hit)
- Cache hit latency < 10ms (p95)
- TTL is 2 hours (7200 seconds)
- Redis key format: `core:risk:{merchantId}` (e.g., `core:risk:m_123`)

**AC-2: RiskService Cache Eviction**
- Method `updateRiskThresholds(String merchantId, RiskThresholds data)` has `@CacheEvict(value = "risk-thresholds", key = "#merchantId")` annotation
- After update, cache entry is removed
- Next `getRiskThresholds()` call queries database (cache miss)

**AC-3: BinLookupService Cache**
- Method `lookupBin(String bin)` has `@Cacheable(value = "bin-lookup", key = "#bin")` annotation
- First call queries external BIN API or database (cache miss)
- Second call with same `bin` returns from cache (cache hit)
- Cache hit latency < 10ms (p95)
- TTL is 7 days (604800 seconds)
- Redis key format: `core:bin:{bin}` (e.g., `core:bin:411111`)

**AC-4: BIN Lookup No Manual Eviction**
- No `@CacheEvict` annotation on any BIN lookup methods
- Cache entries expire automatically after 7 days (TTL-based)
- No manual invalidation needed (semi-static data)

### Failure Cases

- Cache miss if Redis is unavailable (fallback to database/API via circuit breaker)
- Invalid `merchantId` or `bin` returns empty `Mono` (not cached)
- External BIN API timeout falls back to database (if available)

### Validation Commands

```bash
# Start application and Redis
docker-compose up -d redis
mvn spring-boot:run

# Test Risk Thresholds Cache
curl http://localhost:8080/api/v1/merchants/m_123/risk-thresholds

# Check Redis for cached entry
docker exec redis redis-cli GET "core:risk:m_123"
# Expected: JSON representation of risk thresholds

# Check TTL (should be ~7200 seconds = 2 hours)
docker exec redis redis-cli TTL "core:risk:m_123"
# Expected: ~7200

# Update risk thresholds
curl -X PUT http://localhost:8080/api/v1/merchants/m_123/risk-thresholds -d '{"high_value_threshold":2000}'

# Verify cache evicted
docker exec redis redis-cli GET "core:risk:m_123"
# Expected: (nil)

# Test BIN Lookup Cache
curl http://localhost:8080/api/v1/bin/411111

# Check Redis for cached entry
docker exec redis redis-cli GET "core:bin:411111"
# Expected: JSON representation of BIN lookup result

# Check TTL (should be ~604800 seconds = 7 days)
docker exec redis redis-cli TTL "core:bin:411111"
# Expected: ~604800

# Query same BIN again (cache hit)
curl http://localhost:8080/api/v1/bin/411111
# Expected: Faster response (< 10ms)
```

### Optional Integration Tests

If integration tests are desired:
- Test risk thresholds cache hit/miss
- Test risk thresholds cache eviction on update
- Test BIN lookup cache hit/miss
- Test BIN lookup TTL expiration (mock time or wait)
- Test fallback to database/API when Redis is unavailable

## Constraints and Negative Instructions

**DO:**
- Use Spring Cache annotations (`@Cacheable`, `@CacheEvict`)
- Use SpEL for cache keys: `#merchantId`, `#bin`
- Use cache names `"risk-thresholds"` and `"bin-lookup"` (match `RedisConfig`)
- Keep method signatures unchanged (reactive `Mono<T>`)
- Add logging for cache operations (optional but recommended)

**DO NOT:**
- Implement manual Redis operations (use annotations only)
- Add `@CacheEvict` to BIN lookup methods (TTL-based expiration only)
- Cache sensitive data (only risk rules and BIN metadata)
- Change repository/API method signatures
- Implement custom TTL logic (configured in `RedisConfig`)

**Out of Scope:**
- Transaction history cache (short TTL, low priority)
- Circuit breaker implementation - task-006
- Idempotency logic - task-007, task-008, task-009
- Integration tests - optional
