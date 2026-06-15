# Technical Plans

A tech plan bridges **behavioral specifications** (the *what*) and **executable tasks** (the *who does what when*). It defines the *how*: stack choices, component breakdown, data flow diagrams, implementation sequencing, risk analysis, and validation criteria. Each plan translates one or more specs into concrete engineering steps with enough detail for a developer to implement without further discovery.

## Naming Convention

Each plan file follows `plan-NNN-title.md`:

| Pattern | Example |
|---------|---------|
| `plan-NNN.md` | `plan-001-core-payment-processing.md` |
| Sequential numbering | 001–005 |
| Lowercase kebab-case title | Matches the feature area |

The [`index.md`](index.md) file serves as the machine-friendly entry point for AI agents — it lists all active plans in priority order.

## Plans Summary

### plan-001 — Core Payment Processing
Implements the Core Payment Service in 8 phases: persistence schema (PostgreSQL + Redis), idempotency enforcement, synchronous Mercado Pago integration, UNKNOWN state reconciliation, transactional outbox/webhook dispatch, health checks, 3DS risk evaluation with challenge flow, PCI-DSS security hardening, and infrastructure validation. Prioritises deterministic consistency and safe failure recovery before scaling.

### plan-002 — 3DS/MFA Auth Engine
Implements a reactive Spring WebFlux microservice with no database dependency — all state lives in Redis. Covers session creation, JWT landing-page redirect, MFA token verification, async HTTP/2 callback to Core, idempotency by `challenge_id`, and session expiry enforcement. The engine is isolated from the relational ledger and from Mercado Pago.

### plan-003 — Entry Point with ngrok and Nginx
Sets up the Docker Compose ingress layer: ngrok for public HTTPS exposure, Nginx as reverse proxy with rate limiting (10 r/s, burst 20) and TLS termination. Configuration is env-driven with `envsubst` template rendering. Minimal footprint — only two containers, no application logic.

### plan-004 — Cache and Idempotency Layer
Defines a shared Redis cache and idempotency layer between Core Service and 3DS Engine. Uses Cache-Aside pattern with Spring `@Cacheable`/`@CacheEvict`, Resilience4j circuit breaker for Redis failure fallback, and a dedicated idempotency key namespace (`core:idempotency:*`) for mutation endpoints. Includes AES-256-GCM encryption infrastructure (prepared, not active in MVP).

### plan-005 — 3DS ↔ Core Payment Integration
Orchestrates bidirectional HTTP communication between the two services. Adds a session creation endpoint to the 3DS Engine and a callback handler + async finalizer to Core. Uses Spring `@TransactionalEventListener(AFTER_COMMIT)` to prevent race conditions — the finalizer (Mercado Pago capture) runs only after the DB commit is durable. Addresses the missing wiring between `PaymentController` and `PaymentOrchestrationService`.

## Relationship to Other Documents

See [`../tasks/README.md`](../tasks/README.md) for the full specs→plans→tasks pipeline diagram.

Each tech plan references one or more behavioral specs and produces a task breakdown that gets populated in `spec/tasks/`. The ADRs in `spec/adrs/` record architecture decisions that constrain or guide the plan's choices (e.g., ADR-003 for the cache layer). Spec changes must precede plan changes; plan changes must precede task changes.

## Navigation

| Document | Link |
|----------|------|
| Plan index (agent entry point) | [`index.md`](index.md) |
| Behavioral specs implemented by these plans | [`../specs/README.md`](../specs/README.md) |
| Task breakdowns derived from these plans | [`../tasks/README.md`](../tasks/README.md) |
| Architecture decisions constraining these plans | [`../adrs/README.md`](../adrs/README.md) |
| SDD folder overview | [`../README.md`](../README.md) |
| Project root (stack, architecture, setup) | [`../../README.md`](../../README.md) |
