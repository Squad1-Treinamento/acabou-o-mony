# Behavioral Specifications

This directory contains the behavioral specifications for the Acabou o Mony platform. Each spec defines **what** the system must do — the business rules, acceptance criteria, validation rules, and state machine transitions that implementation and tests must conform to.

## Purpose

A spec in this project is a deterministic, testable document that:

- Defines behavioral rules (state machines, transition guards, idempotency invariants)
- States acceptance criteria in BDD (Given/When/Then) form
- Establishes data contracts (request/response schemas, payload formats, headers)
- Declares error-handling behavior for every failure scenario
- Prescribes performance SLAs and concurrency guarantees

Specs are the **source of truth** for implementation. Tech plans (in `../tech-plans/`) derive from specs and define **how** to implement. Tasks (in `../tasks/`) break the tech plans into atomic units of work. If a discrepancy arises between code and a spec, the spec takes precedence — and must be updated first.

## Naming Convention

```
spec-NNN-title-with-hyphens.md
```

- `NNN` — sequential number (001, 002, ...), assigned at creation.
- `title-with-hyphens` — kebab-case English descriptor.
- Each spec has an `id` front-matter field (e.g. `id: spec-001`) and a `status` field (`active`, `draft`, `superseded`).

## Specs Overview

### spec-001 — Core Payment Processing
Defines the full transaction lifecycle state machine (CREATED → VALIDATED → CHALLENGE_PENDING → AUTHENTICATED → PROCESSING → COMPLETED/DECLINED/FAILED/UNKNOWN), two-tier idempotency (Redis fast-path + PostgreSQL authoritative), optimistic locking with version increments on every transition, the transactional outbox pattern for webhook delivery, reconciliation of UNKNOWN payments with exponential-backoff retries, 3DS risk evaluation rules, and persistence/audit schemas. ~80 acceptance criteria embedded as behavioral rules.

### spec-002 — 3DS / MFA Auth Engine
Specifies the isolated step-up authentication service: risk evaluation and the 3DS trigger, challenge initiation via JWT-signed sessions in Redis, the landing page and bank ACS redirect flow, MFA verification with the `/verify` endpoint, and asynchronous callback to the Core Service for ledger finalization. Covers session expiry, idempotency (by `challenge_id`), and the guarantee that the <1s SLA for low-risk transactions is never degraded. BDD acceptance criteria cover all 8 key scenarios.

### spec-003 — Entry Point with ngrok + Nginx
Describes the local production-simulated entry point: ngrok as the public HTTPS tunnel, Nginx as the reverse proxy and load balancer (round-robin), `/healthz` served locally without backend calls, rate limiting (10r/s burst=20 nodelay), env-driven upstream configuration (`UPSTREAMS` / `UPSTREAM_HOST` + `UPSTREAM_PORT`), and fail-fast validation of `NGROK_AUTHTOKEN` and `UPSTREAMS`. 7 BDD scenarios cover startup, routing, health check, and failure cases.

### spec-004 — Cache and Idempotency Layer
Defines the Redis-based caching and idempotency infrastructure: Docker Compose provisioning (Redis 7 Alpine, AOF persistence, 512 MB LRU), `.env.example` configuration with all Redis/cache/circuit-breaker variables, 5 cache namespaces (merchant config, risk thresholds, BIN lookup, transaction history, idempotency keys) with specific TTLs and invalidation rules, a Resilience4j circuit breaker with OPEN/HALF_OPEN/CLOSED states and silent fallback to PostgreSQL, and an AES-256-GCM encryption scaffold (prepared but inactive in MVP). 30+ BDD acceptance criteria across all functional areas.

### spec-005 — 3DS ↔ Core Payment Integration
Defines the bidirectional contract between the Core Payment (Spring MVC + JPA) and the 3DS Engine: synchronous `POST /api/v1/3ds/sessions` via `RestTemplate` to initiate challenges, `CallbackNotifier` HTTP callback to `POST /api/v1/payments/3ds-callback`, `@TransactionalEventListener(AFTER_COMMIT)` to eliminate race conditions between DB commit and async finalization, the `ThreeDsPaymentFinalizer` for the AUTHENTICATED → PROCESSING transition, and the `ThreeDsClient` with risk-aware fallback (LOW risk → `Optional.empty()`, HIGH risk → 503). Covers new components in both services, idempotency guards, error-handling matrix, and testing strategy with WireMock and `@RecordApplicationEvents`.

## Specs vs Tech Plans

| Artifact | Role | Audience |
|---|---|---|
| **Spec** (this directory) | Defines **what** — behavioral rules, acceptance criteria, data contracts | Developers, QA, reviewers |
| **Tech Plan** (`../tech-plans/`) | Defines **how** — class structure, algorithms, config files, migration scripts | Implementers |
| **Task** (`../tasks/`) | Defines **who does what** — atomic work items with verification criteria | Sprint / assignment |

All three must remain consistent. Update the spec first when behavior changes; then update the tech plan and tasks.

## Index

For a flat list with one-line descriptions of every spec, see [`index.md`](index.md).

## Related

- [SDD Overview](../README.md) — root of the spec directory tree
- [Tech Plans](../tech-plans/README.md) — implementation plans derived from these specs
- [Tasks](../tasks/README.md) — atomic work items broken down from tech plans
