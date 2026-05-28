---
id: spec-001
status: active
links:
  - spec/specs/index.md
  - spec/user-stories/us-001-transaction-processing.md
  - spec/user-stories/us-002-scalability.md
  - spec/user-stories/us-003-transaction-security.md
  - spec/user-stories/us-004-live-conversational-integration.md
  - spec/tech-plans/index.md
---

# Core Payment Processing Service Specification

This document defines the behavioral specifications, business rules, and validation criteria for the Core Payment Processing Service. It translates user stories into clear, testable, and deterministic rules to guide implementation and verification.

## Definition
The Core Payment Processing Service is the central transactional engine of the "Acabou o Mony" gateway. It provides high-performance, low-latency, and highly-secure processing of credit and debit card payments. Built on Java 21 and Spring Boot 3.x, it integrates with Redis for idempotency and rate limiting, PostgreSQL 16 for ledger persistence, and the Mercado Pago API for merchant acquiring.

## Purpose
The purpose of this service is to enable merchants to accept online credit and debit card payments rapidly and securely. It is designed to sustain heavy transactional surges during Live Commerce events and Conversational Commerce interactions (such as inside live streams or chats) without platform redirection or customer disruption, strictly maintaining a synchronous sub-second latency SLA (<1s).

## Core Responsibilities
- **Synchronous Entrypoint**: Expose high-performance REST APIs for credit and debit card payments.
- **Request Pre-Validation**: Perform rapid schema, Luhn algorithm, data format, and range verification.
- **Idempotency Enforcement**: Prevent double-billing by resolving incoming requests against active Redis locks and completed transaction caches.
- **Risk-Based Authentication (RBA)**: Run low-latency risk checks to dynamically trigger 3D Secure (3DS) challenges.
- **Tokenization & Masking**: Shield sensitive card details through AES-256-GCM tokenization and masked PAN persistence.
- **Acquirer Integration**: Manage secure, timeout-restricted HTTP/2 outbound calls to the Mercado Pago API.
- **ACID Persistence Ledger**: Ensure strict, read-committed transactional persistence of payment states in PostgreSQL.
- **Asynchronous Execution**: Dispatch post-processing tasks (webhook notifications, audit seals, Prometheus telemetry) asynchronously using virtual thread executors.

## Payment Processing Lifecycle
The transaction progresses through a well-defined state machine to guarantee consistency:

```text
       [Inbound Request]
               │
               ▼
          ┌─────────┐
          │ CREATED │ ──(Validation/Auth fails)──► [FAILED]
          └────┬────┘
               │ (Payload & Auth Valid)
               ▼
         ┌───────────┐
         │ VALIDATED │
         └─────┬─────┘
               │
      ┌────────┴────────┐
      │ (High Risk)     │ (Low Risk / Bypassed)
      ▼                 ▼
┌──────────────┐  ┌────────────┐
│  CHALLENGE_  │  │ PROCESSING │ ◄──(MFA Success)
│   PENDING    │  └─────┬──────┘
└──────┬───────┘        │
       │                ├───────────────────────────────┬───────────────────────────────┐
       │ (MFA Fail/     │ (Acquirer Approved)           │ (Acquirer Rejected)           │ (Timeout/API Error)
       │  Timeout)      ▼                               ▼                               ▼
       ▼          ┌───────────┐                   ┌───────────┐                   ┌───────────┐
   [FAILED]       │ COMPLETED │                   │ DECLINED  │                   │  FAILED   │
                  └───────────┘                   └───────────┘                   └───────────┘
```

- **States**:
  - `CREATED`: Request initialized; idempotency verified and lock acquired in Redis.
  - `VALIDATED`: JSON schema validated; merchant authorization verified.
  - `CHALLENGE_PENDING`: Risk evaluation flagged high risk; transaction suspended pending 3DS validation.
  - `PROCESSING`: Payload compiled and dispatched to Mercado Pago API; awaiting HTTP response.
  - `COMPLETED`: Mercado Pago API returned authorization success (201 Approved).
  - `DECLINED`: Mercado Pago API returned acquirer-level rejection (e.g., insufficient funds, card block).
  - `FAILED`: Aborted due to validation failure, 3DS authentication failure/timeout, system crash, or network timeout.

## Request Validation Rules
Every inbound transaction to `/api/v1/payments` must be validated before processing continues:

- **Structure Constraints**:
  - `amount`: Decimal string (e.g., `"199.50"`). Must be greater than `0.00`, positive, formatted with up to 2 decimal places. Mandatory.
  - `currency`: String. Exactly `"BRL"` or `"USD"` (ISO 4217). Mandatory.
  - `payment_method`: String. Must be exactly `"credit_card"` or `"debit_card"`. Mandatory.
  - `card_token_id`: String (UUID v4 format). Required if raw card object is omitted.
  - `card`: Object. Required if `card_token_id` is omitted.
    - `card_holder_name`: String. 2 to 100 characters. Alphabetic and spaces only. Mandatory.
    - `card_number`: String. 13 to 19 digits. Must pass standard Luhn algorithm check. Mandatory.
    - `expiration_month`: Integer. Range `1` to `12`. Mandatory.
    - `expiration_year`: Integer. Range `2026` to `2050` (rolling validation, must be >= current year). Mandatory.
    - `security_code`: String. Exactly 3 or 4 digits. Mandatory.
  - `customer`: Object. Mandatory.
    - `id`: String (UUID v4 format). Customer ID on the parent platform. Mandatory.
    - `email`: String. Must match standard RFC 5322 email format. Mandatory.

- **Validation Protocol**:
  - If any input validation fails, the service must abort further execution, return HTTP status `400 Bad Request`, set the transaction status to `FAILED` with a validation reason, and return a structured JSON response identifying the failing fields:
    ```json
    {
      "code": "VALIDATION_FAILED",
      "message": "The request payload contains invalid fields",
      "errors": [
        {
          "field": "card.card_number",
          "message": "Invalid card number (Luhn check failed)"
        }
      ]
    }
    ```

## Authentication & Authorization Rules
API endpoints are strictly secured to protect financial ledger access:

- **Merchant Credentials**:
  - Every request must include the header `X-Merchant-API-Key`.
  - The API Key is looked up in the PostgreSQL `merchants` table. To protect against leak risks, API keys are stored in the database as SHA-256 hashes.
  - The inbound raw key is hashed using SHA-256 and matched against active database records.
- **Access Decisions**:
  - If the key is missing, malformed, or doesn't match an active record, return `401 Unauthorized` with JSON:
    ```json
    {
      "code": "UNAUTHORIZED_ACCESS",
      "message": "Invalid or inactive API Key"
    }
    ```
  - Upon successful authentication, the request context is bound to the corresponding `merchant_id`.

## Idempotency Rules
To prevent double-billing during high-velocity Live Commerce events, the service implements a strict Redis-based idempotency lock:

- **Key Construction**:
  - `idempotency:merchant:{merchant_id}:key:{idempotency_key}`
- **Execution Flow**:
  1. Construct the key using the authenticated `merchant_id` and the required `Idempotency-Key` header (UUID v4).
  2. Execute Redis command: `SET {key} "IN_PROGRESS" NX PX 86400000` (expires in 24 hours).
  3. **Lock Denied**: If the command returns null, a transaction with this key is already running or complete.
     - Read the existing value from Redis.
     - If the value is `"IN_PROGRESS"`, return HTTP `409 Conflict`:
       ```json
       {
         "code": "CONCURRENT_TRANSACTION",
         "message": "A transaction with this idempotency key is already in progress"
       }
       ```
     - If the value is a cached JSON response, return the cached payload immediately with HTTP `200 OK` (or `201 Created` as cached) and the header `X-Cache-Lookup: HIT`.
  4. **Lock Acquired**: Proceed to process the transaction.
  5. **Completion**: Upon transaction finalization (`COMPLETED`, `DECLINED`, or permanent non-retryable `FAILED`), serialize the API response payload (status, transaction ID, masked card, timestamp) and write it to Redis replacing `"IN_PROGRESS"`, preserving the remaining 24-hour TTL.
  6. **Recovery**: If a transient system error occurs (e.g., PostgreSQL downtime before the acquirer was invoked), delete the Redis key immediately to allow a subsequent merchant retry.

## Risk & 3DS Flow
Transactions must be evaluated for fraud in real-time to decide whether step-up MFA/3DS is required:

- **Risk-Based Evaluation Engine (RBA)**:
  - Runs in `<100ms` prior to external calls.
  - Flagged as **HIGH RISK** if:
    - The transaction `amount > 5000.00 BRL`.
    - Velocity check: The card number has initiated `> 3 transactions within a rolling 60-second window` (tracked in Redis).
    - New Merchant: The merchant account is `< 7 days old` and `amount > 500.00 BRL`.
- **Bypass (Low Risk)**:
  - Proceed directly to Mercado Pago payment processing.
- **Step-Up Challenge (High Risk)**:
  - Suspend transaction, update database status to `CHALLENGE_PENDING`.
  - Generate a secure, short-lived JWT (10-minute expiry) containing signed transaction metadata.
  - Return HTTP `200 OK` with status `3DS_CHALLENGE_REQUIRED` and challenge details:
    ```json
    {
      "status": "3DS_CHALLENGE_REQUIRED",
      "redirect_url": "https://api.acabouomony.com.br/api/v1/payments/3ds-challenge?token=JWT_TOKEN",
      "transaction_id": "TX_UUID_12345"
    }
    ```
- **Live / Conversational Integration**:
  - The checkout web app or conversational chat card displays this `redirect_url` in an iframe, overlay, or inline webview.
  - The customer verifies their identity (SMS/Push/Token).
  - The challenge handler verifies the JWT, updates database transaction state to `AUTHENTICATED`, and proceeds to execute payment.
  - The conversational flow remains unbroken because the entire challenge is handled in-situ without redirecting the customer away from the stream or chat.

## Transaction Persistence Rules
The service must maintain an immutable, tamper-resistant transaction ledger in PostgreSQL:

- **ACID Transaction Configuration**:
  - Read-committed isolation level (`Isolation.READ_COMMITTED`).
  - Optimistic locking configured on the `transactions` table using a `@Version` field to prevent concurrent overwrites.
- **State Modifications**:
  - Every update to a transaction status MUST generate a corresponding insert in the `audit_logs` table inside the same database transaction.
  - Schema constraints:
    - `transactions`: `id` (UUID, Primary Key), `merchant_id` (UUID, Foreign Key, Index), `amount` (Numeric), `currency` (Varchar), `status` (Varchar), `masked_card` (Varchar), `card_token_id` (UUID), `version` (Integer).
    - `audit_logs`: `id` (UUID, Primary Key), `transaction_id` (UUID, Foreign Key, Index), `old_status` (Varchar), `new_status` (Varchar), `actor` (Varchar), `checksum` (Varchar), `created_at` (Timestamp).
- **Audit Verification Cryptography**:
  - To prevent ledger manipulation, the `checksum` column in `audit_logs` is populated with a SHA-256 hash computed over: `transaction_id + old_status + new_status + timestamp + SECRET_SALT`. This provides an easily verifiable, tamper-resistant history.

## Mercado Pago Integration Rules
Outbound payment clearing is delegated directly to the Mercado Pago API:

- **Client Design**:
  - Non-blocking `WebClient` configured with HTTP/2 to enable high connection reuse and low-latency handshakes.
  - Timeout configurations:
    - `ConnectTimeout`: 500ms
    - `ReadTimeout`: 500ms
    - `WriteTimeout`: 500ms
- **Payload Translation**:
  - Core transaction data is converted into Mercado Pago's JSON schema (`POST /v1/payments` endpoint):
    ```json
    {
      "transaction_amount": 100.00,
      "token": "CARD_TOKEN_FROM_TOKENIZER",
      "description": "Acabou o Mony Payment",
      "payment_method_id": "visa",
      "payer": {
        "email": "customer@email.com"
      },
      "metadata": {
        "acabou_o_mony_tx_id": "TX_UUID_12345"
      }
    }
    ```
- **Response Resolution**:
  - **HTTP 201 Approved**: Update internal status to `COMPLETED`. Save acquirer reference ID.
  - **HTTP 201 Rejected / Pending**: Map the acquirer reason code to internal `DECLINED` status (e.g., `cc_rejected_insufficient_amount` -> insufficient funds).
  - **HTTP 4xx / 5xx**: Map to `FAILED` transaction status. Raise system alerts.
  - **Connection/Read Timeout**: Catch exception, set database transaction status to `FAILED` with retryable code `TIMEOUT_ON_ACQUIRER`. Launch background check task to verify status on Mercado Pago and void the payment if it went through, preventing double charges.

## Async Processing Rules
All secondary, post-payment operations are offloaded from the synchronous response path to ensure SLA compliance:

- **Asynchronous Execution Pool**:
  - Powered by a Java 21 `ExecutorService` utilizing virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`). Virtual threads allow unlimited, low-overhead concurrent tasks without pooling bottlenecks.
- **Tasks Offloaded**:
  - **Webhook Broadcaster**: Send a POST request to the merchant's callback URL with the final transaction status and metadata. Configured with a 3-try retry schedule (exponential backoff: 1s, 2s, 4s).
  - **Audit Ledger Seal**: Calculate and write the ledger audit cryptographic checksum.
  - **Telemetry Exporter**: Publish latency, database pool state, and payment outcomes to Micrometer/Prometheus registries.

## Failure Scenarios
The service maps exceptions to deterministic error codes and HTTP responses:

| Scenario / Error Cause | Handling Logic | HTTP Status | Response Payload |
| :--- | :--- | :---: | :--- |
| **Invalid Payload Schema** | Validation engine fails fast. Transaction marked `FAILED`. | `400 Bad Request` | `{"code": "VALIDATION_FAILED", "errors": [...]}` |
| **Expired / Bad API Key** | Pre-auth check fails. | `401 Unauthorized` | `{"code": "UNAUTHORIZED_ACCESS", "message": "..."}` |
| **Duplicate Transaction** | Redis intercepts key. Cached response returned. | `200 OK` / `211` | Cached response body with `X-Cache-Lookup: HIT` |
| **Concurrent Execution** | Redis detects lock is active (value = `IN_PROGRESS`). | `409 Conflict` | `{"code": "CONCURRENT_TRANSACTION", "message": "..."}` |
| **Redis Offline** | Graceful fallback to SQL index lookups. Continue execution. | (Continues) | Process payment; log warning telemetry. |
| **Database Down** | Return error; if transaction was not logged, clear Redis key. | `503 Service Unavailable` | `{"code": "DATABASE_UNAVAILABLE", "message": "..."}` |
| **Acquirer Timeout** | Abort request after 500ms. Mark state `FAILED`. | `504 Gateway Timeout` | `{"code": "ACQUIRER_TIMEOUT", "message": "..."}` |
| **Insufficient Funds** | Map acquirer decline state. Update state to `DECLINED`. | `422 Unprocessable` | `{"code": "INSUFFICIENT_FUNDS", "status": "DECLINED"}` |

## Security Constraints
To conform with PCI-DSS guidelines and maximize transaction safety:

- **Network Security**:
  - Enforce HTTPS with TLS 1.3 only (strong cipher suites like `TLS_AES_256_GCM_SHA384`).
  - Core Service runs inside an isolated Docker subnet, accessed externally only via Nginx Reverse Proxy.
- **PAN Tokenization (PCI-DSS Section 3.4)**:
  - Primary Account Numbers (PAN / credit card numbers) are **never** persisted in raw format.
  - **Tokenizer Sub-system**:
    - The Core Service delegates raw card data to an internal Tokenization component.
    - This component encrypts the PAN using **AES-256-GCM** with a master key sourced from an environment configuration.
    - It generates a random, cryptographically secure `card_token_id` and mask (e.g., `411111XXXXXX1111`).
    - The raw card number is discarded from memory immediately after tokenization.
    - PostgreSQL ledger only holds the `card_token_id` and the masked PAN.

## Performance Constraints
- **Response SLA**: Synchronous API response returned to the client in `< 1.0s` (Goal: p99 < 1s under sustained load).
- **Internal Budgets**:
  - Payload pre-validation and authentication: `< 50ms`.
  - Risk-Based Authentication Evaluation: `< 100ms`.
  - PostgreSQL transaction ledger write: `< 300ms`.
  - Outbound Mercado Pago HTTP/2 call: `< 400ms`.
- **Concurrency & Throughput**:
  - Support a baseline of `500 RPS` per Docker container.
  - Sustain overall load spikes of up to `2000 RPS` during Live Commerce events.

## Scalability Expectations
- **Stateless Instances**:
  - The Core Payment Service holds zero local state.
  - Scale horizontally by spinning up additional container replicas (`docker compose scale payment-service=N`).
  - Shares database connections via HikariCP pointing to the PostgreSQL cluster.
  - Shares session and idempotency locks via the Redis cluster.
- **Elastic Scale Metric**:
  - Automatically trigger container replica scaling when average instance CPU usage exceeds `70%` or memory utilization exceeds `75%` over a 2-minute window.

## Acceptance Criteria
- **Scenario 1: Successful Low-Risk Credit Card Purchase**
  - **Given** an authenticated merchant `MerchantX`
  - **And** a payment request of `150.00 BRL` for a low-risk user profile
  - **When** the merchant sends a `POST /api/v1/payments` with a unique idempotency key
  - **Then** the service tokenizes the card details and returns masked card `411111XXXXXX1111`
  - **And** executes an outbound HTTP/2 call to Mercado Pago, receiving a success status in under 400ms
  - **And** records the transaction as `COMPLETED` in the database with a validated audit log entry
  - **And** returns a `201 Created` response containing the transaction ID in under 1 second.

- **Scenario 2: High-Risk Purchase Suspended for 3DS Step-up**
  - **Given** an authenticated merchant `MerchantX`
  - **And** a payment request of `7500.00 BRL` (exceeding high-risk thresholds)
  - **When** the merchant sends the `POST` request
  - **Then** the service registers the transaction status as `CHALLENGE_PENDING`
  - **And** returns an HTTP `200 OK` with status `3DS_CHALLENGE_REQUIRED` and a redirect URL with JWT in under 150ms.

- **Scenario 3: Submitting an Identical Request Within 24 Hours**
  - **Given** a payment transaction was already completed under idempotency key `idemp-999`
  - **When** the merchant submits the identical request body and key `idemp-999`
  - **Then** the service intercepts the request via Redis
  - **And** returns the exact cached payment confirmation with header `X-Cache-Lookup: HIT` in under 50ms without hitting the database or Mercado Pago.

- **Scenario 4: Graceful Handling of Acquirer Network Timeout**
  - **Given** a valid payment request is submitted
  - **And** Mercado Pago API fails to respond within the 500ms timeout
  - **When** the request times out
  - **Then** the payment service rolls back transaction state to `FAILED` with retryable code `TIMEOUT_ON_ACQUIRER`
  - **And** returns a `504 Gateway Timeout` response in under 1 second.

## Project Impact
- **Ana (Live Commerce)**: Eliminates high stream cart abandonment by keeping checkouts lightning-fast and preventing double-charging.
- **In-Chat Integration**: Enables conversational and live-feed checkout interfaces to render overlays for 3DS challenges, keeping customer journey frictionless and inside the host app.
- **System Integrity**: Enforces strict audit trails, PCI-compliant tokenization, and auto-scaling resilience.

## Implementation Recommendation
This section details the optimal, low-complexity architecture for this academic payment service, balancing high-performance scalability with developer productivity:

### Architectural Choices & Virtual Threads
- **Runtime Model**: Java 21 with Virtual Threads (Project Loom) on standard Spring Boot 3.x with Tomcat is highly recommended over Spring WebFlux + Netty + R2DBC.
  - **Why**: Traditional reactive stacks (WebFlux/R2DBC) have a steep learning curve, lack mature JPA toolsets, and increase code complexity. In contrast, Virtual Threads allow developers to write standard synchronous, readable JDBC-compatible blocking code (Spring Data JPA) while achieving near-reactive performance. When a virtual thread blocks on a JDBC query or WebClient network call, the underlying JVM carrier thread is released to run other virtual threads.
  - **Trade-off & Mitigation**: Ensure no carrier thread "pinning" occurs. Carrier pinning happens if a virtual thread enters a `synchronized` block and executes blocking operations. To prevent this:
    - Avoid using legacy synchronized libraries.
    - If locking is required, use modern `java.util.concurrent.locks.ReentrantLock`.
    - Ensure Spring Boot 3.x features (which are optimized for virtual threads) are enabled via configuration: `spring.threads.virtual.enabled=true`.

### Database Connection Pool Tuning
- **HikariCP Configuration**: Because virtual threads can scale to thousands of concurrent executions, the bottleneck easily shifts from CPU threads to Database connection threads.
  - Recommend sizing the HikariCP pool to `30-50` connections instead of the default `10`.
  - Set `connectionTimeout` to `1000` (1 second) so requests fail fast under severe database pressure rather than stalling threads.
  - Enforce composite indexes on columns frequently filtered during lookups: `(merchant_id, status)` and `(idempotency_key)`.

### Idempotency Lock Sizing
- **Redis SET NX Lock**:
  - Redis standalone instance (or a standard primary-replica set) is chosen instead of a complex Redis Cluster. This reduces setup overhead, satisfying academic project feasibility while offering sub-millisecond key checking.
  - Command execution is deterministic and fast: `redis.set(key, "IN_PROGRESS", SetArgs.Builder.nx().px(86400000))`.

### Simplified Tokenization
- **Encryption Sub-system**:
  - To minimize operational overhead, replace enterprise HashiCorp Vault infrastructure with a secure encryption library bean loaded inside the Spring container.
  - Use Java's native `Cipher` class with `AES/GCM/NoPadding`. The 256-bit AES master key is injected via secure system environment variables (`TOKENIZER_MASTER_KEY`).
  - This preserves PCI-DSS data-at-rest encryption requirements while maintaining extremely high developer productivity and keeping Docker Compose configuration simple.
