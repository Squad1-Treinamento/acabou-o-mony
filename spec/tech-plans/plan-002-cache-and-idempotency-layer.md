---
id: plan-002
status: active
links:
  - spec/tech-plans/index.md
  - spec/specs/spec-002-cache-and-idempotency-layer.md
  - spec/adrs/adr-002-cache-and-idempotency-layer.md
  - spec/user-stories/us-001-transaction-processing.md
  - spec/user-stories/us-002-scalability.md
  - ARCHITECTURE.md
---

# Cache e Idempotency Layer — Tech Plan

## Architecture Overview and Data Flow

A camada de cache e idempotência usa Redis standalone compartilhado entre 3DS Engine e Core Payment Service. O padrão Cache-Aside (Lazy Loading) é aplicado com fallback automático para PostgreSQL em caso de falha do Redis.

```
┌─────────────────────────────────────────────────────────────┐
│                    Client (Merchant API)                     │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTPS
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Nginx Reverse Proxy (TLS Termination)          │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP/2
                            ▼
┌─────────────────────────────────────────────────────────────┐
│           Core Payment Processing Service (Future)           │
│         (Spring Boot 3.x, WebFlux, Java 21, Netty)          │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Cache Layer (Spring Cache Annotations)              │  │
│  │  - @Cacheable (read)                                 │  │
│  │  - @CachePut (write)                                 │  │
│  │  - @CacheEvict (invalidate)                          │  │
│  └────────────┬─────────────────────────┬─────────────────┘  │
│               │                         │                    │
│               │ Cache Hit               │ Cache Miss         │
│               ▼                         ▼                    │
│  ┌─────────────────────┐   ┌──────────────────────────┐    │
│  │ Return from Cache   │   │ Query PostgreSQL         │    │
│  │ (< 10ms)            │   │ Save to Cache (TTL)      │    │
│  └─────────────────────┘   │ Return to Client         │    │
│                             └──────────────────────────┘    │
└───────────────┬─────────────────────────┬───────────────────┘
                │                         │
                │ Redis Protocol          │ R2DBC (Non-blocking)
                ▼                         ▼
┌──────────────────────────┐   ┌──────────────────────────┐
│   Redis Standalone       │   │   PostgreSQL 16          │
│   (512MB, allkeys-lru)   │   │   (ACID Ledger)          │
│                          │   │                          │
│  Namespaces:             │   │  Tables:                 │
│  - core:merchant:*       │   │  - merchants             │
│  - core:risk:*           │   │  - transactions          │
│  - core:bin:*            │   │  - risk_rules            │
│  - core:idempotency:*    │   │  - bin_lookup            │
│  - 3ds:session:*         │   │                          │
│  - 3ds:auth:*            │   │                          │
└──────────────────────────┘   └──────────────────────────┘
```

### Data Flow: Cache-Aside Pattern

**1. Read Operation (Cache Hit):**
```
Client → Core Service → Check Redis (core:merchant:m_123)
                     → Cache Hit → Return from Redis (< 10ms)
```

**2. Read Operation (Cache Miss):**
```
Client → Core Service → Check Redis (core:merchant:m_456)
                     → Cache Miss → Query PostgreSQL
                     → Save to Redis (TTL 24h)
                     → Return to Client
```

**3. Write Operation (Cache Invalidation):**
```
Client → Core Service → Update PostgreSQL
                     → @CacheEvict (core:merchant:m_123)
                     → Delete from Redis
                     → Return to Client
```

**4. Idempotency Check:**
```
Client → Core Service → Check Redis (core:idempotency:req_abc123)
                     → Key Exists → Return cached result (X-Idempotent-Replayed: true)
                     → Key Not Exists → Process transaction
                                     → Save result to Redis (TTL 24h)
                                     → Return to Client
```

**5. Circuit Breaker (Redis Failure):**
```
Client → Core Service → Check Redis → Connection Timeout (3 failures)
                     → Circuit Breaker OPEN
                     → Fallback to PostgreSQL
                     → Log WARN
                     → Return to Client (no error)
```

---

## Stack and Dependencies

### Infrastructure

- **Redis:** Version 7 Alpine (`redis:7-alpine`)
- **Docker Compose:** Version 3.8+
- **Volume:** Named volume `redis-data` for persistence
- **Network:** Shared Docker network `acabou-o-mony-network`

### Redis Configuration

```redis
# Persistence
appendonly yes
appendfsync everysec

# Memory Management
maxmemory 512mb
maxmemory-policy allkeys-lru

# Performance
tcp-backlog 511
timeout 0
tcp-keepalive 300

# Security (production)
# requirepass <strong_password>
```

### Application Dependencies (Core Service - Future)

**Maven Dependencies:**
```xml
<!-- Spring Cache Abstraction -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Redis Reactive -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>

<!-- Circuit Breaker -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-reactor</artifactId>
    <version>2.1.0</version>
</dependency>

<!-- Encryption (for future use) -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.77</version>
</dependency>
```

---

## Design Patterns and Code Conventions

### Patterns

**1. Cache-Aside (Lazy Loading):**
- Application checks cache first
- On miss, loads from database and populates cache
- On write, invalidates cache entry

**2. Circuit Breaker:**
- Protects application from cascading failures
- Automatically opens after threshold failures
- Automatically recovers when Redis is healthy

**3. Idempotency Key:**
- Unique identifier for each mutation request
- Prevents duplicate processing
- Industry standard (Stripe, Adyen, PayPal)

**4. Namespace Isolation:**
- Separate key prefixes per service (`core:*`, `3ds:*`)
- Prevents key collision
- Enables selective cache clearing

### Code Conventions (Future Implementation)

**Service Layer with Cache Annotations:**
```java
@Service
public class MerchantService {
    
    @Cacheable(value = "merchants", key = "#merchantId")
    public Mono<Merchant> findById(String merchantId) {
        return merchantRepository.findById(merchantId);
    }
    
    @CacheEvict(value = "merchants", key = "#merchantId")
    public Mono<Merchant> update(String merchantId, Merchant data) {
        return merchantRepository.save(data);
    }
}
```

**Manual Cache for Complex Logic:**
```java
@Service
public class IdempotencyService {
    
    private final ReactiveRedisTemplate<String, TransactionResult> redisTemplate;
    
    public Mono<TransactionResult> checkIdempotency(String idempotencyKey) {
        String key = "core:idempotency:" + idempotencyKey;
        return redisTemplate.opsForValue()
            .get(key)
            .switchIfEmpty(Mono.empty());
    }
    
    public Mono<Void> saveIdempotency(String idempotencyKey, TransactionResult result) {
        String key = "core:idempotency:" + idempotencyKey;
        Duration ttl = Duration.ofHours(24);
        return redisTemplate.opsForValue()
            .set(key, result, ttl)
            .then();
    }
}
```

**Circuit Breaker Configuration:**
```java
@Configuration
public class CacheConfig {
    
    @Bean
    public CircuitBreaker redisCacheCircuitBreaker(
            @Value("${cache.circuit-breaker.failure-threshold}") int failureThreshold,
            @Value("${cache.circuit-breaker.wait-duration-seconds}") int waitDuration) {
        
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureThreshold)
            .waitDurationInOpenState(Duration.ofSeconds(waitDuration))
            .slidingWindowSize(10)
            .recordExceptions(RedisConnectionException.class)
            .build();
        
        return CircuitBreaker.of("redis-cache", config);
    }
}
```

**AES Encryptor (Prepared, Not Used):**
```java
@Component
public class AESEncryptor {
    
    private final SecretKey secretKey;
    
    public AESEncryptor(@Value("${cache.encryption.key}") String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(decodedKey, "AES");
    }
    
    public String encrypt(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12]; // GCM standard IV size
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    public String decrypt(String encryptedBase64) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);
        byte[] iv = Arrays.copyOfRange(combined, 0, 12);
        byte[] ciphertext = Arrays.copyOfRange(combined, 12, combined.length);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
}
```

---

## Persistence and Data Modeling

### Redis Key Conventions

| Key Pattern | Type | TTL | Example | Purpose |
|---|---|---|---|---|
| `core:merchant:{merchant_id}` | STRING (JSON) | 24h | `core:merchant:m_123` | Merchant configuration cache |
| `core:risk:{merchant_id}` | STRING (JSON) | 2h | `core:risk:m_123` | Risk thresholds cache |
| `core:bin:{bin}` | STRING (JSON) | 7d | `core:bin:411111` | BIN lookup results cache |
| `core:tx_history:{merchant_id}:{card_token}` | STRING (JSON) | 1m | `core:tx_history:m_123:tok_xyz` | Transaction history metadata |
| `core:idempotency:{idempotency_key}` | STRING (JSON) | 24h | `core:idempotency:req_abc123` | Idempotency key cache |
| `3ds:session:{challenge_id}` | HASH | 10m | `3ds:session:ch_001` | 3DS challenge session (existing) |
| `3ds:auth:{challenge_id}` | STRING | 24h | `3ds:auth:ch_001` | 3DS auth result (existing) |

### JSON Serialization Examples

**Merchant Configuration:**
```json
{
  "merchant_id": "m_123",
  "name": "Ana's Clothing Store",
  "email": "ana@store.com",
  "country": "BR",
  "currency": "BRL",
  "max_transaction_amount": 10000.00,
  "risk_profile": "medium",
  "created_at": "2026-01-01T00:00:00Z"
}
```

**Risk Thresholds:**
```json
{
  "merchant_id": "m_123",
  "high_value_threshold": 1500.00,
  "risk_score_threshold": 75,
  "max_daily_transactions": 1000,
  "require_3ds_above": 500.00,
  "updated_at": "2026-06-01T12:00:00Z"
}
```

**BIN Lookup Result:**
```json
{
  "bin": "411111",
  "issuer": "Visa Test Bank",
  "country": "US",
  "card_type": "credit",
  "acs_url": "https://acs.testbank.com/3ds-auth",
  "cached_at": "2026-06-01T12:00:00Z"
}
```

**Idempotency Key:**
```json
{
  "idempotency_key": "req_abc123",
  "transaction_id": "tx_001",
  "status": "approved",
  "amount": 1500.00,
  "currency": "BRL",
  "merchant_id": "m_123",
  "created_at": "2026-06-01T12:00:00Z",
  "response": {
    "status": "approved",
    "transaction_id": "tx_001",
    "authorization_code": "AUTH123"
  }
}
```

---

## Non-Functional Requirements and Security

### Performance

- **Cache Hit Latency:** < 10ms (p95)
- **Cache Miss Latency:** < 100ms (p95) - includes DB query + Redis write
- **Idempotency Check:** < 5ms (p95)
- **Circuit Breaker Overhead:** < 1ms (negligible)

### Scalability

- **Redis Memory:** 512MB supports ~50,000 cached entries (avg 10KB each)
- **Throughput:** Redis standalone supports 100,000+ ops/sec
- **Connection Pool:** 8 max active connections (Lettuce default)

### Security

**Data Protection:**
- ❌ **Never cache:** PAN, CVV, passwords, API keys, JWT secrets
- ✅ **Safe to cache:** Merchant config, risk rules, BIN lookup, transaction IDs
- 🔐 **Encryption ready:** AES-256-GCM infrastructure prepared (not used in MVP)

**Network Security:**
- Redis accessible only within Docker network (not exposed publicly)
- TLS for Redis connections in production (via `rediss://` protocol)
- Password authentication required in production (`requirepass`)

**Audit and Compliance:**
- Log all cache operations (hit/miss/evict) with structured JSON
- Log circuit breaker state transitions (CLOSED → OPEN → HALF_OPEN)
- Do not log sensitive data in cache keys or values

### Reliability

**Circuit Breaker Parameters:**
- Failure threshold: 3 consecutive failures
- Wait duration: 30 seconds
- Sliding window: 10 requests
- Automatic recovery: Yes (HALF_OPEN → CLOSED)

**Fallback Strategy:**
- Redis unavailable → Query PostgreSQL
- Log level: WARN (not ERROR)
- Client impact: None (transparent fallback)
- SLA impact: +50-100ms latency (acceptable)

**Data Consistency:**
- Cache invalidation on write operations (`@CacheEvict`)
- TTL as safety net for stale data
- Eventual consistency acceptable (not financial data in cache)

---

## Implementation Roadmap

### Phase 1: Infrastructure Setup (Current)

**Deliverables:**
- ✅ `docker-compose.yml` with Redis standalone
- ✅ `.env.example` with all configuration variables
- ✅ `docs/redis-setup.md` with setup and validation commands
- ✅ ADR-002, Spec-002, Plan-002 (this document)

**Tasks:**
1. Create `docker-compose.yml` with Redis service
2. Create `.env.example` with Redis, cache, and circuit breaker variables
3. Create `docs/redis-setup.md` with setup instructions
4. Update `spec/adrs/index.md`, `spec/specs/index.md`, `spec/tech-plans/index.md`
5. Validate Redis container starts and health check passes

**Validation:**
```bash
# Start Redis
docker-compose up -d redis

# Check health
docker exec redis redis-cli ping
# Expected: PONG

# Check persistence
docker exec redis redis-cli CONFIG GET appendonly
# Expected: appendonly yes

# Check memory limit
docker exec redis redis-cli CONFIG GET maxmemory
# Expected: maxmemory 536870912 (512MB in bytes)
```

---

### Phase 2: Core Service Cache Implementation (Future)

**Deliverables:**
- Spring Boot Core Service with cache annotations
- Merchant Configuration cache (TTL 24h)
- Risk Thresholds cache (TTL 2h)
- BIN Lookup cache (TTL 7 days)
- Circuit breaker integration
- Unit and integration tests

**Tasks:**
1. Add Spring Cache and Redis dependencies to Core Service `pom.xml`
2. Configure `application.yml` with Redis connection and cache settings
3. Implement `MerchantService` with `@Cacheable` and `@CacheEvict`
4. Implement `RiskService` with cache annotations
5. Implement `BinLookupService` with cache annotations
6. Configure Resilience4j circuit breaker
7. Write integration tests with Testcontainers Redis
8. Document cache usage in service layer

**Validation:**
- Cache hit rate > 70% for merchant config
- Cache miss latency < 100ms (p95)
- Circuit breaker opens after 3 Redis failures
- Fallback to PostgreSQL works without errors

---

### Phase 3: Idempotency Implementation (Future)

**Deliverables:**
- Idempotency interceptor for mutation endpoints
- `IdempotencyService` with Redis integration
- Header validation (`Idempotency-Key: req_{uuid}`)
- Error caching (failed transactions)
- Integration tests

**Tasks:**
1. Create `IdempotencyInterceptor` to check header
2. Implement `IdempotencyService` with Redis operations
3. Add interceptor to `POST /api/v1/payments`, `/refunds`, `/capture`
4. Implement error caching (cache declined transactions)
5. Add header `X-Idempotent-Replayed` to responses
6. Write integration tests for duplicate requests
7. Document idempotency behavior in API docs

**Validation:**
- Duplicate requests return cached result (same HTTP status and body)
- `X-Idempotent-Replayed: true` header present on replayed requests
- Failed transactions are also cached (prevent retry storms)
- Idempotency key format validation rejects invalid keys

---

### Phase 4: Encryption Infrastructure (Future)

**Deliverables:**
- `AESEncryptor` class with encrypt/decrypt methods
- Configuration validation for encryption key
- Unit tests for encryption/decryption
- Documentation for enabling encryption

**Tasks:**
1. Create `AESEncryptor` class with AES-256-GCM
2. Add `CACHE_ENCRYPTION_KEY` validation on startup
3. Write unit tests for encrypt/decrypt roundtrip
4. Document how to generate encryption key (openssl)
5. Document when to enable encryption (future use case)

**Validation:**
- Encrypt/decrypt roundtrip returns original plaintext
- Invalid encryption key fails application startup
- Flag `CACHE_ENCRYPT_SENSITIVE_DATA=false` by default

---

## Examples and Use Cases

### Example 1: Merchant Configuration Cache

**Scenario:** Merchant "m_123" makes 1000 transactions per day. Without cache, each transaction queries PostgreSQL for merchant config.

**Without Cache:**
- 1000 transactions × 50ms DB query = 50,000ms (50 seconds) total latency
- High load on PostgreSQL

**With Cache (24h TTL):**
- First transaction: 50ms (cache miss + DB query + Redis write)
- Next 999 transactions: 5ms each (cache hit)
- Total latency: 50ms + (999 × 5ms) = 5,045ms (5 seconds)
- **90% latency reduction**

---

### Example 2: Idempotency Prevents Double Charge

**Scenario:** Customer double-clicks "Pay" button during Live Commerce stream.

**Without Idempotency:**
```
Request 1: POST /api/v1/payments (Idempotency-Key: req_abc123)
→ Process transaction → Charge $150 → Return success

Request 2: POST /api/v1/payments (Idempotency-Key: req_abc123)
→ Process transaction → Charge $150 AGAIN → Return success

Result: Customer charged $300 (double charge) ❌
```

**With Idempotency:**
```
Request 1: POST /api/v1/payments (Idempotency-Key: req_abc123)
→ Check Redis (key not found)
→ Process transaction → Charge $150
→ Save result to Redis (TTL 24h)
→ Return success (X-Idempotent-Replayed: false)

Request 2: POST /api/v1/payments (Idempotency-Key: req_abc123)
→ Check Redis (key found)
→ Return cached result (no processing)
→ Return success (X-Idempotent-Replayed: true)

Result: Customer charged $150 (single charge) ✅
```

---

### Example 3: Circuit Breaker Prevents Cascading Failure

**Scenario:** Redis crashes during peak traffic (1000 req/sec).

**Without Circuit Breaker:**
```
Every request tries to connect to Redis
→ Connection timeout (5 seconds each)
→ 1000 req/sec × 5s = 5000 blocked threads
→ Application crashes (thread pool exhausted) ❌
```

**With Circuit Breaker:**
```
Request 1-3: Try Redis → Timeout (3 failures)
→ Circuit breaker opens (state: OPEN)

Request 4-1000: Skip Redis → Query PostgreSQL directly
→ Latency: +50ms (acceptable)
→ Application continues functioning ✅

After 30 seconds: Circuit breaker tries reconnect (state: HALF_OPEN)
→ If Redis is back: Close circuit (state: CLOSED)
→ If Redis still down: Stay open for another 30s
```

---

## Testing Strategy

### Unit Tests

**Cache Service Tests:**
- Test cache hit returns data from Redis
- Test cache miss queries database and populates cache
- Test cache eviction removes key from Redis
- Test TTL expiration (simulate time passing)

**Idempotency Service Tests:**
- Test first request processes transaction
- Test duplicate request returns cached result
- Test error caching (failed transactions)
- Test idempotency key format validation

**AES Encryptor Tests:**
- Test encrypt/decrypt roundtrip
- Test invalid key throws exception
- Test empty plaintext handling

### Integration Tests (Testcontainers)

**Redis Integration:**
```java
@Testcontainers
@SpringBootTest
class CacheIntegrationTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @Test
    void testMerchantCacheHit() {
        // Given: Merchant in cache
        merchantService.findById("m_123").block();
        
        // When: Query again
        StepVerifier.create(merchantService.findById("m_123"))
            .expectNextMatches(m -> m.getId().equals("m_123"))
            .verifyComplete();
        
        // Then: No database query (verify with mock)
        verify(merchantRepository, times(1)).findById("m_123");
    }
}
```

**Circuit Breaker Integration:**
```java
@Test
void testCircuitBreakerOpensOnRedisFailure() {
    // Given: Redis is down
    redis.stop();
    
    // When: Make 3 requests
    for (int i = 0; i < 3; i++) {
        merchantService.findById("m_123").block();
    }
    
    // Then: Circuit breaker should be OPEN
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("redis-cache");
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
}
```

---

## Monitoring and Observability (Out of Scope for MVP)

**Note:** Observability is out of scope for this phase, but documented here for future reference.

**Metrics to Track (Future):**
- Cache hit rate (%)
- Cache miss rate (%)
- Cache eviction rate (keys/sec)
- Redis memory usage (MB)
- Circuit breaker state (CLOSED/OPEN/HALF_OPEN)
- Fallback invocations (count)
- Idempotency replays (count)

**Logs to Emit:**
```json
{
  "event": "cache.hit",
  "key": "core:merchant:m_123",
  "ttl_remaining_seconds": 3600,
  "timestamp": "2026-06-01T12:00:00Z"
}

{
  "event": "cache.miss",
  "key": "core:merchant:m_456",
  "fallback": "database",
  "latency_ms": 85,
  "timestamp": "2026-06-01T12:00:01Z"
}

{
  "event": "circuit_breaker.state_transition",
  "from": "CLOSED",
  "to": "OPEN",
  "reason": "redis_connection_timeout",
  "timestamp": "2026-06-01T12:00:02Z"
}
```

---

## References

- [ADR-002: Cache e Idempotency Layer](../adrs/adr-002-cache-and-idempotency-layer.md)
- [Spec-002: Cache e Idempotency Layer](../specs/spec-002-cache-and-idempotency-layer.md)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Redis Persistence (AOF)](https://redis.io/docs/management/persistence/)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)
- [Stripe API Idempotency](https://stripe.com/docs/api/idempotent_requests)
