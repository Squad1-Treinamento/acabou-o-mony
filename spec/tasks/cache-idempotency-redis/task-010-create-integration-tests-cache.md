---
id: task-010
status: planned
links:
  - spec/tasks/index.md
  - spec/specs/spec-002-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-002-cache-and-idempotency-layer.md
  - spec/tasks/task-004-implement-merchant-service-cache.md
  - spec/tasks/task-005-implement-risk-and-bin-cache.md
  - spec/tasks/task-006-implement-circuit-breaker.md
---

# Create Integration Tests for Cache Layer (Optional)

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Create:**
- `src/test/java/com/acabouomony/core/service/MerchantServiceCacheTest.java` (integration test)
- `src/test/java/com/acabouomony/core/service/RiskServiceCacheTest.java` (integration test)
- `src/test/java/com/acabouomony/core/service/BinLookupServiceCacheTest.java` (integration test)
- `src/test/java/com/acabouomony/core/config/CircuitBreakerIntegrationTest.java` (integration test)

**Dependencies:**
- `@Testcontainers` (from testcontainers-junit-jupiter)
- `GenericContainer` (from testcontainers)
- `@SpringBootTest` (from spring-boot-test)
- `MerchantService`, `RiskService`, `BinLookupService` (from main code)
- `CircuitBreaker`, `CircuitBreakerRegistry` (from resilience4j)

**Test Scenarios:**
- Cache hit: Query twice, verify database called only once
- Cache miss: Query new entity, verify database called
- Cache eviction: Update entity, verify cache cleared
- Circuit breaker: Redis failure, verify fallback to database
- TTL expiration: Verify cache expires after configured time (optional, requires time mocking)

## Scope

### Part 1: MerchantServiceCacheTest

1. Create test class with `@Testcontainers` and `@SpringBootTest` annotations
2. Add Redis container: `GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379)`
3. Configure Spring to use test Redis container (via `@DynamicPropertySource`)
4. Implement test: `testCacheHit()`
   - Query merchant twice with same ID
   - Verify repository called only once (cache hit on second call)
5. Implement test: `testCacheMiss()`
   - Query merchant with new ID
   - Verify repository called (cache miss)
6. Implement test: `testCacheEviction()`
   - Query merchant (cache miss)
   - Update merchant (cache eviction)
   - Query again (cache miss, database called again)

### Part 2: RiskServiceCacheTest

1. Create test class with same structure as `MerchantServiceCacheTest`
2. Implement test: `testRiskThresholdsCacheHit()`
3. Implement test: `testRiskThresholdsCacheMiss()`
4. Implement test: `testRiskThresholdsCacheEviction()`
5. Verify TTL is 2 hours (optional, requires time mocking)

### Part 3: BinLookupServiceCacheTest

1. Create test class with same structure
2. Implement test: `testBinLookupCacheHit()`
3. Implement test: `testBinLookupCacheMiss()`
4. Verify TTL is 7 days (optional, requires time mocking)
5. Verify no cache eviction on update (BIN data is semi-static)

### Part 4: CircuitBreakerIntegrationTest

1. Create test class with `@Testcontainers` and `@SpringBootTest`
2. Implement test: `testCircuitBreakerOpensOnRedisFailure()`
   - Stop Redis container
   - Make 3 requests (trigger circuit breaker)
   - Verify circuit breaker state is `OPEN`
   - Verify requests still succeed (fallback to database)
3. Implement test: `testCircuitBreakerRecovery()`
   - Open circuit breaker (stop Redis, make 3 requests)
   - Wait 30 seconds (or mock time)
   - Start Redis container
   - Make 1 request (test request in `HALF_OPEN` state)
   - Verify circuit breaker state is `CLOSED`

**Implementation Notes:**
- Use `@DynamicPropertySource` to configure Redis host/port from Testcontainers
- Use `@Autowired` to inject services and repositories
- Use `verify(repository, times(n))` to verify database calls (requires Mockito)
- Use `StepVerifier` for reactive assertions (Project Reactor)
- Use `@BeforeEach` to reset Redis state between tests

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: MerchantServiceCacheTest**
- Test `testCacheHit()` passes: Database called only once for two queries
- Test `testCacheMiss()` passes: Database called for new merchant
- Test `testCacheEviction()` passes: Cache cleared after update
- Code coverage > 80% for `MerchantService`

**AC-2: RiskServiceCacheTest**
- Test `testRiskThresholdsCacheHit()` passes
- Test `testRiskThresholdsCacheMiss()` passes
- Test `testRiskThresholdsCacheEviction()` passes
- Code coverage > 80% for `RiskService`

**AC-3: BinLookupServiceCacheTest**
- Test `testBinLookupCacheHit()` passes
- Test `testBinLookupCacheMiss()` passes
- Code coverage > 80% for `BinLookupService`

**AC-4: CircuitBreakerIntegrationTest**
- Test `testCircuitBreakerOpensOnRedisFailure()` passes
- Test `testCircuitBreakerRecovery()` passes (optional, requires time mocking)
- Code coverage > 80% for `CircuitBreakerConfig`

**AC-5: Test Execution**
- All tests pass with `mvn test`
- Tests run in isolation (no shared state between tests)
- Tests clean up Redis state after execution

### Failure Cases

- Tests fail if Redis container does not start
- Tests fail if cache annotations are missing
- Tests fail if circuit breaker is not configured
- Tests fail if TTLs are incorrect

### Validation Commands

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=MerchantServiceCacheTest

# Run with coverage report
mvn test jacoco:report

# Check coverage report
open target/site/jacoco/index.html
```

## Constraints and Negative Instructions

**DO:**
- Use `@Testcontainers` for Redis integration tests
- Use `@DynamicPropertySource` to configure test Redis
- Use `verify()` to assert database call counts
- Use `StepVerifier` for reactive assertions
- Clean up Redis state between tests (`@BeforeEach`)
- Mock time for TTL expiration tests (optional)

**DO NOT:**
- Use real Redis instance (always use Testcontainers)
- Share state between tests (use `@BeforeEach` to reset)
- Hard-code Redis host/port (use `@DynamicPropertySource`)
- Skip cleanup (always clean Redis state after tests)
- Test implementation details (test behavior, not internals)

**Out of Scope:**
- Unit tests (focus on integration tests only)
- Performance tests (load testing, stress testing)
- Idempotency tests - task-011
- End-to-end tests (full API tests)
