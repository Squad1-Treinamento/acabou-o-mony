---
id: plan-001
status: active
links:
  - spec/tech-plans/index.md
  - spec/specs/spec-001-core-payment-processing.md
  - spec/tasks/index.md
---

# Core Payment Processing Service Technical Plan

This technical plan details the implementation strategy, directory structure, data models, and logical sequence required to build the Core Payment Processing Service for "Acabou o Mony."

## Architecture Overview and Data Flow

### Components
- **Reverse Proxy (Nginx)**: Terminates TLS 1.3 connections, applies edge rate limits, and routes requests into the isolated internal subnet.
- **Payment Processing Core Service (Spring Boot 3.x)**: A stateless Web MVC service running inside an embedded Tomcat container optimized with Java 21's Virtual Threads (`spring.threads.virtual.enabled=true`).
- **Redis Cache (Redis 7.x)**: Sub-millisecond lookup engine for transaction idempotency locks and real-time merchant-customer velocity metrics.
- **PostgreSQL Database (PostgreSQL 16)**: Secure, ACID-compliant relational store for the merchant keys, payment ledger, and tamper-resistant audit logs.
- **Acquiring Gateway (Mercado Pago API)**: External payment clearing layer integrated via non-blocking HTTP/2 `WebClient`.
- **Asynchronous Task Executor**: Built-in Virtual Thread thread pool executing offloaded background events (webhooks, audit hashing, Prometheus metrics).

### Core Transaction Sequence
```text
[Client]   [Nginx]   [Core Engine]   [Redis]   [PostgreSQL]   [Mercado Pago]
   │          │            │            │            │               │
   │── POST ─►│            │            │            │               │ (Sub-1s SLA Edge)
   │  /pay    │── Routed ─►│            │            │               │
   │          │            │── SET NX ─►│            │               │ (Idempotency check)
   │          │            │◄─ Locked ──│            │               │
   │          │            │                         │               │
   │          │            │────── API Key Auth ────►│               │ (Merchant verified)
   │          │            │◄───── Active Merchant ──│               │
   │          │            │                         │               │
   │          │            │── Save State CREATED ──►│               │ (DB Ledger Start)
   │          │            │                         │               │
   │          │            │── Get Velocity check ──►│               │ (RBA Risk check)
   │          │            │◄── Low Risk ────────────│               │
   │          │            │                         │               │
   │          │            │────────────────── POST /payments ──────►│ (Acquirer call)
   │          │            │◄───────────────── 201 Approved ─────────│
   │          │            │                         │               │
   │          │            │── Update COMPLETED ────►│               │ (DB Ledger Seal)
   │          │            │── Cache Response ──────►│               │ (Idempotency Save)
   │◄─ 201 Created ────────│                         │               │
   │          │            │── [Async VT Executor] ─────────────────►│ (Webhook/Audits)
```

## Stack and Dependencies

- **Runtime Environment**: Java 21 JDK, Spring Boot 3.3.x, Docker Compose V2.
- **Database Servers**: PostgreSQL 16, Redis 7.2 (Alpine).
- **Core Libraries**:
  - `org.springframework.boot:spring-boot-starter-web`: Embedded Tomcat container running on Virtual Threads.
  - `org.springframework.boot:spring-boot-starter-security`: For merchant authentication filters.
  - `org.springframework.boot:spring-boot-starter-data-jpa`: Relational persistence using Hibernate.
  - `org.postgresql:postgresql`: PostgreSQL JDBC Driver.
  - `org.springframework.boot:spring-boot-starter-data-redis`: Redis client integration.
  - `org.springframework.boot:spring-boot-starter-webflux`: Enabler for non-blocking HTTP/2 `WebClient` outbound calls.
  - `io.projectreactor.netty:reactor-netty`: Embedded Netty client supporting HTTP/2 connections.
  - `org.projectlombok:lombok`: Code reduction utility.
  - `org.springframework.boot:spring-boot-starter-actuator` & `io.micrometer:micrometer-registry-prometheus`: Real-time system monitoring.
- **Restricted Libraries**:
  - No reactive databases (do NOT use Spring Data R2DBC).
  - No message brokers (do NOT use Apache Kafka or RabbitMQ).
  - No external orchestrators (do NOT use Kubernetes dependencies).

## Design Patterns and Code Conventions

### Code Structure
```text
src/main/java/com/acabouomony/payment/
├── config/                 # Security, Redis, Thread Pool, WebClient, Auditing
├── controller/             # REST endpoints (Payments, Challenges, Webhooks)
├── dto/                    # Requests, responses, and API mapping structures
├── exception/              # Global exceptions and domain-specific mapping
├── filter/                 # API Key authentication filter, Idempotency interceptor
├── model/                  # JPA Relational Entities (Merchant, Transaction, AuditLog)
├── repository/             # JPA Query Interfaces
└── service/                # Business components
    ├── audit/              # Cryptographic log signing services
    ├── idempotency/        # Redis lock and cache management services
    ├── integration/        # Mercado Pago integration services
    ├── risk/               # Risk evaluation and velocity tracking engines
    ├── security/           # API Key lookup and tokenization services
    └── transaction/        # Processing orchestrator and lifecycle services
```

### Key Coding Patterns
- **Orchestrator Pattern**: `PaymentOrchestrationService` coordinates the steps of transaction lifecycle (validation, tokenization, risk checking, acquirer request, persistence, and async worker dispatch).
- **Strategy Pattern**: `RiskRule` interfaces applied in a pipeline to evaluate transaction fraud profiles dynamically.
- **Interceptor Pattern**: Filters intercept inbound servlet requests to enforce tokenization extraction and idempotency checks before invoking core controllers.
- **Repository Pattern**: Extends Spring Data JPA Repositories for state updates.

### Conventions
- **Naming**: `camelCase` for Java fields and methods, `PascalCase` for classes, `SNAKE_CASE` for database table/column mappings.
- **Optimistic Locking**: Every write transaction must update database tables with an incremented `@Version` field to block concurrent modification hazards.
- **Immutability**: DTO structures must use Lombok `@Value` or Java 16 `record` constructs.

## Persistence and Data Modeling

### Data Models
- **Merchant Entity (`merchants`)**:
  - `id` UUID PRIMARY KEY
  - `name` VARCHAR(100) NOT NULL
  - `api_key_hash` VARCHAR(64) UNIQUE NOT NULL (SHA-256)
  - `status` VARCHAR(20) NOT NULL (ACTIVE, SUSPENDED)
  - `created_at` TIMESTAMP NOT NULL
- **Transaction Entity (`transactions`)**:
  - `id` UUID PRIMARY KEY
  - `merchant_id` UUID NOT NULL (FK to merchants, INDEXED)
  - `amount` NUMERIC(15,2) NOT NULL
  - `currency` VARCHAR(3) NOT NULL
  - `payment_method` VARCHAR(15) NOT NULL
  - `status` VARCHAR(25) NOT NULL
  - `masked_card` VARCHAR(16) NOT NULL
  - `card_token_id` UUID NOT NULL
  - `created_at` TIMESTAMP NOT NULL
  - `updated_at` TIMESTAMP NOT NULL
  - `version` INT NOT NULL (for optimistic locking)
- **Audit Log Entity (`audit_logs`)**:
  - `id` UUID PRIMARY KEY
  - `transaction_id` UUID NOT NULL (FK to transactions, INDEXED)
  - `old_status` VARCHAR(25)
  - `new_status` VARCHAR(25) NOT NULL
  - `actor` VARCHAR(50) NOT NULL
  - `checksum` VARCHAR(64) NOT NULL (SHA-256)
  - `created_at` TIMESTAMP NOT NULL

### Persistence Rules
- Default database isolation level set to `READ_COMMITTED`.
- Database operations must utilize JPA Entity Managers mapping to a HikariCP pool tuned to `30-50` connections.
- Verification audits must calculate log hashes asynchronously and store them using append-only access paths. No modification is ever permitted on `audit_logs`.

## Non-Functional Requirements and Security

### Performance Budgets
- **Total Synchronous API p99 Limit**: `< 1000ms` under 2000 RPS.
- **Phase Limits**:
  - Pre-validation and Auth: `< 50ms`
  - Fraud/Velocity scoring: `< 100ms`
  - PostgreSQL transaction ledger entry: `< 300ms`
  - Mercado Pago HTTP/2 call: `< 400ms`

### Cryptography & PCI Compliance
- PAN numbers are intercepted inside the tokenization module and immediately hashed/encrypted.
- **Encryption**: AES-256-GCM (`AES/GCM/NoPadding`) is used to encrypt raw credit card PANs before caching details or requesting card tokens.
- **Salt & Secrets**: Key strings are configured in Docker environment variables (`TOKENIZER_MASTER_KEY` and `LEDGER_SALT`) and never hardcoded in files.

## Task Breakdown Preview

- **[planned] [Foundation]** Initialize Spring Boot 3.x project, configure Maven, setup multi-container Docker Compose (PostgreSQL, Redis, App), and configure `spring.threads.virtual.enabled`.
- **[planned] [Database]** Create Liquibase/Flyway migrations mapping `merchants`, `transactions`, and `audit_logs` with optimistic locking versions, constraint indexes, and create Spring Data repositories.
- **[planned] [Security]** Build Spring Security filter checking SHA-256 hashed API Keys, implement AES-256-GCM encryptor bean for tokenization, and write secure header configurations.
- **[planned] [Idempotency]** Design Redis transaction interceptor using SET NX with 24h TTL, incorporating atomic locks and completed transaction serialization.
- **[planned] [Integration]** Write reactive WebClient client for Mercado Pago API with strict 500ms timeouts, fallback error handlers, and mock server tests.
- **[planned] [Risk-Engine]** Implement risk rules (amount threshold, velocity tracking in Redis) and the 3DS pending status `/api/v1/payments/3ds-challenge` JWT verification endpoint.
- **[planned] [Core-Flow]** Integrate controller routing, request payload parsing, transactional DB orchestrator, and background executor dispatcher.
- **[planned] [Async-Workers]** Create ExecutorService running on Virtual Threads to handle webhook dispatches (with exponential retries) and compute cryptographic ledger checksums.
- **[planned] [Testing]** Author integration suites evaluating payment success, validation, idempotency conflicts, timeout resilience, and Locust load benchmarks.

## Examples

### API Inbound Submit Request
- **Headers**:
  - `X-Merchant-API-Key`: `mch_key_prod_abc123`
  - `Idempotency-Key`: `e396601f-0e9e-4b68-8097-df417c822765`
- **Body**:
  ```json
  {
    "amount": "150.00",
    "currency": "BRL",
    "payment_method": "credit_card",
    "card": {
      "card_holder_name": "ANA SOUZA",
      "card_number": "4111111111111111",
      "expiration_month": 12,
      "expiration_year": 2028,
      "security_code": "123"
    },
    "customer": {
      "id": "cust_3940294",
      "email": "ana.souza@gmail.com"
    }
  }
  ```

### API Success Outbound Response (Status 201 Created)
- **Body**:
  ```json
  {
    "transaction_id": "8fa97c36-64f3-4f9e-be5d-10b659c25f4a",
    "status": "COMPLETED",
    "masked_card": "411111XXXXXX1111",
    "amount": "150.00",
    "currency": "BRL",
    "created_at": "2026-05-28T14:30:00Z"
  }
  ```
