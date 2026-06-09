---
id: plan-001
status: active
links:
  - spec/tech-plans/index.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
  - spec/user-stories/us-001-transaction-processing.md
  - spec/user-stories/us-003-transaction-security.md
  - ARCHITECTURE.md
---

# 3DS / MFA Auth Engine — Tech Plan

## Architecture Overview and Data Flow

The 3DS / MFA Auth Engine runs as a separate microservice with no direct database access. It shares the Redis Cluster with the Core Payment Processing Service and communicates back via internal HTTP/2 callbacks.

```
Merchant / Cardholder Bank
        |
        | 3DS challenge result (via redirect)
        v
+-----------------------+    Redis Protocol     +-------------------+
|   3DS / MFA Auth      | <-------------------> |   Redis Cluster   |
|       Engine          |                       | (session state,   |
| (Spring WebFlux,      |                       |  auth results)    |
|  Java 21, Netty)      |                       +-------------------+
+----------+------------+                             ^
           |                                          |
           | Internal HTTP/2 callback                 | Shared Redis
           | (async, non-blocking)                    |
           v                                          |
+----------------------+   Redis Protocol             |
|   Core Payment       | -----------------------------+
|   Processing Service |
| (Spring WebFlux,     |
|  Java 21, Netty)     |
+----------------------+
```

### Data Flow

1. **Risk Evaluation (Core Service):** Core evaluates risk using fraud parameters in Redis. If low-risk, skip 3DS.
2. **Challenge Initiation (Core Service):** If high-risk/high-value/first-time, Core resolves ACS URL (BIN lookup), writes session to Redis (key `3ds:session:{challenge_id}` including `acs_url`), signs JWT, and returns `3DS_CHALLENGE_REQUIRED` with landing page URL + JWT query param.
3. **Landing Page Redirect (3DS Engine):** Cardholder's browser hits `GET /challenge/{challenge_id}?jwt=<token>`. Engine verifies JWT, reads session from Redis, extracts `acs_url`, and HTTP-redirects to the bank's ACS.
4. **Challenge Processing (3DS Engine):** Bank authenticates cardholder, then POSTs MFA result to `POST /api/v1/3ds/verify` (via Nginx reverse proxy). Engine reads session from Redis, validates MFA token (session exists + token non-empty), writes auth result to Redis (key `3ds:auth:{challenge_id}`).
5. **Async Notification (3DS Engine -> Core):** Engine POSTs to Core's `/api/v1/payments/3ds-callback` with the auth result.
6. **Ledger Finalization (Core Service):** Core reads auth result from Redis, finalizes ledger, submits to Mercado Pago API.

## Stack and Dependencies

- **Java 21** (LTS, virtual threads support available but optional given WebFlux)
- **Spring Boot 3.x** with **Spring WebFlux** and **Netty**
- **Spring Data Redis Reactive** (`spring-boot-starter-data-redis-reactive`)
- **Spring Security** for endpoint protection (API Key / JWT validation)
- **jjwt** (or `spring-security-oauth2-jose`) for JWT signing and verification (HS256/RS256)
- **Project Reactor** (`Mono`/`Flux`) for async composition
- **Redis Cluster** — shared with Core Service, separate key namespace (`3ds:*`)
- **Test containers** for Redis integration tests
- **No relational database dependency** in the 3DS Engine
- **No Mercado Pago dependency** in the 3DS Engine

## Design Patterns and Code Conventions

### Patterns

- **Reactive Controller-Service-Repository:** Standard WebFlux layers. Controllers return `Mono<ResponseEntity>`. Services return `Mono<T>`. Redis operations via `ReactiveRedisTemplate`.
- **Saga-like Async Callback:** 3DS Engine completes its local write to Redis, then fires an async HTTP callback to Core. If the callback fails, Core can poll Redis as fallback.
- **Idempotency Key:** Each challenge carries `challenge_id`. Before processing, 3DS Engine checks Redis for existing auth result by `challenge_id`. If found, returns cached result.
- **Configuration-Driven Tuning:** Risk thresholds, TTLs, JWT secret, and callback URL come from `application.yml` / environment variables.

### Code Conventions

- Use `@Component` for Redis client wrappers; avoid raw `ReactiveRedisTemplate` in controllers.
- JWT utility class as a `@Component` with `verify()` only (signing is done by Core Service).
- Custom `3dsException` with `CHALLENGE_EXPIRED`, `INVALID_TOKEN`, `DUPLICATE_CHALLENGE` variants.
- Global `@ControllerAdvice` for standardized error JSON.
- Log `challenge_id` and `transaction_id` in every operation for traceability.

## Persistence and Data Modeling

The 3DS Engine uses Redis exclusively. No SQL tables.

### Redis Key Conventions

| Key Pattern | Type | TTL | Purpose |
|---|---|---|---|
| `3ds:session:{challenge_id}` | HASH | 600s (configurable) | Challenge session with transaction context, status, timestamps |
| `3ds:auth:{challenge_id}` | STRING | 86400s (24h) | Authentication result ("approved" \| "declined") for idempotency |
| `3ds:counter:{merchant_id}` | STRING | none (persistent) | Optional: transaction counter for first-time card detection |

### Session HASH Fields

`transaction_id`, `merchant_id`, `amount`, `currency`, `card_token`, `acs_url` (bank's ACS URL, resolved by Core), `status` (pending|approved|declined|expired), `created_at`, `ttl`.

## Non-Functional Requirements and Security

- **SLA:** 3DS Engine processing under 500ms p95. Async callback must not block the challenge response.
- **Isolation:** 3DS Engine never accesses the relational database. All state lives in Redis.
- **JWT Security:** JWT signed with HS256 (or RS256 for multi-service verification). Include `exp`, `iat`, `challenge_id`, `transaction_id`, `merchant_id`, `amount`. Validate expiry on every 3DS callback.
- **Redis Security:** Use dedicated Redis key namespace `3ds:*`. No PII stored in Redis values.
- **Audit:** Every state transition (challenge created, auth approved, auth declined, expired) is logged with structured JSON, including `challenge_id` and `transaction_id`.
- **TTL Enforcement:** Redis auto-evicts expired sessions. The 3DS Engine also checks `status` field before processing to handle edge cases where TTL has not yet fired.
- **Rate Limiting:** 3DS Engine endpoints should accept a configurable max requests per second per merchant.

## Task Breakdown Preview

- task-001: Create Spring Boot WebFlux project scaffold for 3DS Engine — planned
- task-002: Implement Redis session repository with reactive CRUD operations (includes `acs_url` field) — planned
- task-003: Implement JWT verification utility (verify-only; Core signs) — planned
- task-004: Implement 3DS landing page (`GET /challenge/{challenge_id}?jwt=`) — planned
- task-005: Implement MFA token verification and auth result persistence — planned
- task-006: Implement async HTTP/2 callback to Core Service — planned
- task-007: Implement idempotency check (deduplicate by `challenge_id`) — planned
- task-008: Add session expiry handling and `3DS_CHALLENGE_EXPIRED` error flow — planned
- task-009: Integration tests with Testcontainers Redis — planned
- task-010: Configuration documentation (`application.yml` with all 3DS properties) — planned
- task-011: Add structured audit logging for all state transitions — planned

> Note: Risk evaluation in Core Service is out of scope for these 3DS Engine tasks — it belongs to Core Service implementation.

## Implementation Checklist

- [ ] Generate Spring Boot project with WebFlux, Redis Reactive, and Security starters
- [ ] Configure `application.yml` with Redis cluster connection, JWT secret, TTLs, thresholds
- [ ] Implement `ChallengeSessionRepository` (ReactiveRedisTemplate wrapper, `acs_url` field)
- [ ] Implement `JwtTokenProvider` (verify-only — HS256, decode claims)
- [ ] Implement `LandingPageController` (GET `/challenge/{id}` with JWT verification + ACS redirect)
- [ ] Implement `AuthVerificationService` (validate MFA token — session exists + non-empty token)
- [ ] Implement `CallbackNotifier` (async WebClient POST to Core Service)
- [ ] Implement idempotency check (`3ds:auth:{challenge_id}` before processing)
- [ ] Implement session expiry handling (status + `created_at+ttl` check)
- [ ] Implement `LandingPageController` verify endpoint (POST `/api/v1/3ds/verify`)
- [ ] Implement global error handler (`@ControllerAdvice`)
- [ ] Write integration tests (Redis + WebTestClient)
- [ ] Add structured audit logging for all state transitions
- [ ] Document all 3DS configuration properties

> Risk evaluation in Core Service is out of scope for this checklist.

## Examples

### Challenge Initiation Request (Core -> Merchant)

```
POST /api/v1/payments
Authorization: Bearer <merchant-jwt>
Content-Type: application/json

{
  "amount": 1500.00,
  "currency": "BRL",
  "card_token": "tok_xyz",
  "merchant_id": "m_456"
}

Response: HTTP 200
{
  "status": "3DS_CHALLENGE_REQUIRED",
  "redirect_url": "https://3ds.acabouomony.com/challenge/ch_001?jwt=eyJhbGciOiJIUzI1NiIs...",
  "jwt_token": "eyJhbGciOiJIUzI1NiIs...",
  "expires_in_seconds": 600
}
```

### Landing Page Redirect (Cardholder Browser -> 3DS Engine)

```
GET /challenge/ch_001?jwt=eyJhbGciOiJIUzI1NiIs...

Response: HTTP 302
Location: https://acs.bank.com/3ds-auth?challenge_id=ch_001&acs_ref=...
```

### MFA Verification Request (Cardholder Bank -> 3DS Engine)

```
POST /api/v1/3ds/verify
Content-Type: application/json

{
  "challenge_id": "ch_001",
  "mfa_token": "mfa_token_value_from_bank"
}

Response: HTTP 200
{
  "status": "approved",
  "challenge_id": "ch_001",
  "transaction_id": "tx_002"
}
```

### Async Callback (3DS Engine -> Core Service)

```
POST /api/v1/payments/3ds-callback
Content-Type: application/json

{
  "challenge_id": "ch_001",
  "transaction_id": "tx_002",
  "auth_status": "approved",
  "authenticated_at": "2026-06-01T12:00:00Z"
}
```
