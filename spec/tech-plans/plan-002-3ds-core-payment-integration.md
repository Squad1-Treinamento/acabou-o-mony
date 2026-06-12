---
id: plan-002
status: active
links:
  - spec/tech-plans/index.md
  - spec/specs/spec-002-3ds-core-payment-integration.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
  - spec/specs/spec-001-core-payment-processing.md
  - ARCHITECTURE.md
---

# 3DS ↔ Core Payment Integration — Tech Plan

## Architecture Overview and Data Flow

The integration connects two existing services via HTTP. The 3DS Engine gains a session creation endpoint. Core Payment gains a synchronous HTTP client, an idempotent callback handler, and an async Spring Event finalizer.

```
Client (merchant checkout)
         |
         | POST /api/v1/payments
         v
+---------------------+    POST /api/v1/3ds/sessions     +--------------------+
|   Core Payment      | --------------------------------> |    3DS Engine      |
|   Service           |   { txId, merchantId, amount,    |                    |
|   (Spring MVC,      |     currency, cardToken }         | - creates session  |
|   JPA/Hibernate)    | <-------------------------------- |   in Redis         |
|                     |   { challengeId, acsUrl, jwt }   | - signs JWT        |
|  saves CHALLENGE_   |                                   | - returns response |
|  PENDING, returns   |                                   +--------------------+
|  202 + challengeId  |
|  + acsUrl to client |  [user completes challenge on ACS]
|                     |
|                     |    POST /api/v1/payments/3ds-callback
|                     | <-------------------------------- 3DS Engine
|  [idempotency check]|   { challengeId, txId,           (CallbackNotifier,
|  @Transactional     |     authStatus }                  retry 3x)
|  then ApplicationEvent
|  AFTER_COMMIT       |
|                     |
|  @Async             |    POST /capture                  Mercado Pago
|  @TransactionalEvent| -------------------------------->
|  Listener finalizer |
+---------------------+
```

### Data Flow

1. **Risk Evaluation (Core):** `RiskEvaluationService` evaluates the transaction. `LOW` risk skips 3DS and goes straight to Mercado Pago.
2. **Challenge Initiation (Core → 3DS):** If `HIGH`, Core calls `POST /api/v1/3ds/sessions` via `ThreeDsClient` (blocking RestTemplate). Engine creates the Redis session, signs the JWT, returns `{ challengeId, acsUrl, jwt }`. Core transitions to `CHALLENGE_PENDING` and returns `202` to the client with `challengeId` and `acsUrl` in the body.
3. **Challenge Resolution (3DS Engine):** User completes challenge on ACS. `AuthVerificationService` validates MFA and `CallbackNotifier` fires `POST /api/v1/payments/3ds-callback` to Core (retry 3×, backoff 1s–5s).
4. **Callback Handling (Core):** `ThreeDsCallbackController` checks `status == CHALLENGE_PENDING` (idempotency guard). Calls `PaymentOrchestrationService.completeThreeDsAuthentication(txId, approved)` inside a `@Transactional` block. Method transitions to `AUTHENTICATED` or `DECLINED`, publishes a `ThreeDsCompletedEvent` via `ApplicationEventPublisher`. Spring only delivers the event **after** the DB commit (`@TransactionalEventListener(AFTER_COMMIT)`) — this is the blocking equivalent of the `.doOnSuccess` race condition fix.
5. **Async Finalization (Core):** `ThreeDsPaymentFinalizer` listens with `@Async @TransactionalEventListener(AFTER_COMMIT)`. For `approved`: transitions to `PROCESSING`, submits to Mercado Pago, transitions to `COMPLETED/DECLINED/FAILED/UNKNOWN`. For `declined`: audit log only. Runs on a dedicated thread pool — does not block the callback HTTP response.

### Key Finding: Missing Orchestration Wire

`PaymentController` currently persists the transaction but **does not call `PaymentOrchestrationService.processPayment()`**. Wiring this is part of this plan (task-core-00).

### Key Finding: `transitionState` is Private

`PaymentOrchestrationService.transitionState()` is `private`. A new `public` method `completeThreeDsAuthentication(UUID txId, boolean approved)` must be added — it contains the same retry/optimistic-lock logic and publishes the `ThreeDsCompletedEvent` after successful commit.

## Stack and Dependencies

- **Java 21**, Spring Boot 3.x, Spring MVC (Servlet stack)
- **JPA/Hibernate** — blocking, `TransactionRepository extends JpaRepository`
- **RestTemplate** — already a `@Bean` in `AppConfig`; configure read/connect timeouts for `ThreeDsClient`
- **Spring Application Events** — `ApplicationEventPublisher`, `ThreeDsCompletedEvent`, `@TransactionalEventListener(AFTER_COMMIT)`, `@Async`
- **JJWT 0.12.6** — `generateToken` added to existing `JwtTokenProvider` (3DS Engine side)
- **WireMock** — `ThreeDsClient` timeout and fallback tests
- **Mockito** — `ThreeDsPaymentFinalizer` unit tests (mock `PaymentAcquirerClient`)

## Design Patterns and Code Conventions

### Patterns

- **Pure transport controller:** `ThreeDsCallbackController` delegates all logic to `PaymentOrchestrationService`. No state transitions in the controller layer.
- **Race condition prevention (blocking equivalent):** `ApplicationEventPublisher.publishEvent(new ThreeDsCompletedEvent(...))` is called inside `completeThreeDsAuthentication()` — a `@Transactional` method. Spring's `@TransactionalEventListener(AFTER_COMMIT)` guarantees the event fires only after the DB row is durable. This mirrors `.doOnSuccess` from the reactive world.
- **Idempotent callback handler:** `ThreeDsCallbackController` loads the transaction and checks `status == CHALLENGE_PENDING` before delegating. Any other status → `200 OK` with no processing — handles `CallbackNotifier` retries transparently.
- **Resilient async finalizer:** `ThreeDsPaymentFinalizer.onThreeDsCompleted()` is `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`. Failures are caught and logged — the caller's HTTP response is already sent so there is no propagation path.
- **Risk-based fallback:** `ThreeDsClient` on timeout: `LOW` risk → skip 3DS silently (frictionless path); `HIGH` risk → throw `PaymentAcquirerException` (blocks the payment).

### Code Conventions

Follow existing patterns in `PaymentController`, `PaymentOrchestrationService`, `MercadoPagoClient`:
- Controllers use `@RestController`, return `ResponseEntity<T>` (blocking)
- Services are `@Service`, injected via constructor
- JSON: Lombok `@Data @Builder`, `@JsonProperty` snake_case
- `X-API-Key` header required on outbound `ThreeDsClient` calls (matches 3DS Engine `SecurityConfig.apiKeyFilter`)
- Audit via `auditLogService.logStateTransition(...)` on every state transition

## Persistence and Data Modeling

No new database tables. One new DB column and one new DTO field.

| Layer | Change |
|---|---|
| `transactions` table | Add `challenge_id VARCHAR(36)` column (nullable) to correlate callback → transaction |
| `Transaction.java` | Add `private String challengeId;` field with `@Column(name = "challenge_id")` |
| `PaymentResponseDTO` | Add `challenge_id` (String) and `acs_url` (String) fields — returned in 202 response |
| Redis (`3ds:session:*`, `3ds:auth:*`) | No schema change |
| `ThreeDsCompletedEvent` | In-memory Spring event record — no persistence |

## Risks and Dependencies

| Risk | Mitigation |
|---|---|
| `transitionState` is private — callback handler cannot reuse it directly | Add `completeThreeDsAuthentication(UUID txId, boolean approved)` as a `public @Transactional` method on `PaymentOrchestrationService` — same retry logic, ends with `publishEvent` |
| `PaymentController` does not call `processPayment()` | task-core-00 wires the orchestration call after transaction persistence |
| `challenge_id` column not yet in `Transaction` entity | Add field + migration in task-core-01; `ThreeDsClient` stores it via `transactionRepository.save(transaction)` after receiving the session response |
| `ThreeDsClient` timeout blocks request thread | `SimpleClientHttpRequestFactory` with `connectTimeout=2s / readTimeout=5s`; risk-based fallback |
| Duplicate callbacks from `CallbackNotifier` retry | Idempotency guard in controller — status != `CHALLENGE_PENDING` → 200 OK immediately |
| `@Async` finalizer exception not propagated to caller | Failures are caught in `ThreeDsPaymentFinalizer` and logged; UNKNOWN reconciliation covers missed Mercado Pago calls |
| `ChallengeSessionService` constructor change breaks existing 3DS tests | Update `ChallengeSessionServiceTest.setUp()` to pass `JwtTokenProvider` and `acsBaseUrl` |
| `acsUrl` double-segment (`/challenge/challenge/id`) | `3ds.redirect-base-url` already contains `/challenge` — use `acsBaseUrl + "/" + challengeId` |

## Task Breakdown

### 3DS Engine (additive only)

- **task-3ds-01:** Add `JwtTokenProvider.generateToken(challengeId, txId, merchantId, amount)` — no existing method changes; unit-test the signing
- **task-3ds-02:** Create `ThreeDsSessionRequest` / `ThreeDsSessionResponse` records; add `ChallengeSessionService.createSession(ThreeDsSessionRequest)` — update constructor with `JwtTokenProvider` and `acsBaseUrl`; fix affected tests
- **task-3ds-03:** Create `ThreeDsSessionController` `POST /api/v1/3ds/sessions` — pure transport, protected by `apiKeyFilter`, returns 201

### Core Payment (new components)

- **task-core-00:** Wire `PaymentController` → `PaymentOrchestrationService.processPayment()` after transaction persistence; add `PaymentOrchestrationService` to controller constructor injection; adjust 202/200 response logic based on transaction status after orchestration
- **task-core-01:** Add `challengeId` column to `Transaction` entity and DB migration; create `ThreeDsSessionRequestDTO` / `ThreeDsSessionResponseDTO` (client-side); implement `ThreeDsClient` with timeout config and risk fallback; store `challengeId` on transaction after successful session creation; add `acs_url` + `challenge_id` fields to `PaymentResponseDTO`
- **task-core-02:** Create `ThreeDsCompletedEvent` record; add `completeThreeDsAuthentication(UUID txId, boolean approved)` to `PaymentOrchestrationService` — loads transaction, idempotency check, transitions state, publishes event; implement `ThreeDsCallbackController` `POST /api/v1/payments/3ds-callback` — pure transport, delegates to service
- **task-core-03:** Implement `ThreeDsPaymentFinalizer` with `@Async @TransactionalEventListener(AFTER_COMMIT)` — for approved: PROCESSING → Mercado Pago → COMPLETED/DECLINED/FAILED/UNKNOWN; for declined: audit log only; exceptions caught and logged

## Implementation Checklist

- [ ] `JwtTokenProvider.generateToken(challengeId, txId, merchantId, amount)` added and unit-tested
- [ ] `ThreeDsSessionRequest` / `ThreeDsSessionResponse` records created (3DS Engine)
- [ ] `ChallengeSessionService.createSession` implemented — UUID, `acsUrl = acsBaseUrl + "/" + challengeId`, JWT, Redis persist
- [ ] `ThreeDsSessionController` `POST /api/v1/3ds/sessions` created — returns 201, behind `apiKeyFilter`
- [ ] `Transaction.challengeId` field added + DB migration
- [ ] `PaymentResponseDTO` has `challenge_id` and `acs_url` fields
- [ ] `ThreeDsClient` implemented — RestTemplate with timeouts, `X-API-Key` header, risk-based fallback
- [ ] `PaymentController` wired to call `processPayment()` — returns 202 if `CHALLENGE_PENDING`, otherwise final status
- [ ] `ThreeDsCompletedEvent` record created
- [ ] `completeThreeDsAuthentication(UUID txId, boolean approved)` added to `PaymentOrchestrationService` — `@Transactional`, state transition + `publishEvent` inside same transaction
- [ ] `ThreeDsCallbackController` `POST /api/v1/payments/3ds-callback` — idempotency guard, delegates to `completeThreeDsAuthentication`
- [ ] `ThreeDsPaymentFinalizer` implemented — `@Async @TransactionalEventListener(AFTER_COMMIT)`, Mercado Pago call for approved path, exceptions isolated
- [ ] `@EnableAsync` declared (or confirm it already exists in config)
- [ ] All new components covered by unit tests
