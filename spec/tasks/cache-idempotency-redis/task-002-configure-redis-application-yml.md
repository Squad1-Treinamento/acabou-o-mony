---
id: task-002
status: planned
links:
  - spec/tasks/index.md
  - spec/specs/spec-004-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-004-cache-and-idempotency-layer.md
  - spec/tasks/task-001-add-cache-maven-dependencies.md
---

# Configure Redis Connection in application.yml

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Modify:**
- `src/main/resources/application.yml` (Spring Boot configuration)

**Configuration Sections:**
- `spring.data.redis` (Redis connection settings)
- `spring.cache` (Cache behavior settings)
- `cache` (Custom cache TTL configuration)
- `resilience4j.circuitbreaker` (Circuit breaker configuration)

**Environment Variables to Reference:**
- All variables defined in `.env.example` (created in Phase 1)
- Redis connection: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, etc.
- Cache TTLs: `CACHE_TTL_MERCHANT_CONFIG`, `CACHE_TTL_RISK_THRESHOLDS`, etc.
- Circuit breaker: `CACHE_CIRCUIT_BREAKER_*` variables

## Scope

1. Open `src/main/resources/application.yml` in Core Service
2. Add `spring.data.redis` section with connection settings (host, port, password, database, timeout)
3. Add `spring.data.redis.lettuce.pool` section with connection pool settings
4. Add `spring.cache` section with Redis cache configuration (TTL, key prefix, null values)
5. Add custom `cac for each cache namespace
6. Add `resilience4j.circuitbreaker` section with Redis circuit breaker configuration
7. Verify application starts without errors
8. Verify application connects to Redis suche` section with TTL valuescessfully

**Implementation Notes:**
- Use `${VAR_NAME:default_value}` syntax for environment variables
- Default values should match `.env.example` from Phase 1
- All timeouts in milliseconds or seconds (specify unit)
- Key prefix `core:` to namespace cache keys

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: Redis Connection Configuration**
- `spring.data.redis.host` configured with `${REDIS_HOST:redis}` default
- `spring.data.redis.port` configured with `${REDIS_PORT:6379}` default
- `spring.data.redis.password` configured with `${REDIS_PASSWORD:}` (empty default)
- `spring.data.redis.database` configured with `${REDIS_DATABASE:0}` default
- `spring.data.redis.timeout` configured with `${REDIS_TIMEOUT_MS:5000}ms` default

**AC-2: Connection Pool Configuration**
- `spring.data.redis.lettuce.pool.max-active` configured with `${REDIS_POOL_MAX_ACTIVE:8}`
- `spring.data.redis.lettuce.pool.max-idle` configured with `${REDIS_POOL_MAX_IDLE:8}`
- `spring.data.redis.lettuce.pool.min-idle` configured with `${REDIS_POOL_MIN_IDLE:0}`

**AC-3: Cache Behavior Configuration**
- `spring.cache.type` set to `redis`
- `spring.cache.redis.time-to-live` configured with `${CACHE_TTL_MERCHANT_CONFIG:86400}s`
- `spring.cache.redis.cache-null-values` set to `false`
- `spring.cache.redis.use-key-prefix` set to `true`
- `spring.cache.redis.key-prefix` set to `"core:"`

**AC-4: Custom Cache TTL Configuration**
- `cache.enabled` configured with `${CACHE_ENABLED:true}`
- `cache.fallback-to-db-on-error` configured with `${CACHE_FALLBACK_TO_DB_ON_ERROR:true}`
- `cache.ttl.merchant-config` configured with `${CACHE_TTL_MERCHANT_CONFIG:86400}`
- `cache.ttl.risk-thresholds` configured with `${CACHE_TTL_RISK_THRESHOLDS:7200}`
- `cache.ttl.bin-lookup` configured with `${CACHE_TTL_BIN_LOOKUP:604800}`
- `cache.ttl.transaction-history` configured with `${CACHE_TTL_TRANSACTION_HISTORY:60}`
- `cache.ttl.idempotency-key` configured with `${CACHE_TTL_IDEMPOTENCY_KEY:86400}`

**AC-5: Circuit Breaker Configuration**
- `resilience4j.circuitbreaker.instances.redis-cache.failure-rate-threshold` configured with `${CACHE_CIRCUIT_BREAKER_FAILURE_THRESHOLD:3}`
- `resilience4j.circuitbreaker.instances.redis-cache.wait-duration-in-open-state` configured with `${CACHE_CIRCUIT_BREAKER_WAIT_DURATION_SECONDS:30}s`
- `resilience4j.circuitbreaker.instances.redis-cache.sliding-window-size` configured with `${CACHE_CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE:10}`
- `resilience4j.circuitbreaker.instances.redis-cache.permitted-number-of-calls-in-half-open-state` set to `1`
- `resilience4j.circuitbreaker.instances.redis-cache.automatic-transition-from-open-to-half-open-enabled` set to `true`
- `resilience4j.circuitbreaker.instances.redis-cache.record-exceptions` includes `RedisConnectionException` and `RedisCommandTimeoutException`

**AC-6: Application Startup**
- Application starts without errors when Redis is running
- Application logs show successful Redis connection
- No configuration validation errors in logs

### Failure Cases

- Application fails to start if `REDIS_HOST` is invalid
- Application fails to start if Redis is not reachable (without circuit breaker fallback)
- Configuration validation errors if TTL values are negative

### Validation Commands

```bash
# Start Redis (if not running)
docker-compose up -d redis

# Start application
mvn spring-boot:run

# Check logs for Redis connection
# Expected: "Lettuce ConnectionFactory initialized" or similar

# Verify Redis connection from application
docker exec redis redis-cli CLIENT LIST
# Expected: Should show connection from Core Service
```

## Constraints and Negative Instructions

**DO:**
- Use environment variable syntax `${VAR:default}` for all configurable values
- Use explicit time units (`ms`, `s`) for timeout/duration values
- Use YAML syntax correctly (proper indentation, no tabs)
- Match default values from `.env.example` created in Phase 1

**DO NOT:**
- Hard-code connection values (always use environment variables)
- Use properties file format (use YAML only)
- Add database/JPA configuration in this task (separate concern)
- Implement Java configuration classes yet (task-003)

**Out of Scope:**
- Java configuration classes (`RedisConfig.java`) - task-003
- Service layer implementation - task-004, task-005
- Circuit breaker Java implementation - task-006
- Integration tests - optional
