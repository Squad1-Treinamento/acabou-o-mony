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

The system uses PostgreSQL for data persistence and Flyway for database migrations. The easiest way to get a database running is with Docker.

1.  Create a `docker-compose.yml` file in your project's root directory:

    ```yaml
    version: '3.8'
    services:
      postgres:
        image: postgres:15
        container_name: payment-core-db
        environment:
          POSTGRES_USER: paymentuser
          POSTGRES_PASSWORD: paymentpassword
          POSTGRES_DB: paymentdb
        ports:
          - "5432:5432"
        volumes:
          - postgres_data:/var/lib/postgresql/data

    volumes:
      postgres_data:
    ```

2.  Run the database from your terminal:
    ```bash
    docker-compose up -d
    ```

### Step 2: Configure the Application

Update your application's configuration file (e.g., `src/main/resources/application.properties` or `application.yml`) to connect to the database.

```properties
# src/main/resources/application.properties

# Database Connection
spring.datasource.url=jdbc:postgresql://localhost:5432/paymentdb
spring.datasource.username=paymentuser
spring.datasource.password=paymentpassword

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=validate # Flyway manages the schema
```

---

## 3. Running the System

### Step 1: Build the Application

Compile the code and package it into a JAR file using Maven.

```bash
mvn clean package
```

### Step 2: Run the Application

Execute the JAR file. On startup, Flyway will automatically run the database migrations (`V1__...`, `V2__...`, etc.) to create all the necessary tables and constraints.

```bash
java -jar target/payment-core-0.0.1-SNAPSHOT.jar
```
You should see Spring Boot startup logs, including messages from Flyway indicating successful migrations.

### Step 3: Create a Merchant and API Key

Merchant API keys are hashed with Argon2 and stored in the `merchants` table. You cannot insert a plaintext key. You must first generate the hash.

#### How to Generate an API Key Hash

You can use a simple Java utility or a dedicated script to generate a secure hash. Here is a conceptual example using Spring Security's `Argon2PasswordEncoder`:

```java
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

public class ApiKeyGenerator {
    public static void main(String[] args) {
        // Use secure defaults from the spec
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);

        String plainTextApiKey = "my-secret-api-key"; // Choose your secret key
        String hash = encoder.encode(plainTextApiKey);

        System.out.println("Plaintext API Key: " + plainTextApiKey);
        System.out.println("Hashed API Key: " + hash);
    }
}
```

#### Inserting the Merchant Record

Now, use the generated hash to insert a merchant record into your database. The `INSERT` statement below is a ready-to-use example.

*   **Plaintext API Key for this example:** `my-secret-api-key`
*   **Webhook URL:** The URL `https://webhook.site/...` is a great tool for testing. It will capture any webhooks sent by the application for this merchant.

```sql
-- This record uses 'my-secret-api-key' as the plaintext API key.
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

Use `curl` or Postman to interact with the payment API.

**API Endpoint:** `POST /api/v1/payments`
**Authentication:** `Authorization: Bearer my-secret-api-key`

### Scenario 1: Successful Payment Request (Happy Path)

This is a new, valid request.

```bash
curl -X POST http://localhost:8080/api/v1/payments \
-H "Content-Type: application/json" \
-H "Authorization: Bearer my-secret-api-key" \
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
-H "Authorization: Bearer my-secret-api-key" \
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
-H "Authorization: Bearer my-secret-api-key" \
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