# Payment Core: Setup and Testing Guide

## 1. System Overview

This system is a modern, event-driven payment processing core built with enterprise-grade patterns. Key features include:

*   **Secure Authentication:** Uses hashed API keys for merchants (Argon2).
*   **Idempotency:** Prevents duplicate transactions using a combination of idempotency keys and payload hashing.
*   **Resilience:** Employs the Transactional Outbox pattern for reliable webhook delivery and a Reconciliation Worker to handle payments in uncertain (`UNKNOWN`) states.
*   **Safety:** Uses optimistic locking to prevent data corruption from concurrent requests.
*   **Auditability:** Creates a structured, immutable audit trail for every significant action.

---

## 2. Setting Up the Environment

### Prerequisites

*   **Java 21+:** Required for virtual threads used by the background workers.
*   **Maven 3.8+:** For building the project.
*   **Docker & Docker Compose:** To easily run a PostgreSQL database.
*   **HTTP Client:** `curl`, Postman, or a similar tool for API testing.

### Step 1: Start the PostgreSQL Database

The system uses PostgreSQL for data persistence and Flyway for database migrations. The easiest way to get a database running is with Docker. Run ```docker compose up``` on the project root so the database can start

### Step 2: Configure the Application

Update your application's configuration file (e.g., `src/main/resources/application.properties`) to connect to the database.

```properties
# src/main/resources/application.properties

# ----------------------------------------
# DATABASE (POSTGRESQL)
# ----------------------------------------
# These should match the values in your docker-compose.yml
spring.datasource.url=jdbc:postgresql://localhost:5433/payment-service
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.jpa.show-sql=false

# JPA/Hibernate settings
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# ----------------------------------------
# CACHE (REDIS) - ainda nao aplicado, apenas existe aqui
# ----------------------------------------
# These should match the values in your docker-compose.yml
spring.data.redis.host=localhost
spring.data.redis.port=6379

# ----------------------------------------
# APPLICATION & WEB SERVER
# ----------------------------------------
server.port=8080

# Enable virtual threads for high concurrency
spring.threads.virtual.enabled=true

# Request timeout settings
spring.mvc.async.request-timeout=60000

# ----------------------------------------
# EXTERNAL SERVICES (MERCADO PAGO MOCK) - ainda nao aplicado
# ----------------------------------------
# URL for the payment acquirer (e.g., Mercado Pago)
# For local testing, this might point to a mock server like WireMock.
acquirer.mercado-pago.api.url=http://localhost:9090/v1/payments
acquirer.mercado-pago.api.token=YOUR_MERCADO_PAGO_TEST_TOKEN

# ----------------------------------------
# SECURITY - ainda nao aplicado
# ----------------------------------------
# Secret key for signing 3DS JWTs
security.jwt.secret=your-super-secret-key-for-jwt-signing-that-is-at-least-256-bits-long
# Secret key for signing merchant webhooks (HMAC-SHA256)
security.merchant.webhook-secret=your-super-secret-key-for-signing-webhooks

# Webhook Dispatch Worker Configuration

# Webhook secret key prefix
webhook.secret-key-prefix=webhook_secret_

# HTTP timeout for webhook requests (milliseconds)
webhook.http-timeout-ms=5000

# Polling interval (milliseconds)
webhook.polling-interval-ms=100

# SLA monitoring interval (milliseconds)
webhook.sla-monitor-interval-ms=30000

# SLA threshold (minutes)
webhook.sla-threshold-minutes=5

# Batch size per poll
# Maximum number of events to process per poll
webhook.batch-size=100

# Maximum retry attempts
webhook.max-retries=5

# Retry backoff delays (milliseconds)
# Exponential backoff delays between retry attempts
webhook.retry-backoff-delays=1000,2000,4000,8000,16000

# Enable webhook dispatch worker
# Set to false to disable webhook dispatch (for testing)
webhook.dispatch.enabled=true

# Enable SLA monitoring
# Set to false to disable SLA monitoring (for testing)
webhook.sla-monitor.enabled=true

# Logging level for webhook dispatch
# DEBUG: Verbose logging for troubleshooting
# INFO: Standard logging
# WARN: Only warnings and errors
logging.level.com.acabouomony.payment.domain.service.WebhookDispatchService=INFO
logging.level.com.acabouomony.payment.infrastructure.worker.WebhookDispatchWorker=INFO
logging.level.com.acabouomony.payment.domain.service.WebhookSignatureService=INFO

```

---

## 3. Running the System

### Step 1: Build the Application

Compile the code and package it into a JAR file using Maven.

```bash
mvn clean package
```

### Step 2: Run the Application

You should see Spring Boot startup logs, including messages from Flyway indicating successful migrations.

### Step 3: Create a Merchant and API Key

Merchant API keys are hashed with Argon2 and stored in the `merchants` table. You cannot insert a plaintext key. You must first generate the hash.

#### How to Generate an API Key Hash

Generate a new API key Hash by using the file "Argon2KeyGenerator.java" in the test package, there you can provide a plain text and it will be converted:

#### Inserting the Merchant Record

Now, use the generated hash to insert a merchant record into your database. The `INSERT` statement below is a ready-to-use example.

*   **Plaintext API Key for this example:** `teste-key`
*   **Webhook URL:** The URL `https://webhook.site/...` is a great tool for testing. It will capture any webhooks sent by the application for this merchant. GET THIS FROM 'https://webhook.site/#!/view/851873e9-cba1-42cc-be81-1392527b1300'

```sql
-- This record uses 'teste-key' as the plaintext API key.
INSERT INTO merchants (id, merchant_id, api_key_hash, webhook_url, created_at)
VALUES (
    'f47ac10b-58cc-4372-a567-0e02b2c3d479',
    'a1b2c3d4-e5f6-4a3b-9c8d-7e6f5a4b3c2d',
    '$argon2id$v=19$m=65536,t=3,p=1$H8ywTNrF+fIm0Is4EHQASA$UDGVw9FSUIVF4aLT6L6U3dsuRca6CLb6s4pegP0lZqI',
    'https://webhook.site/851873e9-cba1-42cc-be81-1392527b1300',
    NOW()
);
```

Your system is now running and ready for testing.

---

## 4. Testing the API

Use `curl` or Postman or Bruno to interact with the payment API.

**API Endpoint:** `POST /api/v1/payments`
**Authentication:** `Authorization: Bearer teste-key`

### Scenario 1: Successful Payment Request (Happy Path)

This is a new, valid request.

```bash
curl -X POST http://localhost:8080/api/v1/payments \
-H "Content-Type: application/json" \
-H "Authorization: Bearer teste-key" \
-d '{
    "amount": 10000,
    "currency": "BRL",
    "idempotency_key": "a4e3b2c1-f6a8-4f9b-9c8d-7e6f5a4b3c2d",
    "payment_method": {
        "card_token_id": "card_tok_1234567890abcdef",
        "masked_card": "411111XXXXXX1111"
    },
    "customer_id": "cust_abc123"
}'
```

**Expected Response:**
*   **Status:** `202 Accepted`. This indicates the request was accepted for processing. The final outcome will be delivered later via webhook.
*   **Body:** A JSON object with the transaction details, including a `transactionId` and the initial `status` (e.g., `VALIDATED` or `PROCESSING`).

### Scenario 2: Idempotent Request (Duplicate)

Send the **exact same request** as Scenario 1 again.

**Expected Response:**
*   **Status:** `200 OK`, `202 Accepted`, or `409 Conflict`. The system recognizes the duplicate `idempotency_key` and returns the status of the original transaction without processing it again.
    *   `200 OK`: If the original transaction is in a terminal state (`COMPLETED`, `DECLINED`).
    *   `409 Conflict`: If the original is still `PROCESSING`.
    *   `202 Accepted`: If the original is `UNKNOWN`.

### Scenario 3: Idempotency Key Re-use with Different Payload

Send a request with the same `idempotency_key` as before, but change the `amount`.

```bash
# Same idempotency_key, different amount
curl -X POST http://localhost:8080/api/v1/payments \
-H "Content-Type: application/json" \
-H "Authorization: Bearer teste-key" \
-d '{
    "amount": 5000,
    "currency": "BRL",
    "idempotency_key": "a4e3b2c1-f6a8-4f9b-9c8d-7e6f5a4b3c2d",
    "payment_method": {
        "card_token_id": "card_tok_1234567890abcdef",
        "masked_card": "411111XXXXXX1111"
    },
    "customer_id": "cust_abc123"
}'
```

**Expected Response:**
*   **Status:** `400 Bad Request`. The system detects that the idempotency key is being reused for a different transaction, which is not allowed.

### Scenario 4: Invalid Request (Validation Error)

Send a request with an invalid currency code (the spec implies only `BRL` is supported).

```bash
# Invalid currency "USD"
curl -X POST http://localhost:8080/api/v1/payments \
-H "Content-Type: application/json" \
-H "Authorization: Bearer teste-key" \
-d '{
    "amount": 10000,
    "currency": "USD",
    "idempotency_key": "b5d4c3a2-e7b9-5g0c-ad9e-8f7g6b5c4d3e",
    "payment_method": {
        "card_token_id": "card_tok_1234567890abcdef",
        "masked_card": "411111XXXXXX1111"
    }
}'
```

**Expected Response:**
*   **Status:** `400 Bad Request`.
*   **Body:** A JSON object detailing the validation error (e.g., "Currency must be BRL").

### Scenario 5: Authentication Failure

Send a request with an incorrect API key.

```bash
curl -X POST http://localhost:8080/api/v1/payments \
-H "Authorization: Bearer wrong-api-key" \
-H "Content-Type: application/json" \
-d '{
    "amount": 10000,
    "currency": "BRL",
    "idempotency_key": "c6e5d4b3-f8c0-6h1d-be0f-9g8h7c6d5e4f",
    "payment_method": {
        "card_token_id": "card_tok_1234567890abcdef",
        "masked_card": "411111XXXXXX1111"
    }
}'
```

**Expected Response:**
*   **Status:** `401 Unauthorized`.

---

## 5. Observing the Results

1.  **Check the Database:**
    *   `SELECT * FROM transactions;` to see the created payment record and its current status.
    *   `SELECT * FROM audit_logs WHERE transaction_id = '...';` to see the state transition history.
    *   `SELECT * FROM outbox_events WHERE aggregate_id = '...';` to see the webhook event that was created.

2.  **Check the Webhook:**
    *   Open the webhook.site URL you used in the `INSERT` statement (`https://webhook.site/851873e9-cba1-42cc-be81-1392527b1300`).
    *   You should see the webhook payload delivered by the `WebhookDispatchWorker`, complete with a signature and idempotency headers (`X-Webhook-ID`, `X-Signature`, etc.).