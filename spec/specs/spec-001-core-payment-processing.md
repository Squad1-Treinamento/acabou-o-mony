---

id: spec-001
status: active
links:

* spec/specs/index.md
* spec/user-stories/us-001-transaction-processing.md
* spec/user-stories/us-002-scalability.md
* spec/user-stories/us-003-transaction-security.md
* spec/user-stories/us-004-live-conversational-integration.md
* spec/tech-plans/index.md

---

# Core Payment Processing Specification

This document defines the behavioral rules, transaction lifecycle, consistency guarantees, and validation expectations for the Core Payment Processing Service.

The specification translates the business and architectural requirements into deterministic processing behavior suitable for implementation and verification.

This spec must remain aligned with:

* `ARCHITECTURE.md`
* `CONTEXT.md`

The specification focuses on:

* payment correctness,
* distributed systems safety,
* idempotent transaction handling,
* low-latency payment execution,
* and reliable payment reconciliation.

---

# Definition

The Core Payment Processing Service is the central transactional engine of the Acabou o Mony platform.

It is responsible for:

* processing credit and debit card payments,
* integrating with Mercado Pago,
* validating and authenticating requests,
* enforcing idempotency,
* orchestrating 3DS flows,
* persisting transaction ledger state,
* dispatching webhooks,
* and reconciling uncertain payment outcomes.

The service operates within the architectural constraints defined in `ARCHITECTURE.md`, including:

* Java 21,
* Spring Boot 3.x,
* Virtual Threads,
* PostgreSQL,
* Redis,
* Docker,
* and Mercado Pago integration.

---

# What It Is Used For

This specification is used to:

* define expected payment behavior,
* establish valid transaction lifecycle rules,
* define retry and timeout handling,
* guarantee deterministic idempotency behavior,
* align distributed systems consistency expectations,
* define persistence guarantees,
* and support testable acceptance criteria.

The spec also establishes behavioral expectations for:

* live commerce,
* conversational commerce,
* low-latency checkout flows,
* and concurrent payment execution.

---

# Payment Lifecycle Rules

The payment lifecycle follows a deterministic state machine.

Allowed transaction states:

```text
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

---

## State Definitions

| State             | Description                                                   |
| ----------------- | ------------------------------------------------------------- |
| CREATED           | Request initialized and idempotency coordination started      |
| VALIDATED         | Request payload and merchant authentication validated         |
| CHALLENGE_PENDING | Awaiting 3DS challenge completion                             |
| AUTHENTICATED     | 3DS authentication completed successfully                     |
| PROCESSING        | Payment dispatched to Mercado Pago                            |
| UNKNOWN           | Payment outcome uncertain due to timeout or network ambiguity |
| COMPLETED         | Payment approved                                              |
| DECLINED          | Payment rejected by acquirer                                  |
| FAILED            | Internal validation or processing failure                     |

---

## Allowed State Transitions

```text
CREATED -> VALIDATED
VALIDATED -> CHALLENGE_PENDING
VALIDATED -> PROCESSING
CHALLENGE_PENDING -> AUTHENTICATED
AUTHENTICATED -> PROCESSING
PROCESSING -> COMPLETED
PROCESSING -> DECLINED
PROCESSING -> UNKNOWN
PROCESSING -> FAILED
UNKNOWN -> COMPLETED
UNKNOWN -> DECLINED
UNKNOWN -> FAILED
```

Invalid transitions MUST be rejected.

---

# Idempotency Rules

Every payment request MUST include:

```http
Idempotency-Key: UUIDv4
```

Idempotency enforcement uses a **two-tier coordination model**:

* **Redis:** Fast-path coordination and response caching (NOT authoritative)
* **PostgreSQL:** Authoritative consistency boundary (ACID enforcement)

---

## Idempotency Implementation Model

Redis provides **FAST-PATH coordination only**. It is NOT authoritative for payment consistency.

### Fast-Path (Redis Cache Hit)

When client submits `Idempotency-Key`:

1. Check Redis for existing response (asynchronous in-memory read, <50ms)
2. If HIT and transaction status is NOT PROCESSING: **Return cached response immediately**
3. If HIT and transaction status is PROCESSING: **Return 202 Accepted** (wait for completion)
4. If MISS or Redis unavailable: Proceed to Slow-Path

**Fast-Path prevents duplicate execution** and satisfies SLA (<1s latency).

### Slow-Path (Redis Miss or Unavailable)

When Redis cache misses or Redis is unavailable:

1. Attempt database INSERT with UNIQUE constraint: `UNIQUE(merchant_id, idempotency_key)`
2. **On INSERT success:** Execute payment processing, persist result, update Redis cache
3. **On constraint violation:** Query database for existing transaction
4. **If transaction IN_PROGRESS:** Return 409 Conflict (timeout; retry later)
5. **If transaction terminal (COMPLETED/DECLINED/FAILED):** Return cached response (same as fast-path)
6. **If transaction UNKNOWN:** Return 202 Accepted (payment outcome uncertain; poll status)

Slow-Path ensures **database-enforced idempotency** even when Redis fails.

### Database Enforcement

The `transactions` table MUST enforce:

```sql
UNIQUE (merchant_id, idempotency_key)
```

This prevents duplicate transaction creation during:

* concurrent retries,
* Redis failures,
* container restarts,
* or race conditions.

---

## Duplicate Request Rules

| Scenario                      | Behavior                          | HTTP Status |
| ----------------------------- | --------------------------------- | ----------- |
| Same key + same payload       | Cached response returned          | 200 OK      |
| Same key + different payload  | Payload mismatch rejection         | 400 Bad     |
| Existing IN_PROGRESS request  | Return conflict (retry later)      | 409 Conflict|
| Existing UNKNOWN transaction  | Return uncertain (poll for status) | 202 Accepted|

The platform MUST persist a payload hash (SHA-256) to validate request integrity across retries.

### Payload Hash Collision Detection

When `Idempotency-Key` matches existing transaction:

**Case 1: payload_hash matches**
- Identical retry detected
- Return cached response with status and transaction_id

**Case 2: payload_hash differs**
- Different request with same idempotency key
- Return 400 Bad Request with error: `"Idempotency key mismatch: payload differs"`
- Transaction remains unchanged
- Client MUST submit with NEW `Idempotency-Key` for different payment

**Payload Definition (for hash computation):**
```
amount, currency, payment_method, card_token_id, customer_id
Payload hash = SHA256(JSON canonical form with sorted keys)
```

### Idempotency Cache TTL

Redis cache entries expire after **24 hours**:

```text
If client retries after 24 hours:
  - Idempotency cache expired
  - New request creates NEW transaction (different transaction_id)
  - Client must use NEW Idempotency-Key for new payment
```

This prevents indefinite cache growth while maintaining reasonable idempotency window.

### Redis Failure Handling

If Redis is unavailable during fast-path check:

* **Accept gracefully:** Proceed to slow-path (database consistency is backup)
* **Do NOT fail fast:** Customer experience should not degrade
* **Log alert:** `"Redis unavailable; using slow-path idempotency"`
* **Database UNIQUE constraint enforces consistency** even without Redis

---

## Idempotency Key Isolation

Idempotency keys are scoped to merchant:

```text
Merchant A: merchant_id=111, Idempotency-Key=abc123 -> Transaction X
Merchant B: merchant_id=222, Idempotency-Key=abc123 -> Transaction Y (independent)
```

Different merchants can safely use identical idempotency keys without collision.
Merchants cannot interfere with each other's idempotency protection.

---

# Timeout & Uncertain Payment Rules

Timeouts MUST NOT automatically become failed payments.

A timeout only indicates that the final payment state is unknown.

---

## Unknown State Handling

If Mercado Pago:

* times out,
* disconnects,
* or returns an ambiguous response,

the transaction MUST transition to:

```text
PROCESSING -> UNKNOWN
```

The platform MUST:

* preserve idempotency protection,
* persist reconciliation metadata,
* prevent unsafe duplicate retries,
* and schedule reconciliation processing.

---

# Reconciliation Rules

The platform MUST contain a reconciliation mechanism responsible for resolving payments in `UNKNOWN` state.

The reconciliation process MUST:

* query Mercado Pago for final payment status,
* update the final transaction state,
* generate audit entries,
* and trigger webhook notifications.

The reconciliation mechanism may be implemented using scheduled workers or asynchronous background execution compatible with the architectural model defined in `ARCHITECTURE.md`.

---

## Reconciliation Behavior & Termination

UNKNOWN transactions MUST transition to a terminal state (COMPLETED, DECLINED, or FAILED) within **5 minutes** of initial timeout.

### Reconciliation Lifecycle

1. **Trigger:** Transaction transitions to UNKNOWN state; reconciliation is immediately scheduled
2. **Attempt:** Reconciliation worker queries Mercado Pago using `acquirer_reference` (NOT idempotency key)
3. **Success:** Final state found, transaction transitions to COMPLETED/DECLINED, audit entry created, webhook notification dispatched
4. **Not Found:** After all retries exhausted, transition to FAILED (assume worst case: payment did not process)
5. **Failure:** Mercado Pago unavailable, retried with exponential backoff

### Reconciliation Retry Rules

Reconciliation MUST respect **maximum 3 retry attempts** with exponential backoff:

```text
Attempt 1: Immediate
Attempt 2: After 1 second delay
Attempt 3: After 2 seconds delay
Total: 3 seconds maximum retry window
```

If all 3 attempts fail:

* Transition transaction to FAILED state
* Log operator alert: `"Reconciliation exhausted for payment {id}, merchant {merchant_id}"`
* Persist audit entry: `actor="system", message="Reconciliation max retries exceeded"`
* Persist webhook event for notification (with retry behavior)
* Do NOT attempt further reconciliation (manual operator intervention required)

### Reconciliation Idempotency

Reconciliation queries are idempotent:

* Multiple reconciliation attempts for same transaction are safe
* Use `acquirer_reference` to query Mercado Pago (not `idempotency_key`)
* Acquirer reference uniquely identifies payment at Mercado Pago
* Idempotency protection prevents charging customer twice

### Rate Limiting & Concurrency

Per-merchant reconciliation limits MUST be enforced:

```text
Maximum 2 concurrent reconciliation queries per merchant_id
If 3rd reconciliation triggered: Queue it (max queue depth: 10)
If queue exceeds 10: Drop new reconciliation requests and alert operator
```

This prevents single merchant from starving other merchants' reconciliation.

### Timeout Ambiguity Resolution

If Mercado Pago timeout occurs during reconciliation query:

* Retry using exponential backoff (as above)
* After 3 retries without response: Assume payment did not process (transition to FAILED)
* Do NOT assume success on timeout (conservative approach)

If Mercado Pago returns HTTP 429 (rate limit):

* Backoff aggressively: 5s, 10s, 20s (separate from normal retry backoff)
* Alert operator if rate limit persists > 60 seconds
* After 3 rate-limit responses: Transition to FAILED and alert

### Stale UNKNOWN Detection

Background job MUST scan for UNKNOWN transactions exceeding 5-minute window:

```text
If UNKNOWN transaction exists > 5 minutes without final state:
  - Log ERROR: "Stale UNKNOWN transaction {id}"
  - Alert operator with transaction details
  - Transition to FAILED (after operator approval, or via manual intervention)
```

Operators have 24-hour window to manually resolve stale UNKNOWN transactions.
After 24 hours, automatic transition to FAILED occurs.

---

## Reconciliation & Idempotency Interaction

After UNKNOWN → FAILED transition, new payment requests using same `idempotency_key`:

* **Scenario 1 (within 24 hours of original request):** Idempotency cache remains active; client receives FAILED response with same transaction_id
* **Scenario 2 (after 24 hours):** Idempotency cache expired; new request creates NEW transaction (different transaction_id, same idempotency_key)

Merchants SHOULD NOT retry after FAILED state; instead, should request new payment with fresh idempotency_key.

---

# Request Validation Rules

Inbound payment requests MUST validate:

* amount,
* currency,
* payment method,
* card information,
* customer information,
* and merchant authentication.

Validation failures MUST:

* terminate processing immediately,
* prevent external acquirer calls,
* and return deterministic validation responses.

---

## Card Validation Rules

Card validation MUST include:

* Luhn validation,
* expiration validation,
* CVV validation,
* and supported card length validation.

Raw PAN values MUST NEVER persist after tokenization.

---

# 3DS & Risk-Based Authentication Rules

Transactions are evaluated using lightweight risk evaluation rules.

High-risk transactions MUST transition to:

```text
VALIDATED -> CHALLENGE_PENDING
```

Successful 3DS completion transitions:

```text
CHALLENGE_PENDING -> AUTHENTICATED
AUTHENTICATED -> PROCESSING
```

---

## Complete 3DS State Machine

The 3DS lifecycle includes explicit handling for all outcomes:

### Valid 3DS Transitions

```text
VALIDATED -> CHALLENGE_PENDING (high-risk flagged)
CHALLENGE_PENDING -> AUTHENTICATED (successful challenge)
CHALLENGE_PENDING -> DECLINED (failed challenge verification)
CHALLENGE_PENDING -> FAILED (challenge timeout or network error)
AUTHENTICATED -> PROCESSING (proceed to payment)
```

### 3DS Challenge Generation

When transaction transitions to CHALLENGE_PENDING:

1. Endpoint: `POST /api/v1/payments/{transaction_id}/3ds-challenge`
2. Called after transaction moves to CHALLENGE_PENDING state
3. Generates JWT containing: `{ transaction_id, merchant_id, amount, exp: now+10min }`
4. Signs JWT using **HS256** with merchant-specific key (from secure configuration)
5. Returns: `{ challenge_url, jwt_token, expires_at }`

### Challenge JWT Requirements

3DS challenge tokens MUST:

* expire within **10 minutes** of generation,
* contain signed transaction metadata (transaction_id, merchant_id, amount),
* include replay protection (nonce = SHA256(transaction_id + merchant_id)),
* be validated on challenge completion.

### Challenge Replay Protection

Nonce-based replay detection:

1. Challenge JWT includes: `nonce = SHA256(transaction_id + merchant_id)`
2. When challenge completes, validate nonce hasn't been used before
3. Nonce stored in Redis with **24-hour TTL** for deduplication
4. If nonce reused: Return 400 Bad Request, log security alert
5. Prevents customer from replaying old challenge completion across different transactions

### Challenge Timeout Handling

Challenge timeout after **10 minutes** without completion:

```text
Background job scans for CHALLENGE_PENDING transactions > 10 minutes old
Automatic transition: CHALLENGE_PENDING -> FAILED
Persist audit entry: actor="system", message="Challenge timeout"
Dispatch webhook notification: "Challenge expired, payment cancelled"
```

Customer can then initiate new payment with NEW idempotency_key.

### Failed Challenge Recovery

If customer fails 3DS verification (e.g., wrong OTP):

```text
Transaction transitions: CHALLENGE_PENDING -> DECLINED
Persist audit entry with failure reason
Dispatch webhook notification: "Authentication failed"
Customer can retry with same card or different card (new idempotency_key)
```

### Challenge Abandonment Handling

If customer abandons challenge mid-flow (closes browser):

```text
No explicit abandonment event from customer browser
Detected via timeout mechanism (above)
After 10 minutes: Automatic CHALLENGE_PENDING -> FAILED
No additional customer action required
```

Customer can retry payment (same card, new idempotency_key, will proceed to PROCESSING if low-risk now).

---

## 3DS Integration with Low-Latency Path

Low-risk transactions bypass 3DS entirely:

```text
VALIDATED -> PROCESSING (skip CHALLENGE_PENDING)
```

This preserves **<1 second SLA** for standard checkout flow.

Only high-risk transactions incur 3DS delay:

```text
VALIDATED -> CHALLENGE_PENDING -> AUTHENTICATED -> PROCESSING
3DS latency: 10-60 seconds typical, up to 10 minutes timeout
```

---

## Risk Evaluation Rules

Risk-based authentication (RBA) evaluates transaction metadata:

```
Low-risk: card previously used, low amount, trusted merchant
High-risk: new card, high amount, unusual geography, velocity checks
```

If flagged high-risk:
* Enforce CHALLENGE_PENDING transition (3DS required)
* Cannot bypass challenge

If flagged low-risk:
* Skip CHALLENGE_PENDING
* Proceed directly to PROCESSING
* Preserves <1s SLA guarantee

---

# Persistence Rules

PostgreSQL is the authoritative source of truth for transaction state.

Redis MUST NEVER be treated as authoritative payment persistence.

---

## Transaction Persistence

Every transaction update MUST:

* execute inside a database transaction,
* generate an audit log entry,
* and respect optimistic locking rules.

Optimistic locking MUST use:

```java
@Version
```

to prevent lost updates.

---

## Optimistic Locking Semantics

Transaction versioning provides concurrency control for distributed state transitions.

### Version Increment Strategy

Transaction.`version` MUST be incremented on **EVERY state transition**, including:

```text
CREATED -> VALIDATED (version: 0 -> 1)
VALIDATED -> CHALLENGE_PENDING (version: 1 -> 2)
VALIDATED -> PROCESSING (version: 1 -> 2)
CHALLENGE_PENDING -> AUTHENTICATED (version: 2 -> 3)
AUTHENTICATED -> PROCESSING (version: 3 -> 4)
PROCESSING -> UNKNOWN (version: 4 -> 5)
PROCESSING -> COMPLETED (version: 4 -> 5)
PROCESSING -> DECLINED (version: 4 -> 5)
PROCESSING -> FAILED (version: 4 -> 5)
UNKNOWN -> COMPLETED (version: 5 -> 6)
UNKNOWN -> DECLINED (version: 5 -> 6)
UNKNOWN -> FAILED (version: 5 -> 6)
```

Every state transition increments version, preventing lost updates.

### Version Conflict Detection

Optimistic lock conflict occurs when:

```text
Thread A: Queries transaction (version = 5)
Thread B: Queries same transaction (version = 5)
Thread A: Updates transaction, increments version to 6, commits
Thread B: Attempts update with stale version 5, FAILS
```

Thread B receives `OptimisticLockException` and MUST NOT auto-retry (indicates concurrent modification).

### Conflict Handling & Retry Strategy

All state transitions MUST implement retry loop with bounded retries:

```java
for (int attempt = 0; attempt < 3; attempt++) {
  try {
    Transaction current = transactionRepository.findById(id);
    updateState(current);  // Updates version field
    transactionRepository.save(current);  // Version conflict checked here
    return;  // Success
  } catch (OptimisticLockException e) {
    if (attempt == 2) {
      // Final attempt failed; do NOT retry further
      logger.error("OptimisticLockException after 3 attempts for transaction {}", id);
      // Log to monitoring system for operator visibility
      monitoringService.recordOptimisticLockFailure();
      throw e;  // Fail fast; operator intervention required
    }
    // Sleep with exponential backoff
    Thread.sleep(100 * (attempt + 1)); // 100ms, 200ms, 300ms
  }
}
```

### Concurrent Transition Scenarios

**Scenario: Webhook + Reconciliation race**

```
Time T0: Transaction in UNKNOWN state (version = 5)
Time T1: Webhook thread queries transaction (version = 5)
Time T2: Reconciliation thread queries transaction (version = 5)
Time T3: Webhook thread updates to COMPLETED (version = 6)
Time T4: Reconciliation thread attempts update to COMPLETED (version = 5)
        -> OptimisticLockException (version mismatch)
        -> Retry loop: re-query (version now = 6)
        -> Detect: already in COMPLETED state
        -> Return success (idempotent outcome, different executor)
```

**Scenario: Double reconciliation attempt**

```
Time T0: Transaction UNKNOWN (version = 5)
Time T1: Reconciliation worker A queries (version = 5)
Time T2: Reconciliation worker B queries (version = 5)
Time T3: Worker A updates to COMPLETED (version = 6)
Time T4: Worker B retries, re-queries, finds COMPLETED (version = 6)
        -> Recognizes already completed by worker A
        -> Persists audit entry: "Reconciliation already successful"
        -> Returns success
```

### Outbox Events & Version Management

Outbox events have INDEPENDENT version field for replay safety:

```
transaction.version = 5
outbox_event.version = 1
outbox_event.status = "PENDING"
```

Outbox events are NOT versioned with parent transaction; they have separate lifecycle.

### Monitoring & Alerting

Operators MUST track:

```
Metric: optimistic_lock_conflicts_total (counter)
Alert: If optimistic_lock_conflicts_total increases by >1 per minute
       -> Indicates concurrent bug or high-contention transaction
       -> Operator investigates: webhook/reconciliation race?
```

---

## Audit Log Persistence

Audit logging is critical to transaction consistency guarantee.

### Audit Log Entry Creation

Every state transition MUST create audit entry WITHIN SAME TRANSACTION:

```java
Transaction.updateState(newState);
transaction.incrementVersion();
AuditLog entry = new AuditLog(
  transactionId: transaction.id,
  oldStatus: oldState,
  newStatus: newState,
  actor: "system" | "webhook" | "reconciliation",
  checksum: SHA256(transaction.id + oldState + newState + actor),
  createdAt: now
);
auditLogRepository.save(entry);
transactionRepository.save(transaction);
// Single database transaction; both persist or both rollback
```

### Audit Log Failure Handling

If audit log insertion fails:

* Entire transaction state update MUST rollback
* Return 500 Internal Server Error to caller
* Operator must investigate disk space / database errors
* Client can safely retry (idempotency protection active)

Audit log table must have:
* Automated alerts if free disk space < 10%
* Retention policy: Keep 7 days of audit logs, archive older entries
* This prevents disk quota from unexpectedly failing mid-transaction

---

## Required Tables

### transactions

Required fields:

```
id (UUID primary key)
merchant_id (UUID foreign key)
idempotency_key (UUID, part of uniqueness constraint)
amount (BIGINT in cents)
currency (VARCHAR(3))
status (VARCHAR(20))
payload_hash (VARCHAR(64), SHA-256 hex)
masked_card (VARCHAR(20), e.g., "411111XXXXXX1111")
card_token_id (VARCHAR(100))
acquirer_reference (VARCHAR(255), Mercado Pago payment ID)
version (INT, optimistic lock counter, default 0)
created_at (TIMESTAMP)
updated_at (TIMESTAMP)

Unique Constraint: UNIQUE(merchant_id, idempotency_key)
Indexes:
  - PRIMARY KEY (id)
  - INDEX (merchant_id)
  - INDEX (status)
  - INDEX (created_at)
  - INDEX (merchant_id, created_at) for efficient merchant queries
```

### audit_logs

Required fields:

```
id (UUID primary key)
transaction_id (UUID foreign key -> transactions.id)
old_status (VARCHAR(20))
new_status (VARCHAR(20))
actor (VARCHAR(50), e.g., "system", "webhook", "reconciliation")
checksum (VARCHAR(64), SHA256 hex, immutable)
created_at (TIMESTAMP)

Indexes:
  - PRIMARY KEY (id)
  - INDEX (transaction_id)
  - INDEX (created_at) for retention policy queries
```

Checksum is computed at write time and NEVER modified. Used to detect tampering.

### outbox_events

Required fields:

```
id (UUID primary key)
event_type (VARCHAR(50), e.g., "payment.completed", "payment.failed")
aggregate_id (UUID, typically transaction_id)
payload (TEXT JSON, webhook body)
status (VARCHAR(20), "PENDING" | "DELIVERED" | "FAILED")
retry_count (INT, default 0, max 5)
created_at (TIMESTAMP)
updated_at (TIMESTAMP)
delivered_at (TIMESTAMP, nullable)

Indexes:
  - PRIMARY KEY (id)
  - INDEX (status) for worker polling
  - INDEX (created_at) for cleanup queries
```

---

# Transactional Outbox Rules

Webhook delivery and asynchronous notifications MUST use a transactional outbox pattern.

The outbox event MUST persist inside the same database transaction as the payment update.

This prevents event loss during:

* crashes,
* container restarts,
* or partial failures.

---

## Async Processing & Client Visibility

After synchronous payment response (COMPLETED/DECLINED/FAILED):

1. **Client receives HTTP status immediately** (synchronous guarantee)
   - Transaction state persisted in database
   - Outbox event persisted in same transaction
   - Strong ACID guarantee

2. **Outbox event is GUARANTEED persisted** (strong guarantee)
   - If database commit succeeds, outbox event exists
   - If database commit fails, neither transaction nor outbox persist
   - All-or-nothing consistency

3. **Client MAY report status to end-user immediately** (async safety)
   - Safe to report payment status from HTTP response
   - Webhook delivery happens asynchronously (weak guarantee)
   - Decouples client visibility from webhook delivery

4. **Webhook delivery happens asynchronously** (eventual delivery)
   - Separate worker threads dispatch webhooks
   - Delivery happens independently of client connection
   - May take seconds to minutes to complete

### Important: Webhook Delivery SLA

**Merchants should NOT assume immediate webhook delivery.**

Target delivery: **< 1 second** after transaction completion
Acceptable delay: up to **30 seconds** (eventual consistency model)

If merchant requires synchronous confirmation:
- Do NOT rely on webhook as primary confirmation channel
- Use polling: `GET /api/v1/payments/{transaction_id}`
- Poll interval: 100ms to 1s
- Polling immediately after sync response will see transaction complete

SLA violation alerting:
- Alert operator if any webhook pending **> 5 minutes**
- Investigate worker backlog or Mercado Pago connectivity
- Alert: `"Webhook delivery delay exceeds SLA for event {id}"`

---

## Outbox Processing Rules

After transaction commit:

* Asynchronous workers poll outbox table for PENDING events
* Dispatch events to merchant webhook endpoints
* Persist webhook acknowledgment on successful delivery
* Retry failed deliveries with exponential backoff
* Mark events as DELIVERED only after receiving 200 OK

Failed deliveries MUST remain recoverable and retryable.

### Webhook Dispatch Pattern

Polling-based webhook dispatch:

```
1. Worker thread: Every 100ms, query outbox for PENDING events
2. Batch retrieval: Up to 100 events per poll (efficiency)
3. Fire-and-forget: Submit to virtual thread executor (non-blocking task execution; database persistence remains JPA/blocking)
4. Continue polling: Don't wait for dispatch completion
5. Failure handling: Mark failed events, retry on next poll
```

This pattern minimizes worker contention and scales with virtual threads.

### Webhook Idempotency

Every webhook MUST include idempotency headers:

```
X-Webhook-ID: UUID (unique per outbox event, immutable)
X-Idempotency-Key: UUID (matching payment idempotency_key)
X-Retry-Count: 0, 1, 2, ... (shows retry attempt number)
X-Timestamp: ISO8601 timestamp
X-Signature: HMAC-SHA256 signature of payload
```

Merchant MUST:
- Verify X-Signature using shared secret
- Treat multiple webhooks with same X-Webhook-ID as duplicates
- Respond 200 OK to all deliveries (including duplicates)
- Safe to retry indefinitely; merchant handles deduplication

Platform MUST:
- Persist webhook acknowledgment timestamp in outbox_events
- Mark event as DELIVERED only after receiving 200 OK
- Retry up to **5 times** with exponential backoff: 1s, 2s, 4s, 8s, 16s
- Total retry window: ~31 seconds
- After 5 retries fail, mark as FAILED and alert operator

### Webhook Failure Alerts

If webhook delivery fails after all retries:

```
Alert operator: "Webhook delivery failed for payment {id}, merchant {merchant_id}"
Alert includes: outbox_event.id, final_error, retry_count
Operator can: manually trigger retry, investigate merchant endpoint, pause webhooks
```

---

---

# Security Rules

Merchant API keys:

* MUST be hashed,
* MUST never appear in logs,
* and MUST use secure comparison.

---

## PAN Handling Rules

Card numbers MUST NEVER:

* persist raw,
* appear in logs,
* appear in exception traces,
* or be exposed in webhook payloads.

The Tokenization subsystem defined in `ARCHITECTURE.md` is responsible for:

* PAN isolation,
* AES-256-GCM encryption,
* token generation,
* and masked card persistence.

---

# Performance Rules

The payment platform targets:

| Metric              | Target            |
| ------------------- | ----------------- |
| p99 latency         | < 1s              |
| baseline throughput | 500 RPS/container |
| spike throughput    | 2000+ RPS         |

The specification assumes the infrastructure and scaling model defined in `ARCHITECTURE.md`.

---

# Failure Handling Rules

| Scenario                 | Expected Behavior                    |
| ------------------------ | ------------------------------------ |
| Invalid payload          | Return 400                           |
| Invalid API key          | Return 401                           |
| Duplicate request        | Return cached response               |
| Redis unavailable        | Fallback to DB uniqueness guarantees |
| Mercado Pago timeout     | Move transaction to UNKNOWN          |
| PostgreSQL unavailable   | Return 503                           |
| 3DS timeout              | FAILED                               |
| Webhook delivery failure | Retry using outbox processing        |

---

# Health Check & Readiness Probe Behavior

The Core Payment Service MUST expose health endpoint for infrastructure monitoring.

---

## Health Check Endpoint

```
GET /actuator/health
Content-Type: application/json
```

### Healthy Response

HTTP 200 OK if ALL conditions are met:

```json
{
  "status": "UP",
  "components": {
    "postgres": {
      "status": "UP",
      "responseTime": 5
    },
    "redis": {
      "status": "UP",
      "responseTime": 2
    }
  }
}
```

Conditions:
- PostgreSQL connection pool can acquire connection (timeout: 1s)
- Redis connection succeeds (timeout: 1s)

### Unhealthy Response

HTTP 503 Service Unavailable if ANY condition failed:

```json
{
  "status": "DOWN",
  "components": {
    "postgres": {
      "status": "DOWN",
      "error": "Connection timeout after 1s"
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

### Health Check Guidelines

- Health check MUST NOT call Mercado Pago (external dependency; not critical to core service)
- Health check MUST NOT perform expensive queries (keep response <100ms)
- Load balancer polls `/actuator/health` every **30 seconds** (readiness probe)
- Kubernetes polls `/actuator/health` every **60 seconds** (liveness probe)

---

# Request Timeout & Client Disconnect Handling

Proper timeout and cancellation handling ensures payment consistency.

---

## Request Lifecycle Timeouts

### HTTP Request Timeout (Nginx Layer)

- Configured at reverse proxy: **60 seconds**
- Must exceed Mercado Pago timeout (backup protection)
- If exceeded: Nginx returns 504 Gateway Timeout
- Core service continues processing (do NOT abort)

### Mercado Pago Call Timeout (Application Layer)

- Timeout configured: **500ms to 2 seconds** (tunable per environment)
- If Mercado Pago doesn't respond: transaction transitions to UNKNOWN
- Retry protection: idempotency guarantees prevent duplicate retries

### Timeout Ambiguity Resolution

If Mercado Pago timeout occurs:

```
1. Transaction transitions to UNKNOWN (NOT FAILED)
2. Reconciliation processing scheduled
3. Idempotency protection remains active
4. Client receives 202 Accepted (processing uncertain)
5. Client can retry; will receive UNKNOWN status until resolved
```

Reconciliation worker will eventually resolve UNKNOWN to final state.

---

## Client Disconnect Handling

If HTTP client closes connection mid-payment:

### Request Thread Behavior

- Payment thread MUST continue to completion (do NOT abort Mercado Pago call)
- **Reason:** Aborting mid-call leaves payment state uncertain at acquirer
- Transaction will persist in database (idempotency preserved)
- Client can retry and receive cached response

### Webhook Dispatch After Disconnect

- Webhook thread continues independently
- Merchant receives webhook regardless of client connection status
- This decouples client availability from payment confirmation guarantee

### Example Scenario

```
Time T0: Client submits payment, waits for response
Time T1: Mercado Pago starts processing (slow network)
Time T2: Customer closes browser (client closes HTTP connection)
Time T3: Core service continues waiting for Mercado Pago response
Time T4: Mercado Pago responds, transaction completed, persisted
Time T5: Outbox worker dispatches webhook to merchant
Time T6: Merchant processes order (customer never saw confirmation)
        -> But transaction is complete and idempotent
```

This is CORRECT behavior; customer doesn't receive confirmation but payment is complete.

---

## Request Cancellation Best Practice

Clients SHOULD:
- Wait for HTTP response before considering request complete
- If closing connection early: assume payment is uncertain
- Retry with same `Idempotency-Key` to get definitive status
- Poll `/api/v1/payments/{id}` for final state

Platform MUST:
- Continue processing despite client disconnect
- Not treat client disconnect as error signal
- Deliver webhooks independently

---

# Rate Limiting & Concurrency Rules

Rate limiting is handled at **Nginx layer** (reverse proxy).

---

## Rate Limiting Ownership

### Nginx (Reverse Proxy): First Line of Defense

- Enforces per-IP rate limiting (DDoS protection)
- Enforces per-merchant rate limiting (abuse prevention)
- Returns HTTP 429 Too Many Requests at edge
- Configuration managed centrally at infrastructure layer

### Application (Core Service): NOT Responsible for Rate Limiting

- Assume Nginx has already filtered abusive traffic
- Redis used ONLY for idempotency coordination, NOT rate limiting
- Do NOT implement application-level rate limiting (prevent double-limiting)

This design:
- Keeps rate limiting policy management centralized at Nginx
- Prevents double-limiting (confusing for debugging)
- Ensures rate limits are enforced even during service degradation

---

## Reconciliation Concurrency Control

Per-merchant reconciliation limits (NOT general rate limiting):

```
Maximum 2 concurrent reconciliation queries per merchant_id
If 3rd reconciliation triggered: Queue it (max queue depth: 10)
If queue exceeds 10: Drop new reconciliation requests and alert operator
```

This is application logic, not rate limiting.
Purpose: Prevent single merchant from starving other merchants' reconciliation.

---

# Example

If Mercado Pago fails to respond before the configured timeout:

* the transaction MUST NOT immediately become FAILED,
* the transaction transitions to UNKNOWN,
* reconciliation processing is scheduled,
* idempotency protection remains active,
* and the client receives a processing-uncertain response.

---

# Impact on the Project

This specification:

* reduces ambiguity during implementation,
* improves payment consistency,
* prevents duplicate charge scenarios,
* improves concurrent processing safety,
* strengthens distributed systems reliability,
* and introduces production-inspired payment architecture patterns.

The resulting system remains:

* maintainable,
* testable,
* horizontally scalable,
* and appropriate for an academic fintech project.
