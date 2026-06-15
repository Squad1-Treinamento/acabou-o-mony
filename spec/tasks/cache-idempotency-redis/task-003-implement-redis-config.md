---
id: task-003
status: in_progress
links:
  - spec/tasks/index.md
  - spec/specs/spec-004-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-004-cache-and-idempotency-layer.md
  - spec/tasks/task-001-add-cache-maven-dependencies.md
  - spec/tasks/task-002-configure-redis-application-yml.md
---

# Implement RedisConfig Java Configuration Class

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Create:**
- `src/main/java/com/acabouomony/core/config/RedisConfig.java` (new configuration class)

**Dependencies:**
- `ReactiveRedisConnectionFactory` (from spring-data-redis-reactive)
- `ReactiveRedisTemplate` (from spring-data-redis-reactive)
- `CacheManager` (from spring-cache)
- `RedisCacheManager` (from spring-data-redis)
- `RedisCacheConfiguration` (from spring-data-redis)
- `StringRedisSerializer` (from spring-data-redis)
- `Jackson2JsonRedisSerializer` (from spring-data-redis)

**Configuration Values:**
- Cache TTLs from `application.yml`: `cache.ttl.merchant-config`, `cache.ttl.risk-thresholds`, `cache.ttl.bin-lookup`
- Injected via `@Value` annotations

## Scope

1. Create package `com.acabouomony.core.config` (if not exists)
2. Create `RedisConfig.java` class annotated with `@Configuration` and `@EnableCaching`
3. Implement `reactiveRedisTemplate()` bean:
   - Configure `StringRedisSerializer` for keys
   - Configure `Jackson2JsonRedisSerializer` for values (JSON serialization)
   - Build `RedisSerializationContext` with key/value serializers
   - Return `ReactiveRedisTemplate` instance
4. Implement `cacheManager()` bean:
   - Inject TTL values via `@Value` annotations
   - Configure default `RedisCacheConfiguration` (disable null caching, set serializers)
   - Create cache-specific configurations with custom TTLs:
     - `merchants` cache with `merchant-config` TTL
     - `risk-thresholds` cache with `risk-thresholds` TTL
     - `bin-lookup` cache with `bin-lookup` TTL
   - Return `RedisCacheManager` with default and custom configurations
5. Verify application starts and beans are registered

**Implementation Notes:**
- Use `@Bean` annotation for both methods
- Use `@Value("${cache.ttl.merchant-config}")` to inject TTL values
- Use `Duration.ofSeconds(ttl)` to convert TTL to Duration
- Disable null value caching with `.disableCachingNullValues()`
- Use `Map.of()` for cache configurations (Java 9+)

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: Configuration Class Structure**
- Class `RedisConfig` exists in package `com.acabouomony.core.config`
- Class is annotated with `@Configuration`
- Class is annotated with `@EnableCaching`

**AC-2: ReactiveRedisTemplate Bean**
- Method `reactiveRedisTemplate(ReactiveRedisConnectionFactory factory)` exists
- Method is annotated with `@Bean`
- Returns `ReactiveRedisTemplate<String, Object>`
- Uses `StringRedisSerializer` for keys
- Uses `Jackson2JsonRedisSerializer<Object>` for values
- Configures `RedisSerializationContext` with both serializers

**AC-3: CacheManager Bean**
- Method `cacheManager(ReactiveRedisConnectionFactory factory, ...)` exists
- Method is annotated with `@Bean`
- Injects TTL values via `@Value` annotations:
  - `@Value("${cache.ttl.merchant-config}") long merchantTtl`
  - `@Value("${cache.ttl.risk-thresholds}") long riskTtl`
  - `@Value("${cache.ttl.bin-lookup}") long binTtl`
- Returns `CacheManager` instance
- Default cache configuration disables null caching
- Custom cache configurations exist for:
  - `"merchants"` with `merchantTtl`
  - `"risk-thresholds"` with `riskTtl`
  - `"bin-lookup"` with `binTtl`

**AC-4: Application Startup**
- Application starts without errors
- Beans `reactiveRedisTemplate` and `cacheManager` are registered in Spring context
- No serialization errors in logs
- Redis connection is established

### Failure Cases

- Application fails to start if `@EnableCaching` is missing
- Bean creation fails if `ReactiveRedisConnectionFactory` is not available
- Serialization fails if Jackson is not in classpath
- Configuration fails if TTL values are not defined in `application.yml`

### Validation Commands

```bash
# Start application
mvn spring-boot:run

# Check Spring context for beans (in application logs)
# Expected: "Bean 'reactiveRedisTemplate' of type [ReactiveRedisTemplate] is registered"
# Expected: "Bean 'cacheManager' of type [RedisCacheManager] is registered"

# Optional: Test Redis connection manually
docker exec redis redis-cli MONITOR
# Then trigger a cache operation from application
# Expected: Should see Redis commands (GET, SET, etc.)
```

### Optional Unit Tests

If unit tests are desired:
- Test `reactiveRedisTemplate` bean creation
- Test `cacheManager` bean creation
- Test cache configurations have correct TTLs
- Mock `ReactiveRedisConnectionFactory` for isolated testing

## Constraints and Negative Instructions

**DO:**
- Use `@Configuration` and `@EnableCaching` annotations
- Use `@Bean` for both methods
- Use `@Value` to inject TTL values from `application.yml`
- Use `Jackson2JsonRedisSerializer` for JSON serialization
- Use `StringRedisSerializer` for keys (human-readable in Redis)
- Disable null value caching (`.disableCachingNullValues()`)

**DO NOT:**
- Hard-code TTL values (always inject from configuration)
- Use XML configuration (Java config only)
- Implement custom serializers (use Spring provided ones)
- Add encryption logic in this task (task-013)
- Implement service layer logic (task-004, task-005)

**Out of Scope:**
- Service layer implementation (`MerchantService`, etc.) - task-004, task-005
- Circuit breaker implementation - task-006
- Idempotency logic - task-007, task-008, task-009
- Encryption infrastructure - task-013, task-014
