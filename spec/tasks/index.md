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

- **task-008-redis-idempotency-coordination.md** — Redis-based idempotency coordination
  - Status: ✅ COMPLETED
  - Components: RedisPaymentResponseCache, PaymentResponseCache interface, RedisTemplate integration
  - Coverage: Cache storage/retrieval, TTL handling, cache invalidation, merchant-scoped keys; 11 unit tests

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

### Completed Tasks (continued)

- **task-016-outbox-persistence.md** — Outbox pattern persistence
  - Status: ✅ COMPLETED
  - Components: OutboxEvent entity, OutboxEventService, atomic persistence with transaction updates
  - Coverage: Atomic persistence, transaction rollback, status tracking, independent versioning; 65 tests

- **task-017-webhook-dispatch-worker.md** — Webhook dispatch worker
  - Status: ✅ COMPLETED
  - Components: WebhookDispatchWorker, WebhookDispatchService, WebhookSignatureService, HMAC-SHA256 signatures
  - Coverage: Polling dispatch, exponential backoff (1s-16s), 5 retries, SLA monitoring, virtual threads; 25 tests

- **task-018-risk-evaluation.md** — Risk evaluation
  - Status: ✅ COMPLETED
  - Components: RiskLevel enum, RiskRule interface, AmountRiskRule, NewCardRiskRule, VelocityRiskRule, RiskEvaluationService
  - Coverage: Pluggable rules, OR logic, configurable thresholds, fail-safe defaults, runtime rule management; 20 tests

- **task-019-structured-audit-logging.md** — Structured audit logging
  - Status: ✅ COMPLETED
  - Components: AuditEventType enum, DataMaskingService, StructuredAuditEvent, StructuredAuditLogger, SHA256 checksums
  - Coverage: 56 event types, PII masking (card, API key, email, phone, document), tamper detection, immutable logs; 30 tests

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

- **task-012-add-encryption-maven-dependency.md** � Add encryption Maven dependencies (Bouncy Castle)

## Auth Engine Core Tasks

Tasks in `spec/tasks/auth-engine-core/`:

- **task-001-scaffold-spring-boot-project.md** — Scaffold Spring Boot project ✅ COMPLETED
- **task-002-redis-session-repository.md** — Redis session repository ✅ COMPLETED
- **task-003-jwt-utility.md** — JWT utility ✅ COMPLETED
- **task-004-challenge-initiation-endpoint.md** — Challenge initiation endpoint ✅ COMPLETED
- **task-005-mfa-verification.md** — MFA verification ✅ COMPLETED
- **task-006-async-callback-core.md** — Async callback to core ✅ COMPLETED
- **task-007-integration-tests.md** — Integration tests ✅ COMPLETED
- **task-008-configuration-documentation.md** — Configuration documentation 🔄 IN PROGRESS
- **task-009-structured-audit-logging.md** — Structured audit logging ✅ COMPLETED
- **task-010-quality-test-improvements.md** — Quality test improvements 🔄 IN PROGRESS
- **task-011-run-tests-and-update-report.md** — Run tests and update report ⏳ NOT STARTED

**Note:** Additional notes, planning docs, and reports are in `notes/`.
## Frontend Tasks (front-end-paty)

Tasks em `spec/tasks/front-end-paty/`:

- **task-001-scaffold-nextjs.md** — Scaffold Next.js 14 project with pnpm, TypeScript, Tailwind, and shadcn/ui
- **task-002-design-system.md** — Implement design.json tokens in Tailwind config and global CSS
- **task-003-shadcn-components.md** — Install and override shadcn/ui base components
- **task-004-shared-components.md** — Build StepIndicator, SecurityBadge, LoadingSpinner, EmptyState, ErrorState, CopyButton
- **task-005-api-client-types.md** — Build API client, TypeScript types, formatters, and idempotency utilities
- **task-006-use-payment-hook.md** — Build usePayment hook with TanStack Query mutation and polling
- **task-007-checkout-shell-form.md** — Build CheckoutShell state machine and StepPaymentForm
- **task-008-step-processing.md** — Build StepProcessing animated loading component
- **task-009-step-three-ds.md** — Build StepThreeDs redirect component and wire CHALLENGE_PENDING flow
- **task-010-step-success-error.md** — Build StepSuccess and StepError terminal screens
- **task-011-wire-checkout-route.md** — Wire checkout route, root layout, and QueryClientProvider
- **task-012-transaction-table.md** — Build TransactionTable, FilterBar, TransactionCard, and useTransactions hook
- **task-013-status-badge-detail.md** — Build StatusBadge and TransactionDetail with polling
- **task-014-wire-dashboard-routes.md** — Wire dashboard routes: login, dashboard, and transaction detail
- **task-015-review-impeccable.md** — Review all screens with impeccable skill and apply improvements
- **task-016-semantic-commits.md** — Create semantic commits for all frontend work

## 3DS ↔ Core Payment Integration Tasks

### 3DS Engine (Additive)

- **task-001-jwt-generate-token.md** — Add `JwtTokenProvider.generateToken` method
  - Status: ✅ COMPLETED
  - Components: `generateToken(challengeId, txId, merchantId, amount)` using existing key and expirationSeconds
  - Depends on: nothing

- **task-002-session-request-dtos-and-create-session.md** — ThreeDsSession DTOs and `ChallengeSessionService.createSession`
  - Status: ✅ COMPLETED
  - Components: `ThreeDsSessionRequest`, `ThreeDsSessionResponse` records; `createSession` method; constructor update
  - Depends on: task-001

- **task-003-session-controller.md** — Create `ThreeDsSessionController` `POST /api/v1/3ds/sessions`
  - Status: ✅ COMPLETED
  - Components: `ThreeDsSessionController` (pure transport, 201 Created, API-key protected)
  - Depends on: task-002

### Core Payment (New Components)

- **task-004-wire-payment-controller.md** — Wire `PaymentController` to call `processPayment()`
  - Status: ✅ COMPLETED
  - Components: `PaymentOrchestrationService` injection, conditional 202 for `CHALLENGE_PENDING`
  - Depends on: nothing (parallelize with 3DS Engine tasks)

- **task-005-three-ds-client.md** — `ThreeDsClient`, `challengeId` column, `PaymentResponseDTO` update
  - Status: ✅ COMPLETED
  - Components: `ThreeDsClient` (RestTemplate, risk fallback), Flyway migration V4, `Transaction.challengeId/challengeAcsUrl`, `PaymentResponseDTO` fields
  - Depends on: task-003, task-004

- **task-006-callback-handler.md** — `ThreeDsCompletedEvent`, callback handler, orchestration methods
  - Status: ✅ COMPLETED
  - Components: `ThreeDsCompletedEvent`, `ThreeDsCallbackController`, `completeThreeDsAuthentication()`, `resumePaymentAfterAuth()`
  - Depends on: task-005

- **task-007-payment-finalizer.md** — `ThreeDsPaymentFinalizer` and `@EnableAsync`
  - Status: ✅ COMPLETED
  - Components: `ThreeDsPaymentFinalizer` (`@Async @TransactionalEventListener(AFTER_COMMIT)`)
  - Depends on: task-006
