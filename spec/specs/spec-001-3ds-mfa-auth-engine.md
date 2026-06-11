---
id: spec-001
status: active
links:
  - spec/specs/index.md
  - spec/user-stories/us-001-transaction-processing.md
  - spec/user-stories/us-003-transaction-security.md
  - ARCHITECTURE.md
---

# 3DS / MFA Auth Engine

## Context and Primary Objective

The 3DS / MFA Auth Engine provides step-up authentication for high-risk, high-value, or first-time transactions. It runs as an isolated service decoupled from the relational database, relying entirely on the Redis Cluster for session state. It communicates authentication results back to the Core Payment Processing Service via internal HTTP/2.

Primary objective: authenticate high-risk transactions with 3D Secure 2.x without degrading the <1s SLA for standard (low-risk) transactions.

## Functional Requirements (Behavior)

### FR-1: Risk Evaluation and 3DS Trigger
- The Core Service evaluates transaction metadata against fraud parameters stored in Redis.
- If flagged as low-risk, the transaction bypasses the 3DS flow and completes instantly.
- Step-Up Authentication is enforced when:
  - Risk score exceeds the configurable threshold.
  - Transaction value exceeds the configurable high-value limit.
  - Card has not been used successfully before (first-time usage).

### FR-2: 3DS Challenge Initiation
- When step-up is required, the Core Service resolves the ACS URL (via BIN lookup), creates the challenge session in Redis (including `acs_url`), signs the JWT, and returns HTTP 200 with status `3DS_CHALLENGE_REQUIRED`.
- The response includes:
  - A redirect URL pointing to the 3DS Engine's landing page (`/challenge/{challenge_id}`).
  - A signed JWT token encoding the transaction context (passed as query param `?jwt=<token>`).
- The Core Service persists the challenge session in Redis with a TTL.

### FR-2.5: Landing Page and Bank Redirect
- The 3DS Engine exposes `GET /challenge/{challenge_id}?jwt=<token>` — a landing page that:
  - Verifies the JWT (signature + expiry). If invalid, returns an error.
  - Reads the session from Redis and extracts the `acs_url`.
  - HTTP-redirects the cardholder's browser to the bank's ACS URL.
- No session state is mutated on the landing page — it is a read-only redirect.

### FR-3: 3DS Challenge Processing
- The cardholder's bank POSTs the MFA result to the 3DS Engine's verify endpoint (`POST /api/v1/3ds/verify`) via Nginx reverse proxy.
- The Engine validates the MFA token against the session in Redis (session exists + token non-empty).
- On success, it writes the authentication result to Redis and asynchronously notifies the Core Service.

### FR-4: Core Ledger Finalization
- The Core Service reads the authentication result from Redis.
- It finalizes the ledger update and submits the transaction to the Mercado Pago API.

### FR-5: Session Expiry and Cleanup
- Unresolved 3DS challenge sessions expire after a configurable TTL.
- Expired sessions are rejected with status `3DS_CHALLENGE_EXPIRED`.
- Redis keys are automatically evicted on TTL expiry.

### FR-6: Idempotency and Duplicate Prevention
- Each 3DS challenge carries a unique `challenge_id`.
- If the same `challenge_id` is received twice, the cached authentication result is returned without reprocessing.

## Acceptance Criteria (BDD)

### AC-1: Low-risk transaction bypasses 3DS
Given a transaction with a low risk score
When the Core Service evaluates the risk
Then the transaction proceeds without 3DS challenge
And the response time stays under 1s

### AC-2: High-risk transaction triggers 3DS challenge
Given a transaction with a risk score above the threshold
When the Core Service initiates payment
Then the response contains status `3DS_CHALLENGE_REQUIRED`
And the response includes a `redirect_url` and a signed `jwt_token`

### AC-3: First-time card usage triggers 3DS
Given a card that has never been used successfully
When a transaction is initiated with that card
Then the system enforces step-up authentication
And returns `3DS_CHALLENGE_REQUIRED`

### AC-4: Valid MFA token completes the transaction
Given a pending 3DS challenge session exists in Redis
When the 3DS Engine receives a valid MFA token
Then it writes success to Redis
And asynchronously notifies the Core Service
And the Core Service finalizes the ledger update
And the final status is `APPROVED`

### AC-5: Invalid MFA token rejects the transaction
Given a pending 3DS challenge session exists in Redis
When the 3DS Engine receives an invalid MFA token
Then the transaction is rejected
And the final status is `DECLINED`

### AC-6: Expired 3DS challenge is rejected
Given a 3DS challenge session whose TTL has expired
When the cardholder submits the MFA token
Then the system returns status `3DS_CHALLENGE_EXPIRED`
And the transaction is not processed

### AC-7: Idempotent challenge processing
Given a 3DS challenge with `challenge_id=X` has already been processed
When the same `challenge_id=X` is submitted again
Then the cached result is returned
And no duplicate processing occurs

### AC-8: Standard SLA is preserved
Given the system is processing both standard and 3DS transactions concurrently
When a low-risk transaction is evaluated
Then its end-to-end latency stays under 1s
And 3DS processing does not degrade standard transaction performance

## Tech Stack and Constraints

- **Runtime Platform:** Java 21 with Spring Boot 3.x, Spring WebFlux, Netty.
- **Communication Protocol:** Internal HTTP/2 (WebClient) between Core Service and 3DS Engine.
- **Session Storage:** Redis Cluster. No relational database access from the 3DS Engine.
- **Authentication Notification:** Asynchronous callback — the 3DS Engine notifies the Core Service when authentication completes.
- **TTL Configurability:** Session TTL, high-value threshold, and risk score threshold must be configurable via application properties.
- **Idempotency Window:** 24 hours for completed 3DS challenges.
- **Session HASH Fields:** `transaction_id`, `merchant_id`, `amount`, `currency`, `card_token`, `acs_url`, `status` (pending|approved|declined|expired), `created_at`, `ttl`.
- **Cryptography:** JWT signed with HS256 by Core Service, verified by 3DS Engine. JWT includes `challenge_id`, `transaction_id`, `merchant_id`, `amount`, `exp`, `iat`.
- **SLA:** 3DS Engine processing must complete within 500ms. The overall transaction SLA (including Mercado Pago communication) stays at <1s for standard flows.