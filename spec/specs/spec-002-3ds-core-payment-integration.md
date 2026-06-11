---
id: spec-002
status: active
links:
  - spec/specs/index.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
  - spec/specs/spec-001-core-payment-processing.md
  - spec/user-stories/us-001-transaction-processing.md
  - spec/user-stories/us-003-transaction-security.md
  - ARCHITECTURE.md
---

# Bidirectional Integration — Core Payment ↔ 3DS Engine

## Context and Primary Objective

The `3ds-engine` is finalized and implements the full challenge flow (Redis session creation, MFA verification, HTTP callback to core), except for the session creation endpoint — which does not exist today. The `core-payment` is under construction and needs both sides designed: how to trigger a 3DS challenge and how to receive and finalize the result.

Primary objective: define the bidirectional contract and component boundaries so both services integrate without shared state coupling or race conditions.

---

## Design Decisions

| Decision | Choice |
|---|---|
| How does the core initiate the challenge? | Synchronous HTTP: `POST /api/v1/3ds/sessions` on the 3DS engine |
| How does the 3DS notify the core? | Existing HTTP callback: `POST /api/v1/payments/3ds-callback` |
| What does the core do with the result? | Persists the status, then emits an internal event via `Sinks.Many` |
| Who finalizes the transaction? | `PaymentFinalizer` — async reactive subscriber |

---

## Full Flow

```
Client
  │
  │ POST /api/v1/payments
  ▼
Core Payment Service
  │
  ├─ [risk check: 3DS required?]
  │       └─ no  → synchronous capture (Mercado Pago directly)
  │       └─ yes ↓
  │
  │ POST /api/v1/3ds/sessions
  │  { transaction_id, merchant_id, amount, currency, card_token }
  ▼
3DS Engine (ThreeDsSessionController → ChallengeSessionService)
  │  generates challengeId (UUID)
  │  builds acsUrl = acsBaseUrl + "/challenge/" + challengeId
  │  persists ChallengeSession in Redis (configurable TTL)
  │  signs JWT with transaction claims
  │  returns { challenge_id, acs_url, jwt }
  │
  ▼
Core Payment Service
  │  saves transaction as PENDING_3DS in the database
  │  returns 202 to client:
  │    { status: "3DS_CHALLENGE_REQUIRED", challenge_id, redirect_url, jwt }
  │
  ─ ─ ─ [user completes challenge on ACS] ─ ─ ─
  │
3DS Engine (AuthVerificationService)
  │  verifies MFA → approved / declined
  │  saves AuthResult in Redis
  │  CallbackNotifier: POST /api/v1/payments/3ds-callback (retry 3x, backoff 1s–5s)
  │    { challenge_id, transaction_id, merchant_id, auth_status, authenticated_at }
  ▼
Core Payment Service (ThreeDsCallbackController)
  │  [idempotency] if status != PENDING_3DS → 200 OK immediately, no reprocessing
  │  persists status → APPROVED_3DS / DECLINED_3DS   ← IO happens HERE
  │  .doOnSuccess → sink.tryEmitNext(ThreeDsCallbackEvent)  ← emits AFTER commit
  │  returns 200 OK to 3DS engine
  │
  ▼
PaymentFinalizer (reactive subscriber, initialized on @PostConstruct)
  │  [if DECLINED] → updates transaction to DECLINED + audit log
  │  [if APPROVED] → calls Mercado Pago → updates to COMPLETED + audit log
```

---

## Data Contracts

### `POST /api/v1/3ds/sessions` — Request (Core → 3DS)

```java
package com.acabouomony.engine.dto;

public record ThreeDsSessionRequest(
        @NotBlank @JsonProperty("transaction_id") String transactionId,
        @NotBlank @JsonProperty("merchant_id")    String merchantId,
        @NotNull  @JsonProperty("amount")          BigDecimal amount,
        @NotBlank @JsonProperty("currency")        String currency,
        @NotBlank @JsonProperty("card_token")      String cardToken
) {}
```

### `POST /api/v1/3ds/sessions` — Response (3DS → Core)

```java
package com.acabouomony.engine.dto;

public record ThreeDsSessionResponse(
        @JsonProperty("challenge_id") String challengeId,
        @JsonProperty("acs_url")      String acsUrl,
        @JsonProperty("jwt")          String jwt
) {}
```

### `POST /api/v1/payments/3ds-callback` — Request (3DS → Core)

Already exists as `com.acabouomony.engine.dto.CallbackRequest`:

```java
public record CallbackRequest(
        @JsonProperty("challenge_id")      String challengeId,
        @JsonProperty("transaction_id")    String transactionId,
        @JsonProperty("merchant_id")       String merchantId,
        @JsonProperty("auth_status")       String authStatus,
        @JsonProperty("authenticated_at")  Instant authenticatedAt
) {}
```

### `ThreeDsCallbackEvent` — Core internal event

```java
package com.acabouomony.payment.domain.event;

public record ThreeDsCallbackEvent(
        String transactionId,
        String challengeId,
        String merchantId,
        String authStatus
) {}
```

---

## New Components

### In `3ds-engine`

#### `ThreeDsSessionController` — pure transport

```java
@PostMapping("/api/v1/3ds/sessions")
Mono<ResponseEntity<ThreeDsSessionResponse>> createSession(
        @Valid @RequestBody ThreeDsSessionRequest request) {
    return sessionService.createSession(request)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
}
```

#### `ChallengeSessionService.createSession` — full orchestration

```java
public Mono<ThreeDsSessionResponse> createSession(ThreeDsSessionRequest request) {
    String challengeId = UUID.randomUUID().toString();
    String acsUrl      = acsBaseUrl + "/challenge/" + challengeId;
    String jwt         = jwtTokenProvider.generateToken(
            challengeId, request.transactionId(), request.merchantId(), request.amount());
    ChallengeSession session = buildSession(request, challengeId, acsUrl);
    return repository.saveSession(challengeId, session)
            .thenReturn(new ThreeDsSessionResponse(challengeId, acsUrl, jwt));
}
```

UUID generation, URL resolution, and JWT signing all live in the service. The controller never touches these rules.

#### `JwtTokenProvider.generateToken` — new public method

```java
public String generateToken(String challengeId, String transactionId,
                            String merchantId, BigDecimal amount) {
    Instant now = Instant.now();
    return Jwts.builder()
            .claim("challenge_id",   challengeId)
            .claim("transaction_id", transactionId)
            .claim("merchant_id",    merchantId)
            .claim("amount",         amount.toPlainString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expirationSeconds)))
            .signWith(key)
            .compact();
}
```

Uses the same `key` and `expirationSeconds` already configured in the existing `verify` method.

### In `core-payment`

#### `ThreeDsClient` — with risk-based fallback

```java
public Mono<ThreeDsSessionResponse> createSession(ThreeDsSessionRequest request, RiskLevel risk) {
    return webClient.post()
            .uri("/api/v1/3ds/sessions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ThreeDsSessionResponse.class)
            .timeout(Duration.ofSeconds(2))
            .onErrorResume(ex -> risk == RiskLevel.LOW
                    ? Mono.empty()   // forced frictionless: skip 3DS, go directly to acquirer
                    : Mono.error(new PaymentAcquirerException("3DS service unavailable")));
}
```

HTTP/2 via `ReactorClientHttpConnector` — same pattern as the 3DS engine's `CallbackNotifier`.

#### `ThreeDsCallbackController` — idempotent, race condition free

```java
@PostMapping("/api/v1/payments/3ds-callback")
Mono<ResponseEntity<Void>> handleCallback(@RequestBody CallbackRequest request) {
    return transactionService.findById(request.transactionId())
            .flatMap(tx -> {
                if (!PaymentStatus.PENDING_3DS.equals(tx.getStatus())) {
                    return Mono.just(ResponseEntity.ok().<Void>build()); // idempotency guard
                }
                var event = new ThreeDsCallbackEvent(
                        request.transactionId(), request.challengeId(),
                        request.merchantId(), request.authStatus());

                PaymentStatus next = "approved".equals(request.authStatus())
                        ? PaymentStatus.APPROVED_3DS
                        : PaymentStatus.DECLINED_3DS;

                return transactionService.updateStatus(request.transactionId(), next)
                        .doOnSuccess(v -> sink.tryEmitNext(event)) // emits AFTER database commit
                        .thenReturn(ResponseEntity.ok().<Void>build());
            });
}
```

The ordering is mandatory: `.doOnSuccess` ensures `PaymentFinalizer` only receives the event after the state is durable in the database, eliminating the race condition where `COMPLETED` would overwrite `APPROVED_3DS`.

#### `PaymentFinalizer` — stream that never dies

```java
@PostConstruct
void startListening() {
    sink.asFlux()
        .flatMap(event -> finalize(event)
            .onErrorResume(err -> {
                log.error("Finalization failed for {}", event.transactionId(), err);
                return Mono.empty(); // isolates the error; stream continues for next events
            }))
        .subscribe();
}

private Mono<Void> finalize(ThreeDsCallbackEvent event) {
    if (!"approved".equals(event.authStatus())) {
        return transactionService.updateStatus(event.transactionId(), PaymentStatus.DECLINED)
                .then(auditLogService.log(AuditEventType.PAYMENT_DECLINED, event.transactionId()));
    }
    return mercadoPagoClient.capture(event.transactionId())
            .flatMap(result -> transactionService.updateStatus(
                    event.transactionId(), PaymentStatus.COMPLETED))
            .then(auditLogService.log(AuditEventType.PAYMENT_COMPLETED, event.transactionId()));
}
```

---

## Error Handling

| Scenario | Behavior |
|---|---|
| 3DS engine down / timeout (LOW risk) | `ThreeDsClient` returns `Mono.empty()` → core executes frictionless capture directly on Mercado Pago |
| 3DS engine down / timeout (HIGH risk) | `ThreeDsClient` propagates `PaymentAcquirerException` → core returns `503` to client |
| Duplicate callback (3DS retry) | `ThreeDsCallbackController` detects `status != PENDING_3DS` → `200 OK` without reprocessing |
| Mercado Pago failure during finalization | `PaymentFinalizer.onErrorResume` absorbs the error, logs it, and continues the stream for subsequent events |
| Race condition: finalization vs callback | Eliminated: `sink.tryEmitNext` only fires inside `.doOnSuccess` after database commit |

---

## Testing Strategy

| Component | Critical scenario | Technique |
|---|---|---|
| `ThreeDsSessionController` | session creation, field validation | `@WebFluxTest` with mocked Redis |
| `ChallengeSessionService.createSession` | unique UUID per call, JWT generated, session persisted | unit test with mocked repository |
| `JwtTokenProvider.generateToken` | correct claims, expiration matches configured TTL | pure unit test |
| `ThreeDsClient` | timeout → frictionless on `LOW`, `503` on `HIGH` | `WireMock` simulating unavailability |
| `ThreeDsCallbackController` | idempotency: second callback with `status=APPROVED_3DS` → `200` without `sink.tryEmitNext` | `@WebFluxTest` with in-memory database |
| `PaymentFinalizer` | stream survives sequential failures (tx-1 fails, tx-2 fails, tx-3 processes) | real component injected with real `Sinks` + Mockito `verify(timeout(1000))` |

### Canonical `PaymentFinalizer` test

```java
@Test
void streamSurvivesSequentialNetworkFailures() {
    Sinks.Many<ThreeDsCallbackEvent> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    PaymentFinalizer finalizer = new PaymentFinalizer(
            sink, mercadoPagoClient, transactionService, auditLogService);
    finalizer.startListening(); // fires the real internal subscription

    when(mercadoPagoClient.capture("tx-1"))
            .thenReturn(Mono.error(new RuntimeException("network timeout")));
    when(mercadoPagoClient.capture("tx-2"))
            .thenReturn(Mono.error(new RuntimeException("network timeout")));
    when(mercadoPagoClient.capture("tx-3"))
            .thenReturn(Mono.just(captureResult));
    when(transactionService.updateStatus("tx-3", PaymentStatus.COMPLETED))
            .thenReturn(Mono.empty());

    sink.tryEmitNext(event("tx-1", "approved"));
    sink.tryEmitNext(event("tx-2", "approved"));
    sink.tryEmitNext(event("tx-3", "approved"));

    // timeout() waits for the Event Loop to process async calls in background
    verify(mercadoPagoClient, timeout(1000).times(3)).capture(any());
    verify(transactionService, timeout(1000)).updateStatus("tx-3", PaymentStatus.COMPLETED);
    verifyNoMoreInteractions(transactionService);
}
```

If `onErrorResume` is missing or misplaced, the Event Loop cancels the subscription after tx-1, tx-3 never reaches Mercado Pago, and the `verify(..., timeout(1000))` assertion fails — empirically proving the stream died.

---

## Impact on Existing Services

| Service | Change | Risk |
|---|---|---|
| `3ds-engine` | New `ThreeDsSessionController` + `createSession` in service + `generateToken` in `JwtTokenProvider` | Low — additive change, no modification to existing flow |
| `core-payment` | New: `ThreeDsClient`, `ThreeDsCallbackController`, `ThreeDsCallbackEvent`, `PaymentFinalizer` | None — all new components |
| Redis | No schema changes (`3ds:session:*`, `3ds:auth:*`) | None |
| Nginx | No routing changes | None |
