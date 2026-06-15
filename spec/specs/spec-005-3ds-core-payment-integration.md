---
id: spec-005
status: active
links:
  - spec/specs/index.md
  - spec/specs/spec-002-3ds-mfa-auth-engine.md
  - spec/specs/spec-001-core-payment-processing.md
  - spec/tech-plans/plan-005-3ds-core-payment-integration.md
  - ARCHITECTURE.md
---

# Bidirectional Integration — Core Payment ↔ 3DS Engine

## Context and Primary Objective

The `3ds-engine` is finalized and implements the full challenge flow (Redis session creation, MFA verification, HTTP callback to core), **except** for the session creation endpoint — which does not yet exist.

The `core-payment` runs on **blocking Spring MVC + JPA/Hibernate** (not WebFlux). Everything on the core side must be designed to fit this stack.

Primary objective: define the bidirectional contract and component boundaries so both services integrate without shared-state coupling or race conditions, using only blocking HTTP and Spring Application Events.

---

## Design Decisions

| Decision | Choice |
|---|---|
| How does core initiate the challenge? | Synchronous HTTP: `POST /api/v1/3ds/sessions` via blocking `RestTemplate` |
| How does 3DS notify core? | Existing `CallbackNotifier` → `POST /api/v1/payments/3ds-callback` (retry 3×, backoff 1s–5s) |
| How does core avoid a race condition between DB commit and finalization? | `@TransactionalEventListener(AFTER_COMMIT)` — Spring only delivers the event after the row is durable |
| Who finalizes the transaction asynchronously? | `ThreeDsPaymentFinalizer` — `@Async @TransactionalEventListener(AFTER_COMMIT)` on a dedicated thread pool |
| What status names are used? | Actual `PaymentStatus` enum values: `CHALLENGE_PENDING`, `AUTHENTICATED`, `DECLINED` |

---

## Full Flow

```
Client
  │
  │ POST /api/v1/payments
  ▼
PaymentController
  │  creates Transaction (CREATED)
  │  calls PaymentOrchestrationService.processPayment(tx)
  │
  ├─ [risk check: HIGH?]
  │       └─ no  → PROCESSING → Mercado Pago → COMPLETED/DECLINED/FAILED
  │       └─ yes ↓
  │
  │  ThreeDsClient.createSession(txId, merchantId, amount, currency, cardToken)
  │  POST /api/v1/3ds/sessions  (RestTemplate, 5s read timeout)
  ▼
3DS Engine (ThreeDsSessionController → ChallengeSessionService)
  │  generates challengeId (UUID)
  │  builds acsUrl = acsBaseUrl + "/" + challengeId
  │    (acsBaseUrl already contains /challenge, e.g. http://localhost:8081/challenge)
  │  persists ChallengeSession in Redis (configurable TTL)
  │  signs JWT with transaction claims (challengeId, txId, merchantId, amount)
  │  returns { challenge_id, acs_url, jwt }
  │
  ▼
PaymentOrchestrationService (back in processPayment)
  │  stores challengeId on transaction
  │  transitions VALIDATED → CHALLENGE_PENDING (via private transitionState)
  │  returns (void) — controller responds 202 with { challenge_id, acs_url }
  │
  ─ ─ ─ [user completes challenge on ACS] ─ ─ ─
  │
3DS Engine (AuthVerificationService)
  │  validates MFA → approved / declined
  │  saves AuthResult in Redis
  │  CallbackNotifier: POST /api/v1/payments/3ds-callback (retry 3×, backoff 1s–5s)
  │    { challenge_id, transaction_id, merchant_id, auth_status, authenticated_at }
  ▼
ThreeDsCallbackController
  │  delegates to PaymentOrchestrationService.completeThreeDsAuthentication(txId, approved)
  │  returns 200 OK immediately (CallbackNotifier does not wait for finalization)
  │
PaymentOrchestrationService.completeThreeDsAuthentication  [@Transactional]
  │  loads transaction by txId
  │  [idempotency guard] if status != CHALLENGE_PENDING → return immediately
  │  transitions → AUTHENTICATED (approved) or DECLINED (declined)
  │  publishes ThreeDsCompletedEvent via ApplicationEventPublisher
  │    ↑ Spring holds event delivery until AFTER DB commit
  │
ThreeDsPaymentFinalizer  [@Async @TransactionalEventListener(AFTER_COMMIT)]
  │  [if DECLINED] → audit log only
  │  [if AUTHENTICATED] → calls PaymentOrchestrationService.resumePaymentAfterAuth(txId)
  │      AUTHENTICATED → PROCESSING → Mercado Pago → COMPLETED / DECLINED / FAILED / UNKNOWN
```

---

## Data Contracts

### `POST /api/v1/3ds/sessions` — Request (Core → 3DS Engine)

```java
// com.acabouomony.engine.dto
public record ThreeDsSessionRequest(
        @NotBlank @JsonProperty("transaction_id") String transactionId,
        @NotBlank @JsonProperty("merchant_id")    String merchantId,
        @NotNull  @JsonProperty("amount")          long amount,      // cents, matches Transaction.amount
        @NotBlank @JsonProperty("currency")        String currency,
        @NotBlank @JsonProperty("card_token")      String cardToken
) {}
```

### `POST /api/v1/3ds/sessions` — Response (3DS Engine → Core)

```java
// com.acabouomony.engine.dto
public record ThreeDsSessionResponse(
        @JsonProperty("challenge_id") String challengeId,
        @JsonProperty("acs_url")      String acsUrl,
        @JsonProperty("jwt")          String jwt
) {}
```

### `POST /api/v1/payments/3ds-callback` — Request (3DS Engine → Core)

Already exists as `com.acabouomony.engine.dto.CallbackRequest`:

```java
public record CallbackRequest(
        @JsonProperty("challenge_id")      String challengeId,
        @JsonProperty("transaction_id")    String transactionId,
        @JsonProperty("merchant_id")       String merchantId,
        @JsonProperty("auth_status")       String authStatus,       // "approved" | "declined"
        @JsonProperty("authenticated_at")  Instant authenticatedAt
) {}
```

### `ThreeDsCompletedEvent` — Core internal Spring event

```java
// com.acabouomony.payment.domain.event
public record ThreeDsCompletedEvent(UUID txId, boolean approved) {}
```

### `PaymentResponseDTO` additions — for 202 CHALLENGE_PENDING response

```java
// Added to existing PaymentResponseDTO
@JsonProperty("challenge_id")  private String challengeId;
@JsonProperty("acs_url")       private String acsUrl;
```

---

## New Components

### In `3ds-engine` (additive only)

#### `JwtTokenProvider.generateToken` — new public method

```java
public String generateToken(String challengeId, String transactionId,
                            String merchantId, long amount) {
    Instant now = Instant.now();
    return Jwts.builder()
            .claim("challenge_id",   challengeId)
            .claim("transaction_id", transactionId)
            .claim("merchant_id",    merchantId)
            .claim("amount",         amount)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expirationSeconds)))
            .signWith(key)
            .compact();
}
```

Uses the same `key` and `expirationSeconds` already configured for the `verify` method.

#### `ChallengeSessionService.createSession` — new public method

```java
public ThreeDsSessionResponse createSession(ThreeDsSessionRequest request) {
    String challengeId = UUID.randomUUID().toString();
    String acsUrl      = acsBaseUrl + "/" + challengeId;  // acsBaseUrl already ends with /challenge
    String jwt         = jwtTokenProvider.generateToken(
            challengeId, request.transactionId(), request.merchantId(), request.amount());
    ChallengeSession session = buildSession(request, challengeId, acsUrl);
    repository.saveSession(challengeId, session).block();  // or use blocking subscribe
    return new ThreeDsSessionResponse(challengeId, acsUrl, jwt);
}
```

Constructor gains two new parameters: `JwtTokenProvider jwtTokenProvider` and `@Value("${3ds.redirect-base-url}") String acsBaseUrl`.

#### `ThreeDsSessionController` — pure transport, API-key protected

```
POST /api/v1/3ds/sessions
Authorization: X-API-Key: <key>
→ 201 Created + ThreeDsSessionResponse body
```

The controller never generates UUIDs, builds URLs, or signs tokens — all logic is in `ChallengeSessionService`.

---

### In `core-payment` (all new)

#### `Transaction.challengeId` — new field

```java
// Added to Transaction entity
@Column(name = "challenge_id", length = 36)
private String challengeId;
```

Required DB migration: `ALTER TABLE transactions ADD COLUMN challenge_id VARCHAR(36)`.

#### `ThreeDsClient` — synchronous HTTP with timeout and risk fallback

```java
// com.acabouomony.payment.infrastructure.client
@Service
public class ThreeDsClient {
    // Injected: RestTemplate (with 2s connect / 5s read timeout), baseUrl, apiKey, riskLevel

    public Optional<ThreeDsSessionResponseDTO> createSession(
            ThreeDsSessionRequestDTO request, RiskLevel risk) {
        try {
            return Optional.of(restTemplate.exchange(
                    url, HttpMethod.POST, entity, ThreeDsSessionResponseDTO.class).getBody());
        } catch (ResourceAccessException e) {    // timeout / connection refused
            if (risk == RiskLevel.LOW) return Optional.empty();  // frictionless fallback
            throw new PaymentAcquirerException("3DS service unavailable");
        }
    }
}
```

`RestTemplate` is configured with `SimpleClientHttpRequestFactory` (connect=2s, read=5s) in `AppConfig`. The `X-API-Key` header is added to every request.

#### `PaymentOrchestrationService` additions

Three new public methods. `transitionState` remains private and unchanged.

```java
// 1. Called by PaymentController after transaction is persisted
//    HIGH-risk branch: calls ThreeDsClient, stores challengeId, transitions to CHALLENGE_PENDING
public void processPayment(Transaction transaction) { /* already exists; wire ThreeDsClient here */ }

// 2. Called by ThreeDsCallbackController (blocking, @Transactional)
@Transactional
public void completeThreeDsAuthentication(UUID txId, boolean approved) {
    Transaction tx = transactionRepository.findById(txId).orElseThrow(...);
    if (tx.getStatus() != PaymentStatus.CHALLENGE_PENDING) return; // idempotency guard
    PaymentStatus next = approved ? PaymentStatus.AUTHENTICATED : PaymentStatus.DECLINED;
    transitionState(tx, next, "3ds-engine");
    // Event is published inside the transaction but delivered AFTER commit:
    eventPublisher.publishEvent(new ThreeDsCompletedEvent(txId, approved));
}

// 3. Called by ThreeDsPaymentFinalizer for the approved path
@Transactional
public void resumePaymentAfterAuth(UUID txId) {
    Transaction tx = transactionRepository.findById(txId).orElseThrow(...);
    transitionState(tx, PaymentStatus.PROCESSING, "system");
    PaymentResult result = paymentAcquirerClient.submitPayment(tx);
    handlePaymentResult(tx, result); // existing private method
}
```

#### `ThreeDsCallbackController` — idempotent, blocking

```java
@RestController
public class ThreeDsCallbackController {

    @PostMapping("/api/v1/payments/3ds-callback")
    public ResponseEntity<Void> handleCallback(@RequestBody CallbackRequest request) {
        orchestrationService.completeThreeDsAuthentication(
            UUID.fromString(request.transactionId()),
            "approved".equals(request.authStatus())
        );
        return ResponseEntity.ok().build();
    }
}
```

The idempotency guard lives inside `completeThreeDsAuthentication` — the controller is a pure transport layer.

#### `ThreeDsPaymentFinalizer` — async Spring Event listener

```java
@Component
public class ThreeDsPaymentFinalizer {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onThreeDsCompleted(ThreeDsCompletedEvent event) {
        if (!event.approved()) {
            auditLogService.logStateTransition(..., DECLINED, ...);
            return;
        }
        try {
            orchestrationService.resumePaymentAfterAuth(event.txId());
        } catch (Exception e) {
            log.error("Finalization failed for txId={}", event.txId(), e);
            // UNKNOWN reconciliation covers missed Mercado Pago calls
        }
    }
}
```

`@EnableAsync` must be present on a configuration class. Exceptions are caught and logged — the callback HTTP response was already sent, so there is no propagation path.

#### `PaymentController` wiring

Currently `PaymentController` persists the transaction but never calls `processPayment()`. Wiring:

```java
// After: Transaction saved = transactionRepository.save(transaction);
orchestrationService.processPayment(saved);

// Build 202 response if CHALLENGE_PENDING:
if (saved.getStatus() == PaymentStatus.CHALLENGE_PENDING) {
    return ResponseEntity.accepted().body(
        PaymentResponseDTO.builder()
            .transactionId(saved.getId())
            .status(saved.getStatus())
            .challengeId(saved.getChallengeId())
            .acsUrl(/* returned from ThreeDsClient and stored on tx */)
            .build());
}
// Otherwise build final response as before
```

---

## Error Handling

| Scenario | Behavior |
|---|---|
| 3DS engine down / timeout — `LOW` risk | `ThreeDsClient` returns `Optional.empty()` → core skips 3DS, goes directly to Mercado Pago |
| 3DS engine down / timeout — `HIGH` risk | `ThreeDsClient` throws `PaymentAcquirerException` → controller returns 503 to client |
| Duplicate callback (3DS Engine retry) | `completeThreeDsAuthentication` detects `status != CHALLENGE_PENDING` → returns immediately; controller always returns 200 OK |
| Mercado Pago failure during finalization | `ThreeDsPaymentFinalizer` catches exception, logs it; transaction stays in `AUTHENTICATED` or transitions to `UNKNOWN` via existing `unknownStateTransitionHandler` |
| Race condition: finalization reads stale state | Eliminated: `@TransactionalEventListener(AFTER_COMMIT)` guarantees `AUTHENTICATED` row is durable before `ThreeDsPaymentFinalizer` runs |
| `@Async` thread pool exhausted | Finalization delayed; callback still returns 200; UNKNOWN reconciliation picks up stuck transactions |

---

## Testing Strategy

| Component | Critical scenario | Technique |
|---|---|---|
| `JwtTokenProvider.generateToken` | Correct claims, expiration matches configured TTL, `amount` stored as `long` | Pure unit test |
| `ChallengeSessionService.createSession` | Unique UUID per call, `acsUrl = acsBaseUrl + "/" + challengeId` (no double segment), JWT generated, session persisted in Redis | Unit test with mocked repository |
| `ThreeDsSessionController` | Session creation → 201; missing required fields → 400; no API key → 401 | `@WebFluxTest` (3DS Engine is WebFlux) with mocked service |
| `ThreeDsClient` | Timeout + LOW risk → `Optional.empty()`; timeout + HIGH risk → `PaymentAcquirerException` | WireMock with delayed response |
| `ThreeDsCallbackController` | Valid callback → 200; second callback with same txId (status = AUTHENTICATED) → 200 with no duplicate transition | `@WebMvcTest` with mocked `PaymentOrchestrationService` |
| `PaymentOrchestrationService.completeThreeDsAuthentication` | First call → AUTHENTICATED + event published; second call → no-op (idempotency) | Unit test with mocked `TransactionRepository` and `ApplicationEventPublisher` |
| `ThreeDsPaymentFinalizer` | Event fires only after DB commit; Mercado Pago failure is caught; declined path skips acquirer call | Unit test with `@RecordApplicationEvents` or `ApplicationEventPublisher` + Mockito `verify` |

---

## Impact on Existing Services

| Service | Change | Risk |
|---|---|---|
| `3ds-engine` | New `ThreeDsSessionController`; `ChallengeSessionService.createSession` + constructor change; `JwtTokenProvider.generateToken` | Low — fully additive, existing flow unchanged |
| `core-payment` | Wire `PaymentController` → `processPayment()`; new `ThreeDsClient`, `ThreeDsCallbackController`, `ThreeDsPaymentFinalizer`, `ThreeDsCompletedEvent`; two new public methods on `PaymentOrchestrationService`; new `challengeId` column | Medium — `PaymentController` and `PaymentOrchestrationService` are modified |
| `transactions` DB table | New nullable column `challenge_id VARCHAR(36)` | Low — nullable, no backfill needed |
| Redis | No schema changes | None |
| Nginx | No routing changes | None |
