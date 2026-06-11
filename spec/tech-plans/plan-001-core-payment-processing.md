---

id: plan-001
status: active
links:

* spec/tech-plans/index.md
* spec/specs/spec-001-core-payment-processing.md
* spec/tasks/index.md

---

# Core Payment Processing Tech Plan

This document defines the implementation approach for the Core Payment Processing Service.

The plan focuses on:

* implementation sequencing,
* dependency ordering,
* consistency guarantees,
* and risk reduction during development.

This plan follows the behavioral requirements defined in:

* `spec-001-core-payment-processing.md`
* `ARCHITECTURE.md`
* `CONTEXT.md`

---

# Definition

The Core Payment Processing implementation will be developed incrementally in layers.

The implementation sequence prioritizes:

1. deterministic transaction consistency,
2. safe persistence behavior,
3. idempotent payment execution,
4. external integration reliability,
5. asynchronous processing safety,
6. and operational validation.

The plan intentionally implements correctness-critical infrastructure before performance optimization or auxiliary functionality.

---

# What It Is Used For

This tech plan is used to:

* define implementation order,
* reduce architectural risk,
* identify critical dependencies,
* validate distributed systems behavior early,
* and guide future task decomposition.

The plan also ensures that:

* payment consistency is implemented before scaling,
* failure recovery paths exist before async execution,
* and external integrations are isolated behind stable interfaces.

---

# Implementation Strategy

The implementation follows a layered approach.

Each phase establishes stable behavior before introducing additional complexity.

---

# Phase 1 — Project Foundation & Persistence Layer

The first phase establishes the foundational platform and authoritative persistence model.

Implementation includes:

* Spring Boot 3.x initialization,
* PostgreSQL integration,
* Redis integration,
* Docker Compose infrastructure,
* JPA entity modeling,
* database migrations,
* optimistic locking support,
* and transaction state modeling.

### Database Schema

This phase establishes complete database schema as foundation:

#### transactions Table

```sql
CREATE TABLE transactions (
  id UUID PRIMARY KEY,
  merchant_id UUID NOT NULL,
  idempotency_key UUID NOT NULL,
  amount BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL,
  status VARCHAR(20) NOT NULL,
  payload_hash VARCHAR(64) NOT NULL,
  masked_card VARCHAR(20),
  card_token_id VARCHAR(100),
  acquirer_reference VARCHAR(255),
  version INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  UNIQUE(merchant_id, idempotency_key),
  INDEX idx_merchant_id (merchant_id),
  INDEX idx_status (status),
  INDEX idx_created_at (created_at)
);
```

#### audit_logs Table

```sql
CREATE TABLE audit_logs (
  id UUID PRIMARY KEY,
  transaction_id UUID NOT NULL REFERENCES transactions(id),
  old_status VARCHAR(20),
  new_status VARCHAR(20) NOT NULL,
  actor VARCHAR(50) NOT NULL,
  checksum VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  INDEX idx_transaction_id (transaction_id),
  INDEX idx_created_at (created_at)
);
```

Audit log entries are written in SAME database transaction as transaction update.
If transaction rolls back, audit entry also rolls back.

**Checksum computation (Phase 1):**
```
checksum = SHA256(transaction_id + old_status + new_status + actor)
Checksum prevents tampering; stored at write time, never modified.
```

#### outbox_events Table

```sql
CREATE TABLE outbox_events (
  id UUID PRIMARY KEY,
  event_type VARCHAR(50) NOT NULL,
  aggregate_id UUID NOT NULL,
  payload TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  retry_count INT DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  delivered_at TIMESTAMP,
  INDEX idx_status (status),
  INDEX idx_created_at (created_at)
);
```

### JPA Entity Modeling

Phase 1 implements JPA entities with:

```java
@Entity
@Table(name = "transactions")
public class Transaction {
  @Id
  private UUID id;
  
  @Version  // Optimistic locking
  private Integer version;
  
  private UUID merchantId;
  private UUID idempotencyKey;
  private Long amount;
  private String currency;
  private String status;
  private String payloadHash;
  private String maskedCard;
  private String cardTokenId;
  private String acquirerReference;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
```

The `@Version` annotation enables optimistic locking.
Version incremented on every state transition.

### Payment Lifecycle State Machine

Phase 1 implements transaction state constants:

```
CREATED
VALIDATED
CHALLENGE_PENDING
AUTHENTICATED
PROCESSING
UNKNOWN
COMPLETED
DECLINED
FAILED
```

State machine MUST be implemented with validation:
- Only allowed transitions permitted (per spec Section 1.2)
- Invalid transitions rejected with exception
- State transition logic isolated in service layer

### Required Validations

Phase 1 establishes database constraints:
- `UNIQUE(merchant_id, idempotency_key)` prevents duplicates
- Foreign key constraints for data integrity
- NOT NULL constraints for critical fields
- Check constraints for valid status values

The payment lifecycle state machine MUST be fully implemented before external integrations.

---

# Phase 2 — Authentication & Request Validation

The second phase implements request security and deterministic validation behavior.

Implementation includes:

* merchant authentication,
* API key validation,
* request schema validation,
* payload hashing,
* card validation,
* and idempotency validation.

This phase also introduces:

* Redis idempotency coordination,
* payload integrity validation,
* duplicate request handling,
* and DB-backed idempotency guarantees.

External payment processing MUST NOT exist before idempotency protections are stable.

---

# Phase 3 — Core Payment Processing

The third phase implements the synchronous payment execution flow.

Implementation includes:

* payment orchestration service,
* Mercado Pago integration,
* outbound HTTP client configuration,
* timeout handling,
* transaction persistence flow,
* and deterministic state transitions.

### What Phase 3 Implements

This phase MUST implement:

* Synchronous request handling with validation
* Merchant authentication verification
* Request payload validation
* Mercado Pago API integration (isolate behind dedicated layer)
* Successful payment transitions: `PROCESSING -> COMPLETED/DECLINED/FAILED`
* Timeout detection: `PROCESSING -> UNKNOWN` (on Mercado Pago timeout)
* Atomic transaction persistence with optimistic locking
* Audit log generation for state transitions
* Acquirer reference persistence (payment ID from Mercado Pago)

### What Phase 3 Does NOT Implement

This phase does NOT include:

* **Reconciliation of UNKNOWN states** (deferred to Phase 4)
* **Background workers** or scheduling (deferred to Phase 4)
* **Webhook dispatch** or outbox processing (deferred to Phase 5)
* **3DS challenge handling** (deferred to Phase 6)
* **Risk evaluation** (deferred to Phase 6)

### Testing Expectations

Phase 3 testing creates UNKNOWN states but does not verify resolution:

- Test: Timeout during Mercado Pago call → transaction moves to UNKNOWN ✓
- Test: Duplicate request with same idempotency key → cached response ✓
- Test: Transaction persists with correct version ✓
- Test: Audit log created for state transition ✓

Do NOT test: UNKNOWN -> COMPLETED resolution (Phase 4 responsibility)

### Mercado Pago Integration

The Mercado Pago integration MUST remain isolated behind a dedicated integration layer:

```java
interface PaymentAcquirerClient {
  PaymentResult submitPayment(PaymentRequest req);
  PaymentStatus queryPaymentStatus(String acquirerReference);
}

@Component
class MercadoPagoClient implements PaymentAcquirerClient {
  // All Mercado Pago API details encapsulated here
}

@Service
class PaymentOrchestrationService {
  private PaymentAcquirerClient acquirer;  // Injected interface
  
  public void processPayment(Transaction transaction) {
    PaymentResult result = acquirer.submitPayment(transaction);
    // Handle result
  }
}
```

This isolation allows Phase 4 reconciliation to use same interface.

---

# Phase 4a — Reconciliation Query Workers

The first part of Phase 4 implements UNKNOWN state recovery without webhook notifications.

Implementation includes:

* Reconciliation worker thread pool
* UNKNOWN transaction polling and status queries
* Mercado Pago status query integration
* Deterministic state transitions (UNKNOWN → COMPLETED/DECLINED/FAILED)
* Reconciliation audit logging
* Retry logic with exponential backoff
* Rate limiting per merchant (max 2 concurrent queries)

### Reconciliation Polling Strategy

Reconciliation worker runs continuously:

```java
@Scheduled(fixedRate = 100)  // Poll every 100ms
public void reconcileUnknownPayments() {
  List<Transaction> unknownTransactions = 
    transactionRepository.findByStatusAndCreatedAtBefore(
      "UNKNOWN", 
      now().minusMinutes(1)  // Only reconcile after 1 minute
    );
  
  for (Transaction tx : unknownTransactions) {
    attemptReconciliation(tx);
  }
}

private void attemptReconciliation(Transaction tx) {
  for (int attempt = 0; attempt < 3; attempt++) {
    try {
      PaymentStatus status = acquirer.queryPaymentStatus(tx.acquirerReference);
      tx.setStatus(status);
      tx.setVersion(tx.getVersion() + 1);  // Increment for state transition
      transactionRepository.save(tx);  // Optimistic lock checked
      auditLogRepository.save(
        new AuditLog(tx.id, "UNKNOWN", status, "reconciliation")
      );
      return;  // Success
    } catch (OptimisticLockException e) {
      // Stale version; will retry on next poll cycle
      return;
    } catch (Exception e) {
      if (attempt == 2) {
        // Final attempt failed
        transitionToFailed(tx, "Reconciliation max retries exceeded");
        return;
      }
      Thread.sleep(1000 * (attempt + 1));  // Backoff: 1s, 2s
    }
  }
}
```

### Reconciliation Concurrency Control

Per-merchant reconciliation queue:

```java
private Map<UUID, Queue<Transaction>> reconciliationQueues = new ConcurrentHashMap<>();

private void attemptReconciliation(Transaction tx) {
  UUID merchantId = tx.merchantId;
  
  // Check if at capacity
  if (concurrentCount.getOrDefault(merchantId, 0) >= 2) {
    Queue<Transaction> queue = reconciliationQueues.get(merchantId);
    if (queue.size() >= 10) {
      // Queue overflow; drop and alert
      alertOperator("Reconciliation queue overflow for merchant " + merchantId);
      return;
    }
    queue.offer(tx);  // Queue it
    return;
  }
  
  // Process immediately
  concurrentCount.merge(merchantId, 1, Integer::sum);
  try {
    PaymentStatus status = acquirer.queryPaymentStatus(tx.acquirerReference);
    // ... update transaction
  } finally {
    concurrentCount.put(merchantId, concurrentCount.get(merchantId) - 1);
  }
}
```

### Testing Phase 4a

Test specifications:
- UNKNOWN → COMPLETED when Mercado Pago confirms success ✓
- UNKNOWN → DECLINED when Mercado Pago confirms rejection ✓
- UNKNOWN → FAILED after 3 query attempts with no response ✓
- Audit log created for each reconciliation attempt ✓
- Optimistic lock retry on version conflict ✓
- Per-merchant concurrency limits enforced ✓

---

# Phase 4b — Webhook Infrastructure

The second part of Phase 4 implements asynchronous webhook dispatch infrastructure.

Implementation includes:

* Transactional outbox event persistence (already in Phase 1, now integrate)
* Virtual Thread executor for async dispatch
* Webhook polling worker
* HTTPS client for merchant endpoints
* Webhook signature generation (HMAC-SHA256)
* Retry logic with exponential backoff
* Retry attempt tracking
* Failure persistence and alerting

### Webhook Dispatch Worker

Polling-based outbox processing:

```java
@Scheduled(fixedRate = 100)  // Poll every 100ms
public void dispatchWebhooks() {
  List<OutboxEvent> pendingEvents = 
    outboxRepository.findByStatusOrderByCreatedAtAsc(
      "PENDING", 
      PageRequest.of(0, 100)  // Batch of 100
    );
  
  for (OutboxEvent event : pendingEvents) {
    executor.submit(() -> dispatchWebhook(event));
  }
}

private void dispatchWebhook(OutboxEvent event) {
  try {
    String signature = generateSignature(event.payload);
    HttpResponse response = httpClient.post(
      merchantWebhookUrl,
      headers: {
        "X-Webhook-ID": event.id,
        "X-Idempotency-Key": event.idempotencyKey,
        "X-Retry-Count": event.retryCount,
        "X-Timestamp": ISO8601(now),
        "X-Signature": signature
      },
      body: event.payload
    );
    
    if (response.status == 200) {
      event.setStatus("DELIVERED");
      event.setDeliveredAt(now);
      outboxRepository.save(event);
    } else {
      retryWebhook(event);
    }
  } catch (Exception e) {
    retryWebhook(event);
  }
}

private void retryWebhook(OutboxEvent event) {
  if (event.retryCount >= 5) {
    event.setStatus("FAILED");
    outboxRepository.save(event);
    alertOperator("Webhook delivery failed for event " + event.id);
  } else {
    int delayMs = (1000 * (1 << event.retryCount));  // Exponential: 1s, 2s, 4s, 8s, 16s
    scheduledExecutor.schedule(
      () -> dispatchWebhook(event),
      delayMs,
      TimeUnit.MILLISECONDS
    );
    event.setRetryCount(event.retryCount + 1);
    outboxRepository.save(event);
  }
}
```

### Virtual Thread Executor

Spring Boot configuration:

```java
@Configuration
public class AsyncConfig {
  
  @Bean
  public ExecutorService webhookExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
```

Virtual Threads provide:
- Light-weight concurrency (millions possible)
- Non-blocking I/O for HTTP calls
- Natural integration with Spring and JPA (no need for reactive/R2DBC refactor)
- Automatic thread management

### Testing Phase 4b

Test specifications:
- Outbox event created when transaction completes ✓
- Webhook dispatched for COMPLETED event ✓
- Webhook retry on failed delivery (5 attempts) ✓
- Exponential backoff: 1s, 2s, 4s, 8s, 16s ✓
- Signature verification with HMAC-SHA256 ✓
- Webhook marked DELIVERED after 200 OK ✓
- Webhook marked FAILED after 5 retries ✓

---

# Phase 4c — Reconciliation with Webhook Notifications

The third part of Phase 4 integrates webhook notifications into reconciliation flow.

Implementation includes:

* Reconciliation updates transaction state
* Outbox event creation within reconciliation transaction
* Webhook notification for reconciliation completion
* Audit logging for reconciliation with webhook dispatch
* End-to-end reconciliation → webhook → merchant flow

### Reconciliation Webhook Integration

```java
private void attemptReconciliation(Transaction tx) {
  for (int attempt = 0; attempt < 3; attempt++) {
    try {
      PaymentStatus status = acquirer.queryPaymentStatus(tx.acquirerReference);
      
      // Update transaction
      tx.setStatus(status);
      tx.setVersion(tx.getVersion() + 1);
      transactionRepository.save(tx);
      
      // Create audit entry (same transaction)
      auditLogRepository.save(
        new AuditLog(tx.id, "UNKNOWN", status, "reconciliation")
      );
      
      // Create outbox event for webhook (same transaction)
      outboxRepository.save(
        new OutboxEvent(
          eventType: "payment.reconciled",
          aggregateId: tx.id,
          payload: { transaction: tx, resolvedFrom: "UNKNOWN" }
        )
      );
      
      return;  // All persisted atomically
    } catch (OptimisticLockException e) {
      return;  // Will retry on next poll
    } catch (Exception e) {
      // ... retry logic
    }
  }
}
```

All three operations (transaction update, audit log, outbox event) persist atomically.

### End-to-End Flow

```
1. Reconciliation worker polls UNKNOWN transactions
2. Queries Mercado Pago for status
3. Updates transaction, audit, and outbox in single DB transaction
4. Commits successfully
5. Webhook worker polls outbox
6. Discovers new event
7. Dispatches webhook to merchant
8. Merchant receives: "Your payment was reconciled as COMPLETED"
```

### Testing Phase 4c

Test specifications:
- UNKNOWN transaction reconciled to COMPLETED ✓
- Webhook created and dispatched after reconciliation ✓
- Audit trail shows reconciliation actor ✓
- All three updates (transaction, audit, outbox) atomic ✓
- End-to-end: reconciliation → webhook delivery ✓

---

# Phase 5 — Async Processing Optimization & Health Checks

The fifth phase introduces optimization of asynchronous processing and operational infrastructure.

Implementation includes:

* Health check endpoint (`/actuator/health`)
* Liveness and readiness probes
* Request timeout handling configuration
* Client disconnect recovery
* Async processing optimization for virtual threads
* Health check monitoring integration

### Health Check Endpoint

```java
@Component
public class PaymentServiceHealthIndicator extends AbstractHealthIndicator {
  
  @Override
  protected void doHealthCheck(Health.Builder builder) {
    // Check PostgreSQL
    try {
      testPostgresConnection(1000);  // 1s timeout
      builder.withDetail("postgres", "UP");
    } catch (Exception e) {
      builder.withDetail("postgres", "DOWN").withException(e);
      builder.down();
      return;
    }
    
    // Check Redis
    try {
      testRedisConnection(1000);  // 1s timeout
      builder.withDetail("redis", "UP");
    } catch (Exception e) {
      builder.withDetail("redis", "DOWN").withException(e);
      builder.down();
      return;
    }
    
    builder.up();
  }
}
```

### Readiness & Liveness Probes

Kubernetes configuration:

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 30
  timeoutSeconds: 5
  failureThreshold: 3

livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 60
  timeoutSeconds: 5
  failureThreshold: 2
```

### Async Processing with Virtual Threads

Configuration validated in this phase:

```properties
# Application configuration
spring.threads.virtual.enabled=true
tomcat.threads.max=1000  # Virtual threads, essentially unlimited
tomcat.accept-count=200
tomcat.max-connections=10000

# Connection pool
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=5000ms
```

### Request Timeout Handling

Configured at multiple layers:

**Nginx layer:**
```nginx
client_body_timeout 60s;
send_timeout 60s;
```

**Spring Boot layer:**
```properties
server.servlet.session.timeout=3600s
spring.mvc.async.request-timeout=60000  # ms
```

**Mercado Pago client:**
```java
Duration connectTimeout = Duration.ofMillis(500);
Duration readTimeout = Duration.ofMillis(2000);
```

### Testing Phase 5

Test specifications:
- Health endpoint returns UP when dependencies available ✓
- Health endpoint returns DOWN when PostgreSQL unavailable ✓
- Health endpoint returns DOWN when Redis unavailable ✓
- Client disconnect doesn't abort payment processing ✓
- Timeout transitions to UNKNOWN (not FAILED) ✓
- Async dispatch continues despite client disconnect ✓
- Virtual threads scale to 1000+ concurrent requests ✓

---

# Phase 6 — Risk Evaluation & 3DS Authentication Flow

The sixth phase introduces risk-based authentication flows and 3D Secure handling.

Implementation includes:

* lightweight fraud evaluation logic,
* 3DS challenge orchestration,
* JWT challenge generation and validation,
* challenge replay protection,
* timeout cleanup workers,
* and authentication state transitions.

This phase also validates:

* asynchronous authentication handling,
* challenge expiration behavior (10 minutes),
* and replay protection enforcement.

3DS flows MUST remain isolated from the standard low-latency payment path.

### Risk Evaluation Rules

Lightweight risk assessment:

```java
@Service
public class RiskEvaluationService {
  
  public boolean isHighRisk(PaymentRequest req) {
    // Simplistic rule-based evaluation
    if (req.amount > 1000) return true;  // High value
    if (isFirstTimeCard(req.cardToken)) return true;  // New card
    if (isUnusualGeography(req.ipAddress)) return true;  // Geographic anomaly
    if (exceedsVelocity(req.merchant)) return true;  // Too many attempts
    return false;
  }
}
```

If high-risk flagged:
- Force VALIDATED → CHALLENGE_PENDING transition
- Bypass not allowed (mandatory 3DS)

If low-risk:
- Skip CHALLENGE_PENDING
- Proceed directly to PROCESSING
- Preserves <1s SLA guarantee

### 3DS Challenge Generation

Endpoint for challenge initiation:

```java
@PostMapping("/api/v1/payments/{transactionId}/3ds-challenge")
public ResponseEntity<?> initiate3dsChallenge(@PathVariable UUID transactionId) {
  Transaction tx = transactionRepository.findById(transactionId)
    .orElseThrow(() -> new NotFoundException());
  
  if (!tx.getStatus().equals("CHALLENGE_PENDING")) {
    throw new IllegalStateException("Transaction not in CHALLENGE_PENDING");
  }
  
  // Generate JWT
  Map<String, Object> claims = Map.of(
    "transaction_id", tx.id,
    "merchant_id", tx.merchantId,
    "amount", tx.amount,
    "nonce", generateNonce(tx),
    "exp", System.currentTimeMillis() + 600000  // 10 minutes
  );
  
  String jwt = jwtProvider.generateToken(tx.merchantId, claims);
  
  return ResponseEntity.ok(Map.of(
    "challenge_url", "https://mercadopago.com/3ds?token=" + jwt,
    "jwt_token", jwt,
    "expires_at", Instant.now().plusSeconds(600)
  ));
}

private String generateNonce(Transaction tx) {
  return SHA256(tx.id + tx.merchantId).toHexString();
}
```

### Challenge Completion

Endpoint for challenge result:

```java
@PostMapping("/api/v1/payments/{transactionId}/3ds-complete")
public ResponseEntity<?> complete3dsChallenge(
    @PathVariable UUID transactionId,
    @RequestBody ChallengeCompleteRequest req) {
  
  Transaction tx = transactionRepository.findById(transactionId)
    .orElseThrow();
  
  // Validate JWT
  Map<String, Object> claims = jwtProvider.validateToken(req.jwtToken, tx.merchantId);
  
  // Validate nonce (replay protection)
  String nonce = (String) claims.get("nonce");
  if (nonceCache.exists(nonce)) {
    throw new SecurityException("JWT nonce already used (replay attempt)");
  }
  nonceCache.put(nonce, "used", Duration.ofHours(24));
  
  // Transition to AUTHENTICATED
  tx.setStatus("AUTHENTICATED");
  tx.setVersion(tx.getVersion() + 1);
  transactionRepository.save(tx);
  
  auditLogRepository.save(
    new AuditLog(tx.id, "CHALLENGE_PENDING", "AUTHENTICATED", "3ds_complete")
  );
  
  return ResponseEntity.ok(Map.of("status", "AUTHENTICATED"));
}
```

### Challenge Timeout Cleanup

Background job monitors challenge expiration:

```java
@Scheduled(fixedRate = 30000)  // Every 30 seconds
public void cleanupExpiredChallenges() {
  List<Transaction> expired = transactionRepository
    .findByStatusAndCreatedAtBefore(
      "CHALLENGE_PENDING",
      Instant.now().minus(Duration.ofMinutes(10))
    );
  
  for (Transaction tx : expired) {
    tx.setStatus("FAILED");
    tx.setVersion(tx.getVersion() + 1);
    transactionRepository.save(tx);
    
    auditLogRepository.save(
      new AuditLog(tx.id, "CHALLENGE_PENDING", "FAILED", "challenge_timeout")
    );
    
    outboxRepository.save(
      new OutboxEvent(
        eventType: "payment.challenge_expired",
        aggregateId: tx.id,
        payload: { reason: "10-minute timeout" }
      )
    );
  }
}
```

This prevents CHALLENGE_PENDING from lingering indefinitely.

### Challenge Failure Handling

If customer fails challenge (wrong OTP, network error):

```java
@PostMapping("/api/v1/payments/{transactionId}/3ds-failed")
public ResponseEntity<?> fail3dsChallenge(@PathVariable UUID transactionId) {
  Transaction tx = transactionRepository.findById(transactionId).orElseThrow();
  
  tx.setStatus("DECLINED");
  tx.setVersion(tx.getVersion() + 1);
  transactionRepository.save(tx);
  
  auditLogRepository.save(
    new AuditLog(tx.id, "CHALLENGE_PENDING", "DECLINED", "3ds_failed")
  );
  
  outboxRepository.save(
    new OutboxEvent(
      eventType: "payment.authentication_failed",
      aggregateId: tx.id,
      payload: { reason: "Customer failed 3DS verification" }
    )
  );
  
  return ResponseEntity.ok(Map.of("status", "DECLINED"));
}
```

### 3DS Integration with Payment Flow

Complete state machine validation:

```
REQUEST -> VALIDATED (low-risk)
       \-> CHALLENGE_PENDING (high-risk)
             \-> AUTHENTICATED (3DS complete)
             \-> DECLINED (3DS failed)
             \-> FAILED (3DS timeout 10min)
             
AUTHENTICATED / VALIDATED -> PROCESSING -> COMPLETED/DECLINED/FAILED
```

### Testing Phase 6

Test specifications:
- Low-risk transaction bypasses 3DS (VALIDATED → PROCESSING) ✓
- High-risk transaction enters 3DS (VALIDATED → CHALLENGE_PENDING) ✓
- Challenge JWT generated with 10-min expiration ✓
- Challenge JWT signed correctly (HS256) ✓
- Nonce validates successfully ✓
- Nonce replay rejected (cached) ✓
- Challenge timeout after 10 min (CHALLENGE_PENDING → FAILED) ✓
- Challenge failure handled (CHALLENGE_PENDING → DECLINED) ✓
- Challenge completion transitions to AUTHENTICATED ✓
- Webhook sent for challenge expiration ✓

---

# Phase 7 — Security Hardening & PCI-DSS Compliance

The seventh phase implements payment-security protections.

Implementation includes:

* tokenization integration with acquirer,
* PAN masking enforcement throughout system,
* AES-256-GCM encryption validation,
* structured audit logging,
* webhook signing (HMAC-SHA256),
* secure secret handling (no plaintext keys in code),
* input sanitization,
* and SQL injection prevention.

This phase also validates:

* sensitive data isolation (PAN never in logs),
* log sanitization (no card data leakage),
* secure persistence behavior,
* and compliance with spec Section 6.2 (PAN Handling Rules).

### PAN Tokenization

Tokenization completes BEFORE payment processing:

```java
@Service
public class TokenizationService {
  
  public TokenizationResult tokenize(String pan, String expiryDate, String cvv) {
    // Validate card basics
    if (!isValidLuhn(pan)) {
      throw new InvalidCardException("Invalid card number");
    }
    
    // Encrypt PAN
    String encryptedPan = encrypt(pan);
    
    // Generate token (surrogate key)
    String token = UUID.randomUUID().toString();
    
    // Store encrypted PAN in secure vault (never in main database)
    tokenVault.store(token, encryptedPan);
    
    // Return only token and masked card
    String masked = maskCard(pan);
    
    return new TokenizationResult(token, masked);
  }
  
  private String maskCard(String pan) {
    // Format: 411111XXXXXX1111
    return pan.substring(0, 6) 
         + "X".repeat(pan.length() - 10) 
         + pan.substring(pan.length() - 4);
  }
}
```

### Payment Request with Token

Client submits token (NOT raw PAN):

```json
{
  "amount": 10000,
  "currency": "BRL",
  "idempotency_key": "abc123",
  "payment_method": {
    "token_id": "tok_xxx",
    "masked_card": "411111XXXXXX1111"
  }
}
```

Payment endpoint receives `token_id` and `masked_card` (never raw PAN).

### PAN Handling Enforcement

```java
@Service
public class PaymentOrchestrationService {
  
  public void processPayment(Transaction tx) {
    // Never access raw PAN
    // Assert.isNull(tx.rawPan);
    
    // Use tokenized value
    String tokenId = tx.cardTokenId;
    
    // Forward token to Mercado Pago
    PaymentResult result = acquirer.submitPayment(tokenId, tx.amount);
    
    // Store masked card for customer reference
    tx.setMaskedCard("411111XXXXXX1111");
    
    // Never persist raw PAN
    // Assert.isNull(tx.rawPan);
    
    transactionRepository.save(tx);
  }
}
```

### Audit Log Sanitization

Audit logs MUST NOT contain sensitive data:

```java
@Service
public class AuditLoggingService {
  
  public void logTransition(Transaction tx, String oldStatus, String newStatus) {
    // Safe to log status
    // UNSAFE to log: card data, PAN, merchant API key
    
    AuditLog entry = new AuditLog();
    entry.setTransactionId(tx.id);
    entry.setOldStatus(oldStatus);
    entry.setNewStatus(newStatus);
    entry.setActor("system");
    
    // Compute checksum
    String checksum = SHA256(
      tx.id.toString() + oldStatus + newStatus + "system"
    );
    entry.setChecksum(checksum);
    
    auditLogRepository.save(entry);
    
    // Example log line (safe):
    // 2024-05-29 12:34:56 AUDIT: Transaction abc123 PROCESSING -> COMPLETED
    
    // Example log line (UNSAFE - never do this):
    // 2024-05-29 12:34:56 AUDIT: Transaction abc123 masked_card=411111XXXXXX1111 PROCESSING -> COMPLETED
  }
}
```

### Webhook Signing

Merchant webhooks signed with HMAC-SHA256:

```java
private String generateSignature(String payload, String merchantSecret) {
  Mac hmac = Mac.getInstance("HmacSHA256");
  hmac.init(new SecretKeySpec(
    merchantSecret.getBytes(StandardCharsets.UTF_8),
    "HmacSHA256"
  ));
  byte[] digest = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
  return Base64.getEncoder().encodeToString(digest);
}
```

Webhook headers include signature:

```
X-Signature: HMAC-SHA256(payload, merchant_secret)
```

Merchant MUST verify signature before processing webhook.

### Secure Configuration Management

API keys, secrets, and credentials NEVER in code:

```properties
# ✓ CORRECT: Load from environment
spring.datasource.password=${DB_PASSWORD}
security.merchant.secret=${MERCHANT_SECRET}
security.encryption.key=${ENCRYPTION_KEY}

# ✗ INCORRECT: Never do this
spring.datasource.password=postgres123
security.encryption.key=hardcoded_key_12345
```

Use externalized configuration:
- Environment variables
- Docker secrets
- Kubernetes secrets
- HashiCorp Vault (for production)

### SQL Injection Prevention

JPA repositories with parameterized queries:

```java
// ✓ CORRECT: JPA prevents injection
Transaction tx = transactionRepository.findByIdempotencyKey(key);

// ✗ INCORRECT: String concatenation (vulnerable)
Query q = em.createQuery("SELECT t FROM Transaction t WHERE id = '" + id + "'");
```

### Input Sanitization

Request validation before processing:

```java
@PostMapping("/api/v1/payments")
public ResponseEntity<?> payment(@Valid @RequestBody PaymentRequest req) {
  // Spring validation ensures:
  // - amount is positive integer
  // - currency is valid ISO code
  // - no null values for required fields
  // - no oversized strings (prevent DoS)
  
  validator.validate(req);  // Throws on validation failure
  
  // Now safe to use req values
}

@Data
public class PaymentRequest {
  @NotNull
  @Min(1)
  private Long amount;
  
  @NotNull
  @Size(min=3, max=3)
  private String currency;
  
  @NotNull
  @NotEmpty
  private String idempotencyKey;
}
```

### Merchant API Key Security

API keys hashed and never logged:

```java
@Service
public class MerchantAuthService {
  
  public Merchant authenticate(String apiKey) {
    // Never log apiKey
    // Log only: merchant_id, auth_result
    logger.info("Authentication attempt for merchant");
    
    // Hash comparison (timing-safe)
    Merchant merchant = merchantRepository.findByApiKeyHash(
      sha256(apiKey)
    );
    
    if (merchant == null) {
      // Timing-safe comparison: always hash even if not found
      logger.warn("Authentication failed");
      return null;
    }
    
    return merchant;
  }
}
```

### Testing Phase 7

Test specifications:
- Raw PAN never persists in database ✓
- PAN never appears in logs ✓
- PAN never appears in webhook payloads ✓
- Tokenization returns token (not PAN) ✓
- Card masking format correct (411111XXXXXX1111) ✓
- Audit checksums computed and persisted ✓
- Webhook signatures validated (HMAC-SHA256) ✓
- API keys hashed in database ✓
- SQL injection attempts rejected ✓
- Oversized input rejected (input sanitization) ✓

---

# Phase 8 — Infrastructure & Operational Validation

The eighth phase validates operational behavior under concurrent load.

Implementation includes:

* Nginx reverse proxy configuration,
* Docker container orchestration,
* Prometheus metrics exposure,
* Grafana dashboards,
* and Locust load testing.

This phase validates:

* latency targets,
* connection pool behavior,
* Redis availability handling,
* Virtual Thread scalability,
* and reconciliation throughput.

Load testing MUST include duplicate-request and timeout simulation scenarios.

---

# Dependency Rules

The implementation sequence MUST respect the following dependencies:

| Phase | Dependency                    | Required Before            |
|-------|-------------------------------|----------------------------|
| 1     | Persistence schema            | Payment processing (Phase 3)|
| 2     | Idempotency enforcement       | Mercado Pago integration (Phase 3) |
| 3     | Merchant authentication       | Payment execution (Phase 3)|
| 3     | Payment processing            | 3DS implementation (Phase 6)|
| 4a    | UNKNOWN state queries         | UNKNOWN resolution (4b/4c) |
| 4b    | Transactional outbox          | Async webhook delivery     |
| 4b    | Virtual thread executor       | Webhook dispatch           |
| 4c    | Webhook infrastructure        | Notification integration   |
| 5     | Health checks                 | Kubernetes deployment      |
| 6     | Risk evaluation               | 3DS flow                   |
| 7     | Tokenization service          | Payment processing (retrospective safety) |
| 8     | All phases complete           | Load testing & verification |

### Critical Sequencing Rules

1. **Phase 1 foundations MUST be complete** before Phase 2-3 begin
   - Database schema immutable once Phase 2+ run (migration complexity)
   - JPA entities must match final schema

2. **Phase 3 MUST create UNKNOWN states** but Phase 4 resolves them
   - Phase 3 testing incomplete until Phase 4 verified
   - Cannot declare Phase 3 "done" until end-to-end tested with Phase 4

3. **Phase 4a (queries) MUST precede 4b/4c** (notifications)
   - Reconciliation worker must query before notifying
   - Webhook infrastructure separate from reconciliation logic

4. **Phase 4b (webhook infrastructure) MUST precede 4c** (notification integration)
   - Cannot integrate notifications without dispatch workers
   - Phase 4c reuses Phase 4b components

5. **Phase 5 optimizations** do NOT block other phases
   - Health checks can be added incrementally
   - Timeout tuning can occur parallel to other development

6. **Phase 6 (3DS) independent** but builds on Phase 1-3
   - Risk evaluation doesn't require earlier phases
   - But 3DS state transitions require complete Phase 3 state machine

7. **Phase 7 (security) must validate Phase 1-3 not already leaking data**
   - Retrospective review for compliance
   - May require minor fixes to earlier phases

---

# Risk Management Strategy

The implementation prioritizes consistency before concurrency optimization.

Primary implementation risks include:

* duplicate charges (CRITICAL),
* timeout ambiguity (CRITICAL),
* concurrent retries (HIGH),
* Redis coordination failure (HIGH),
* partial transaction persistence (HIGH),
* optimistic lock failures (HIGH),
* version conflict cascades (HIGH),
* UNKNOWN state starvation (MEDIUM),
* and partial failure recovery (MEDIUM).

### Risk Mitigation Strategy

Risk mitigation focuses on:

* **DB-enforced consistency** (UNIQUE constraint prevents duplicate inserts)
* **Deterministic state transitions** (explicit state machine with validation)
* **Reconciliation flows** (UNKNOWN states resolved to terminal state within 5 minutes)
* **Optimistic locking** (version conflicts detected, retried, or escalated)
* **Transactional persistence** (outbox guarantees webhook delivery)
* **Exception handling** (version conflicts don't silently fail)
* **Monitoring & alerting** (operators notified of anomalies)

### Optimistic Lock Conflict Mitigation

Risk: Version conflicts during concurrent state transitions (webhook + reconciliation)

Occurrence: Both threads attempt UNKNOWN → COMPLETED simultaneously

Mitigation:

```java
// All state transitions use retry loop
for (int attempt = 0; attempt < 3; attempt++) {
  try {
    Transaction current = repo.findById(id);
    updateState(current);  // Version incremented
    repo.save(current);    // Version checked before save
    return;  // Success
  } catch (OptimisticLockException e) {
    if (attempt == 2) {
      // Give up after 3 attempts
      logger.error("OptimisticLockException after 3 attempts");
      monitoringService.recordOptimisticLockFailure();
      throw e;  // Operator intervention required
    }
    Thread.sleep(100 * (attempt + 1)); // 100ms, 200ms backoff
  }
}
```

Operators MUST monitor: `optimistic_lock_failures_total` metric

Alert: If failures/minute > 1, declare incident (indicates concurrent bug)

### UNKNOWN State Starvation Prevention

Risk: UNKNOWN transaction never reconciled (stuck indefinitely)

Mitigation:

1. Reconciliation scheduled immediately when UNKNOWN created
2. Maximum 3 retry attempts with exponential backoff (1s, 2s total)
3. After 3 failures: transition to FAILED, alert operator
4. Background job scans for UNKNOWN > 5 minutes, alerts operator
5. Manual operator intervention after 24 hours (force transition)

### Redis Failure Handling

Risk: Redis down during idempotency check (duplicate charge possibility)

Mitigation:

1. Redis is fast-path cache only (NOT authoritative)
2. Database UNIQUE constraint is authoritative
3. If Redis unavailable: fallback to slow-path (DB lookup)
4. Constraint violation caught and handled (duplicate returns cached response)
5. Log alert: "Redis unavailable; using slow-path idempotency"

### Webhook Delivery Failure Handling

Risk: Webhook never delivered to merchant (order not processed)

Mitigation:

1. Transactional outbox (outbox event persisted with transaction)
2. Retry up to 5 times with exponential backoff (1s, 2s, 4s, 8s, 16s)
3. After 5 retries: mark FAILED, alert operator
4. Operator can manually trigger retry or investigate endpoint
5. Merchant receives X-Webhook-ID for deduplication

---

# Example

The implementation sequence for a payment request follows:

1. Validate merchant authentication ✓ Phase 2
2. Validate request payload ✓ Phase 2
3. Acquire idempotency protection (Redis / DB) ✓ Phase 2
4. Persist transaction initialization ✓ Phase 1 (schema)
5. Execute Mercado Pago request ✓ Phase 3
6. Persist deterministic transaction state ✓ Phase 3
7. Persist audit entry ✓ Phase 1 (schema), Phase 3 (implementation)
8. Persist outbox event ✓ Phase 1 (schema), Phase 3 (implementation)
9. Dispatch asynchronous notifications ✓ Phase 4b/4c

If timeout ambiguity occurs:

* the transaction transitions to UNKNOWN ✓ Phase 3
* reconciliation processing is scheduled ✓ Phase 4a
* retry protection remains active ✓ Phase 2
* client receives 202 Accepted (uncertain status) ✓ Phase 3

If reconciliation resolves UNKNOWN:

* Mercado Pago queried for final status ✓ Phase 4a
* Transaction updated to COMPLETED/DECLINED/FAILED ✓ Phase 4a
* Audit entry created with "reconciliation" actor ✓ Phase 4a
* Outbox event created for merchant notification ✓ Phase 4a/4c
* Webhook dispatched asynchronously ✓ Phase 4b/4c
* Merchant receives final payment status ✓ Phase 4c

---

# Impact on the Project

This tech plan:

* reduces implementation rework,
* improves architectural consistency,
* minimizes distributed systems risk,
* clarifies implementation sequencing,
* and establishes safe incremental delivery.

The resulting implementation path remains:

* maintainable,
* production-inspired,
* testable,
* and appropriate for an academic fintech platform.
