# Acabou o Mony: Payment Gateway Architecture Design & Implementation Blueprint

This blueprint outlines the technical architecture, security protocols, scaling strategies, and implementation checklist for the "Acabou o Mony" Payment Gateway. The system is built with **Modern Java (Java 21 / Spring Boot 3.x)**, **Docker**, and integrates directly with the **Mercado Pago API** as the acquiring layer to support Live and Conversational Commerce.

---

## 1. High-Level Architectural Design

The payment gateway processes critical financial operations. To satisfy the `<1s` latency SLA and support horizontal auto-scaling, a streamlined **reactive, non-blocking architecture** is implemented. The API Gateway layer has been removed to reduce network hops, placing edge responsibilities squarely on the Reverse Proxy.

```text
                  ┌──────────────────────────────────────────────┐
                  │   API Consumers (Live/Conversational Comm.)  │
                  └──────────────────────┬───────────────────────┘
                                         │ HTTPS / TLS 1.3
                                         ▼
                  ┌──────────────────────────────────────────────┐
                  │      Reverse Proxy & Load Balancer           │
                  │   (Nginx: TLS Termination & Rate Limiting)   │
                  └──────────┬─────────────────────────────┬─────┘
                             │ gRPC/ HTTP/2                │ HTTP/2
                             ▼                             ▼
┌────────────────────────┐  ┌──────────────────────────────────────────────┐ Internal HTTP/2 ┌─────────────────────────────────┐
│   Mercado Pago API     │◄─┤       Core Payment Processing Service        │────────────────►│      3DS / MFA Auth Engine      │
│ (External Acquirer)    │  │    (Spring Boot 3.x, Spring WebFlux, Netty)  │                 │  (3D Secure 2.x authentication) │
└────────────────────────┘  └────┬───────────────────────┬─────────────────┘                 └──┬──────────────────────────────┘
                                 │                       │                                      │
                                 │ R2DBC (Non-blocking)  │ Redis Protocol                       │ Redis Protocol
                                 ▼                       ▼                                      │
                            ┌──────────────────┐   ┌────────────────────┐                       │
                            │ PostgreSQL 16    │   │ Redis Cluster      │ ◄─────────────────────┘
                            │ (ACID Ledger)    │   │ (Shared Cache)     │
                            └──────────────────┘   └────────────────────┘
```
## 2. High-Performance Transaction Processing (<1s SLA)

### Architecture Decisions
* **Runtime Platform:** Java 21 leveraging Spring Boot 3.x with Spring WebFlux and Netty. This provides a fully reactive, non-blocking programming model from edge to database, achieving maximum performance and throughput under heavy concurrent load.
* **Microservices Communication:** The 3DS/MFA Auth Engine is completely decoupled from the relational database. It relies entirely on the Redis Cluster for ultra-fast session state management and communicates authentication results back to the Core Service via internal network calls (e.g., HTTP/2 WebClient).
* **Reverse Proxy / Load Balancer:** Kept structurally simple but robust by supporting Nginx. These handle the essential implementations of external load balancing, TLS 1.3 termination, and edge-level rate limiting before traffic hits the core application.
* **Database Engine:** **PostgreSQL 16** integrated via R2DBC (Reactive Relational Database Connectivity) to ensure non-blocking database interactions.
* **Caching & Idempotency Layer:** **Redis** for sub-millisecond lookups of idempotent request keys, temporary MFA session states, and token validations.
* **External Acquiring:** **Mercado Pago API** replaces any mock processors to handle real-world transaction clearing and acquiring.
* **Asynchronous Execution:** Heavy, non-blocking post-processing (e.g., webhook notifications, analytics ingestion, audit logging) is handled natively through Project Reactor's asynchronous event loops.

### SLA Compliance Mechanics
1. **Fast-path Validation (<50ms):**
   * Pre-validation of schema, constraints, and cryptographic signatures at the Core Service level.
   * Idempotency checks against Redis: If a duplicate `Idempotency-Key` is found within a 24-hour window, the cached response is instantly returned.
2. **Risk-Based Authentication (RBA) (<100ms):**
   * Rapid evaluation of transaction metadata against basic fraud parameters stored in Redis (handled directly within the Core Processing Service).
   * If flagged as low-risk, bypasses the step-up 3DS flow to preserve the `<1s` latency.
3. **Database Ledger Write (<300ms):**
   * Optimized execution of the core ledger update via R2DBC inside a reactive transaction boundary.
   * Use of composite indexes on frequently searched fields (`transaction_id`, `merchant_id`).
4. **Mercado Pago Communication (<400ms):**
   * Outbound communication to the Mercado Pago API via high-performance HTTP/2-enabled `WebClient` with aggressive timeouts (e.g., 500ms connection/read timeouts).

---

## 3. Automated Elastic Scalability (CPU/Memory Threshold: 70%)

### Scaling Strategy
The gateway is packaged as lightweight Docker containers designed for rapid startup times (<3 seconds to healthy state using JVM optimizations and Spring AOT/GraalVM if needed).

* **Container Orchestration Platform:** Kubernetes (Staging/Prod blueprint) / Docker Compose with replica scaling (Dev/Staging environment).
* **Target Scaling Metric:** **CPU Utilization >= 70%** or **Memory Utilization >= 75%** sustained over a 2-minute window.
* **Auto-Scaling Componentry:**
  * **Upstream Load Balancer (Nginx):** Dynamically detects new container replicas via DNS round-robin or service discovery.
  * **Staging Auto-scaler Simulation:** A bash script monitoring Docker statistics that spins up/down additional containers (`docker compose scale payment-service=N`) based on active Locust load measurements.
* **Zero-Degradation Deployment:** Graceful shutdown configuration (`server.shutdown=graceful` in Spring Boot) ensures active transactions have up to 30 seconds to complete before a container is terminated during scale-down operations.

### Real-Time Observability Stack
* **Metrics Ingestion:** **Micrometer** exporting directly to a **Prometheus** endpoint `/actuator/prometheus`.
* **Visualization Dashboard:** **Grafana** displaying real-time graphs for:
  * Transaction Throughput (Requests Per Second - RPS).
  * Latency Percentiles (p50, p95, p99) - ensuring p99 stays < 1000ms.
  * System Resources (CPU/Memory per JVM instance).
  * Error Rates (HTTP 4xx/5xx counts).
  * Active DB Connection Pool utilization.

---

## 4. Enterprise-Grade Security & PCI-DSS Compliance

### Cryptography & Transport Security
* **Network Isolation:** All external endpoints strictly enforce **HTTPS with TLS 1.3** handled at the Nginx layer, using strong cipher suites (e.g., `TLS_AES_256_GCM_SHA384`).
* **Sensitive Data at Rest (PCI-DSS Section 3.4):**
  * Primary Account Numbers (PANs/Credit Card numbers) are **never** stored in plain text.
  * A separate, highly isolated **Tokenization Service** intercepts the PAN, generates a unique, non-reversible surrogate token, and encrypts the actual PAN using **AES-256-GCM** with a master key stored in an external Key Management System (KMS / HashiCorp Vault mockup).
  * The main Database only records the masked card number (`411111XXXXXX1111`) and the token surrogate.

### 3D Secure (3DS) & Multi-Factor Authentication
* **Standard Secure Flow:** Pre-authorized, authenticated API requests from pre-vetted merchants complete instantly.
* **Step-Up Authentication (3D Secure 2.x):**
  * Enforced for high-risk flags, high-value transactions, or first-time card usages.
  * When triggered, the backend returns a `3DS_CHALLENGE_REQUIRED` status with a redirect URL and a JWT token, persisting the challenge state strictly in Redis.
  * The Core Processing SLA for safe API calls remains intact (<1s) because standard calls bypass this flow entirely. The separated 3DS Engine validates the MFA token via Redis and asynchronously notifies the Core Service to finalize the ledger update.

### Vulnerability Prevention & Auditing
* **Input Sanitization & Injection Prevention:** SQL Injection is prevented by utilizing parameterized queries / Reactive Repositories. Cross-Site Scripting (XSS) is mitigated by strict content-type headers and input encoders.
* **Secure Audit Log:** Every security-sensitive action (e.g., key rotation, token access, failed log-ins) is logged to a write-once ledger using structured JSON logging containing a cryptographic checksum to prevent tampering.

---

## 5. Architectural Implementation Roadmap & Checklist

### Phase 1: Core System & Transaction Processing
- [ ] Initialize Spring Boot 3.x project with Java 21, Spring WebFlux, Spring Security, Spring Data R2DBC, and PostgreSQL R2DBC Driver.
- [ ] Implement database schema with full auditing, indexing, and strict constraints for `transactions`, `merchants`, and `audit_logs`.
- [ ] Develop the core Payment Processing REST Controller for `/api/v1/payments` (Credit/Debit) with robust model validation.
- [ ] Write the high-performance **Mercado Pago API Integration** using a reactive HTTP `WebClient` with aggressive timeouts for acquiring.
- [ ] Implement a sub-millisecond idempotency check interceptor using Redis cache.
- [ ] Configure reactive pipelines utilizing Project Reactor to offload background reporting, webhook emission, and audit logging.

### Phase 2: Security & PCI-DSS Hardening
- [ ] Implement Tokenization Engine to safely encrypt (AES-256-GCM) and store credit card numbers.
- [ ] Enforce Spring Security rules directly in the Core Service, restricting endpoints to authorized merchant accounts using API Key / JWT tokens.
- [ ] Develop the Risk-Based Authentication Engine and 3DS flow (`/api/v1/payments/3ds-challenge`), utilizing Redis for session state and internal WebClient calls to update the Core Service.
- [ ] Configure Nginx reverse proxy for secure TLS 1.3 termination and baseline rate-limiting.

### Phase 3: Dockerization, Observability & Scaling
- [ ] Package Spring Boot application into a multi-stage Dockerfile optimized for small image sizes and fast execution.
- [ ] Create a `docker-compose.yml` defining the multi-container architecture: App container replicas, Nginx Load Balancer, PostgreSQL, and Redis.
- [ ] Add Prometheus and Grafana service containers to Docker Compose, configuring dashboards with key SLIs (RPS, Latency p95/p99, CPU/Mem).
- [ ] Implement simulated Auto-Scaling monitor bash script to dynamically scale containers based on resource pressure.

### Phase 4: Load Testing & Verification
- [ ] Author a comprehensive `locustfile.py` script simulating concurrent merchant API clients submitting both standard payments and 3DS payments.
- [ ] Run high-throughput endurance tests (e.g., 500+ Concurrent Users, up to 2000 RPS) to observe:
  - Latency SLA compliance (<1s).
  - Proper reactive memory management under WebFlux models.
  - Successful scale-up triggers at >70% CPU.
- [ ] Generate a final verification report summarizing test metrics and confirming system stability under Live Commerce load conditions.