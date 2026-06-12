# Tasks Index

Use this index to find executable tasks. Tasks live in subdirectories
grouped by feature branch (`tasks/<branch>/`).

## Core Payment Processing Tasks

### Completed Tasks

- **task-001-database-schema-jpa.md** — Database schema and JPA entities
  - Status: ✅ COMPLETED
  - Components: Transaction, AuditLog, OutboxEvent entities; Flyway migrations; 100+ integration tests
  - Coverage: Schema creation, constraints, optimistic locking, enum validation

- **task-002-payment-state-machine.md** — Payment lifecycle state machine with validation
  - Status: ✅ COMPLETED
  - Components: PaymentStatus enum, StateTransition value object, PaymentStateMachine service
  - Coverage: 23 allowed transitions, invalid transition rejection, state validation

- **task-003-optimistic-locking.md** — Optimistic locking and version management
  - Status: ✅ COMPLETED
  - Components: TransactionVersionService, OptimisticLockRetryHandler, version conflict detection
  - Coverage: Version increment, retry logic, concurrent update handling

- **task-004-merchant-authentication.md** — Merchant authentication and API key validation
  - Status: ✅ COMPLETED
  - Components: Merchant entity, MerchantRepository, MerchantAuthService, ApiKeyAuthenticationFilter
  - Coverage: API key hashing, timing-safe comparison, authentication flow

- **task-005-payment-request-validation.md** — Payment request validation
  - Status: ✅ COMPLETED
  - Components: PaymentRequest DTO, PaymentRequestValidator, validation annotations
  - Coverage: Amount, currency, idempotency key, payment method validation; 45+ tests

- **task-006-card-validation.md** — Card validation (Luhn check, expiry, CVV)
  - Status: ✅ COMPLETED
  - Components: CardType enum, CardValidator service, CardValidationException
  - Coverage: Luhn check, card type detection, expiry validation, CVV validation; 40+ tests

- **task-007-payload-hashing.md** — Payload hashing for idempotency
  - Status: ✅ COMPLETED
  - Components: PayloadHashingService, IdempotencyService, deterministic SHA-256 hashing
  - Coverage: Hash computation, duplicate detection, payload validation; 31+ tests

- **task-009-db-idempotency-enforcement.md** — Database-level idempotency enforcement
  - Status: ✅ COMPLETED
  - Components: DuplicatePaymentHandler, PaymentResponseDTO, UNIQUE constraint enforcement
  - Coverage: Duplicate detection, fallback recovery, constraint violation handling; 25+ tests

- **task-010-duplicate-request-recovery.md** — Duplicate request recovery and response caching
  - Status: ✅ COMPLETED
  - Components: PaymentResponseCache, InMemoryPaymentResponseCache, DuplicateRequestRecoveryService
  - Coverage: Response caching, TTL expiration, mismatched payload handling; 30+ tests

- **task-011-mercado-pago-client.md** — Mercado Pago integration client
  - Status: ✅ COMPLETED
  - Components: PaymentAcquirerClient interface, MercadoPagoClient implementation, timeout handling
  - Coverage: Payment submission, status query, error mapping, retry logic; 8+ tests

- **task-012-payment-orchestration.md** — Payment processing orchestration
  - Status: ✅ COMPLETED
  - Components: PaymentOrchestrationService, StateTransitionValidator, RiskEvaluationService, AuditLogService, OutboxEventService
  - Coverage: Complete payment flow, state transitions, risk evaluation, audit logging; 25+ tests

- **task-013-unknown-state-handling.md** — UNKNOWN state handling with reconciliation infrastructure
  - Status: ✅ COMPLETED
  - Components: UnknownStateTransitionHandler, AlertService, UnknownStateProperties, configuration
  - Coverage: Timeout handling, acquirer errors, alerts, configuration properties

- **task-014-reconciliation-worker.md** — Reconciliation worker with polling and concurrency control
  - Status: ✅ COMPLETED
  - Components: ReconciliationWorker, ReconciliationService, ReconciliationConcurrencyManager, StaleUnknownMonitor
  - Coverage: Polling, concurrency limits, queue management, stale detection; 50+ unit tests, 14 integration tests

- **task-015-reconciliation-logic.md** — Reconciliation logic with atomic state transitions
  - Status: ✅ COMPLETED
  - Components: ReconciliationStateTransitionHandler, status mapping, audit logging, outbox events
  - Coverage: Atomic transactions, optimistic locking, idempotent reconciliation; 6 unit tests, 14 integration tests

### In Progress Tasks

- **task-016-outbox-persistence.md** — Outbox pattern persistence
  - Status: 🔄 IN PROGRESS
  - Description: Transactional outbox persistence for payment outcomes and webhook workflow

### Not Started Tasks

- **task-017-webhook-dispatch-worker.md** — Webhook dispatch worker
  - Status: ⏳ NOT STARTED
  - Description: Background worker for reliable delivery and retry of outgoing webhooks

- **task-018-risk-evaluation.md** — Risk evaluation
  - Status: ⏳ NOT STARTED
  - Description: Merchant and transaction risk evaluation with configurable rules

- **task-019-structured-audit-logging.md** — Structured audit logging
  - Status: ⏳ NOT STARTED
  - Description: Structured audit logging with masking and tamper-detection checksums

## Cache e Idempotency Layer com Redis Tasks

Tasks in `spec/tasks/cache-idempotency-redis/`:

- **task-001-add-cache-maven-dependencies.md** — Add cache Maven dependencies to Core Service
- **task-002-configure-redis-application-yml.md** — Configure Redis connection in application.yml
- **task-003-implement-redis-config.md** — Implement RedisConfig + RedisPaymentResponseCache
- **task-004-implement-merchant-service-cache.md** — MerchantService cache (fase futura, placeholder)
- **task-005-implement-risk-and-bin-cache.md** — Risk and BIN cache (fase futura, placeholder)
- **task-006-implement-circuit-breaker.md** — Circuit breaker configuration via Resilience4j
- **task-007-implement-idempotency-service.md** — Add Redis fast-path to IdempotencyService
- **task-008-implement-idempotency-interceptor.md** — Wire Redis cache in DuplicateRequestRecoveryService
- **task-009-apply-idempotency-to-endpoints.md** — Apply idempotency interceptor to endpoints
- **task-010-create-integration-tests-cache.md** — Integration tests for cache layer (Testcontainers)
- **task-011-create-integration-tests-idempotency.md** — Integration tests for idempotency layer
- **task-013-implement-aes-encryptor.md** — AESEncryptor infrastructure (preparação, não usado no MVP)
- **task-014-configure-encryption-validation.md** — Encryption key validation on startup
- **task-015-document-cache-encryption.md** — Cache encryption documentation

**Note:** task-012 (Bouncy Castle dependency) foi removido — `bcprov-jdk18on` já está no `pom.xml`.

## Auth Engine Core Tasks

## 3DS ↔ Core Payment Integration Tasks

### 3DS Engine (Additive)

- **task-3ds-01-jwt-generate-token.md** — Add `JwtTokenProvider.generateToken` method
  - Status: ⏳ NOT STARTED
  - Components: `generateToken(challengeId, txId, merchantId, amount)` using existing key and expirationSeconds
  - Depends on: nothing

- **task-3ds-02-session-request-dtos-and-create-session.md** — ThreeDsSession DTOs and `ChallengeSessionService.createSession`
  - Status: ⏳ NOT STARTED
  - Components: `ThreeDsSessionRequest`, `ThreeDsSessionResponse` records; `createSession` method; constructor update
  - Depends on: task-3ds-01

- **task-3ds-03-session-controller.md** — Create `ThreeDsSessionController` `POST /api/v1/3ds/sessions`
  - Status: ⏳ NOT STARTED
  - Components: `ThreeDsSessionController` (pure transport, 201 Created, API-key protected)
  - Depends on: task-3ds-02

### Core Payment (New Components)

- **task-core-00-wire-payment-controller.md** — Wire `PaymentController` to call `processPayment()`
  - Status: ⏳ NOT STARTED
  - Components: `PaymentOrchestrationService` injection, conditional 202 for `CHALLENGE_PENDING`
  - Depends on: nothing (parallelize with 3DS Engine tasks)

- **task-core-01-three-ds-client.md** — `ThreeDsClient`, `challengeId` column, `PaymentResponseDTO` update
  - Status: ⏳ NOT STARTED
  - Components: `ThreeDsClient` (RestTemplate, risk fallback), Flyway migration V4, `Transaction.challengeId/challengeAcsUrl`, `PaymentResponseDTO` fields
  - Depends on: task-3ds-03, task-core-00

- **task-core-02-callback-handler.md** — `ThreeDsCompletedEvent`, callback handler, orchestration methods
  - Status: ⏳ NOT STARTED
  - Components: `ThreeDsCompletedEvent`, `ThreeDsCallbackController`, `completeThreeDsAuthentication()`, `resumePaymentAfterAuth()`
  - Depends on: task-core-01

- **task-core-03-payment-finalizer.md** — `ThreeDsPaymentFinalizer` and `@EnableAsync`
  - Status: ⏳ NOT STARTED
  - Components: `ThreeDsPaymentFinalizer` (`@Async @TransactionalEventListener(AFTER_COMMIT)`)
  - Depends on: task-core-02
