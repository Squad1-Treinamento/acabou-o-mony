---
id: task-006
status: planned
links:
  - spec/tasks/index.md
  - spec/specs/spec-002-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-002-cache-and-idempotency-layer.md
  - spec/tasks/task-001-add-cache-maven-dependencies.md
  - spec/tasks/task-002-configure-redis-application-yml.md
---

# Implement Circuit Breaker for Redis Cache

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Create:**
- `src/main/java/com/acabouomony/core/config/CircuitBreakerConfig.java` (new configuration class)

**Dependencies:**
- `CircuitBreaker` (from resilience4j-circuitbreaker)
- `CircuitBreakerRegistry` (from resilience4j-circuitbreaker)
- `CircuitBreakerConfig` (from resilience4j-circuitbreaker)
- `RedisConnectionException` (from io.lettuce.core)
- `RedisCommandTimeoutException` (from io.lettuce.core)

**Configuration Values:**
- Failure threshold: `${CACHE_CIRCUIT_BREAKER_FAILURE_THRESHOLD:3}` (from `application.yml`)
- Wait duration: `${CACHE_CIRCUIT_BREAKER_WAIT_DURATION_SECONDS:30}` seconds
- Sliding window size: `${CACHE_CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE:10}`

**Circuit Breaker States:**
- **CLOSED:** Normal operation, Redis working
- **OPEN:** After 3 failures, skip Redis and fallback to database
- **HALF_OPEN:** After 30s, try 1 request to test Redis recovery

## Scope

1. Create package `com.acabouomony.core.config` (if not exists)
2. Create `CircuitBreakerConfig.java` class annotated with `@Configuration`
3. Implement `circuitBreakerRegistry()` bean:
   - Inject configuration values via `@Value` annotations
   - Create `CircuitBreakerConfig` with:
     - Failure rate threshold (default: 3)
     - Wait duration in open state (default: 30 seconds)
     - Sliding window size (default: 10)
     - Record exceptions: `RedisConnectionException`, `RedisCommandTimeoutException`
   - Return `CircuitBreakerRegistry` instance
4. Implement `redisCacheCircuitBreaker()` bean:
   - Use registry to create circuit breaker named `"redis-cache"`
   - Return `CircuitBreaker` instance
5. Add logging for circuit breaker state transitions (optional but recommended)
6. Verify circuit breaker opens after 3 Redis failures
7. Verify automatic recovery after 30 seconds

**Implementation Notes:**
- Use Resilience4j's `CircuitBreakerConfig.custom()` builder
- Use `Duration.ofSeconds()` for wait duration
- Circuit breaker should be applied at cache layer (transparent to services)
- Fallback to database should be automatic (no code changes in services)

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: Configuration Class Structure**
- Class `CircuitBreakerConfig` exists in package `com.acabouomony.core.config`
- Class is annotated with `@Configuration`

**AC-2: CircuitBreakerRegistry Bean**
- Method `circuitBreakerRegistry(...)` exists
- Method is annotated with `@Bean`
- Injects configuration values via `@Value`:
  - `@Value("${cache.circuit-breaker.failure-threshold:3}") int failureThreshold`
  - `@Value("${cache.circuit-breaker.wait-duration-seconds:30}") int waitDuration`
  - `@Value("${cache.circuit-breaker.sliding-window-size:10}") int slidingWindow`
- Creates `CircuitBreakerConfig` with:
  - `failureRateThreshold(failureThreshold)`
  - `waitDurationInOpenState(Duration.ofSeconds(waitDuration))`
  - `slidingWindowSize(slidingWindow)`
  - `recordExceptions(RedisConnectionException.class, RedisCommandTimeoutException.class)`
- Returns `CircuitBreakerRegistry.of(config)`

**AC-3: CircuitBreaker Bean**
- Method `redisCacheCircuitBreaker(CircuitBreakerRegistry registry)` exists
- Method is annotated with `@Bean`
- Returns `registry.circuitBreaker("redis-cache")`

**AC-4: Circuit Breaker Opens After Failures**
- After 3 consecutive Redis connection failures, circuit breaker state is `OPEN`
- Subsequent requests skip Redis and go directly to database
- Log message: `WARN - Redis circuit breaker OPEN, falling back to database`

**AC-5: Circuit Breaker Recovers Automatically**
- After 30 seconds in `OPEN` state, circuit breaker transitions to `HALF_OPEN`
- Allows 1 test request to Redis
- If test succeeds, transitions to `CLOSED` (normal operation)
- If test fails, stays `OPEN` for another 30 seconds

**AC-6: Fallback Behavior**
- When circuit breaker is `OPEN`, cache operations fallback to database
- Application continues functioning (no HTTP errors to client)
- Latency increases by ~50-100ms (acceptable degradation)

### Failure Cases

- Circuit breaker does not open if failures are non-consecutive
- Circuit breaker does not recover if Redis remains unavailable
- Application crashes if fallback to database is not implemented (should not happen with Spring Cache)

### Validation Commands

```bash
# Start application with Redis running
docker-compose up -d redis
mvn spring-boot:run

# Verify circuit breaker is CLOSED (normal state)
# Check application logs for: "Circuit breaker 'redis-cache' initialized in CLOSED state"

# Stop Redis to simulate failure
docker-compose stop redis

# Make 3 requests to trigger circuit breaker
curl http://localhost:8080/api/v1/merchants/m_123
curl http://localhost:8080/api/v1/merchants/m_123
curl http://localhost:8080/api/v1/merchants/m_123

# Check logs for circuit breaker state transition
# Expected: "Circuit breaker 'redis-cache' transitioned to OPEN"
# Expected: "WARN - Redis circuit breaker OPEN, falling back to database"

# Verify requests still succeed (fallback to database)
curl http://localhost:8080/api/v1/merchants/m_123
# Expected: HTTP 200 with merchant data (from database)

# Wait 30 seconds
sleep 30

# Check logs for HALF_OPEN transition
# Expected: "Circuit breaker 'redis-cache' transitioned to HALF_OPEN"

# Restart Redis
docker-compose start redis

# Make 1 request (test request in HALF_OPEN state)
curl http://localhost:8080/api/v1/merchants/m_123

# Check logs for CLOSED transition
# Expected: "Circuit breaker 'redis-cache' transitioned to CLOSED"
```

### Optional Integration Tests

If integration tests are desired (using Testcontainers):
- Test circuit breaker opens after 3 failures
- Test circuit breaker stays CLOSED if failures are non-consecutive
- Test circuit breaker transitions to HALF_OPEN after wait duration
- Test circuit breaker closes after successful test request
- Test fallback to database when circuit is OPEN

## Constraints and Negative Instructions

**DO:**
- Use Resilience4j's `CircuitBreakerConfig.custom()` builder
- Inject configuration values via `@Value` annotations
- Record only Redis-specific exceptions (`RedisConnectionException`, `RedisCommandTimeoutException`)
- Log state transitions at `WARN` level (not `ERROR`)
- Use circuit breaker name `"redis-cache"` (consistent with `application.yml`)

**DO NOT:**
- Hard-code configuration values (always inject from `application.yml`)
- Record generic exceptions (only Redis-specific ones)
- Log at `ERROR` level (degradation is expected, not an error)
- Implement custom fallback logic (Spring Cache handles it automatically)
- Change service layer code (circuit breaker is transparent)

**Out of Scope:**
- Custom fallback logic (Spring Cache provides automatic fallback)
- Metrics/observability (Prometheus, Grafana) - future work
- Idempotency logic - task-007, task-008, task-009
- Integration tests - optional
