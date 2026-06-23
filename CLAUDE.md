# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Workflow (SDD)

Specs are the source of truth. Before changing behavior, check and update specs. Tasks live in `spec/tasks/<branch>/`.

**Choose one track per task:**
- **Quick fix** (≤3 files, obvious scope): Implement → Validate. Skip spec docs.
- **Direct** (clear, constrained task): Vision → Specs → Tasks → Implement → Validate
- **Full** (ambiguity or cross-cutting): Vision → User Stories → Specs → Tech Plans → Tasks → Implement → Validate

**Validation commands (run after every change):**
```bash
cd core-payment && mvn test         # Core payment service tests
cd 3ds-engine && mvn test           # 3DS engine tests
pytest tests/                       # Python infra/integration tests
docker compose up -d --wait && pytest tests/test_entrypoint.py  # Full stack
```

**Stop and ask a human when:** specs are missing/contradictory, the change touches ADRs or external dependencies, or validation fails without a clear fix.

## Commands

```bash
# Build
cd core-payment && mvn clean package
cd 3ds-engine && mvn clean package

# Test — all tests in a module
cd core-payment && mvn test
cd 3ds-engine && mvn test

# Test — single test class
cd core-payment && mvn test -Dtest=PaymentControllerTest
cd 3ds-engine && mvn test -Dtest=JwtTokenProviderTest

# Test — by group/tag
cd core-payment && mvn test -Dgroups=IntegrationTest

# Docker full stack
docker compose up -d --wait        # Start all services (waits for health checks)
docker compose down -v             # Tear down including volumes

# Python infra tests
pip install -r requirements.txt
pytest tests/
```

## Architecture

Two Spring Boot microservices sit behind an Nginx edge proxy. ngrok tunnels the local stack to the public internet for webhook/acquirer integration.

```
Internet → ngrok → Nginx :8080 → core-payment :8082
                                       ↕
                               3ds-engine :8081
                                       ↕
                         PostgreSQL :5433 + Redis :6379
```

**core-payment** (Spring Boot 3.2.5, Spring Web, Java 21 virtual threads, port 8082)
- Orchestrates the full payment lifecycle: validation → risk evaluation → 3DS step-up (optional) → acquirer submission → reconciliation
- Persists to PostgreSQL via JPA/Flyway; uses Redis for caching and idempotency
- Dispatches webhooks via Transactional Outbox pattern (`outbox_events` table)
- Merchant authentication: Argon2-hashed API key stored in `merchants` table, cached with SHA-256 key (5-min TTL) to meet the <1s SLA

**3ds-engine** (Spring Boot 3.3.5, Spring WebFlux reactive, port 8081)
- Manages 3DS 2.x challenge sessions exclusively; no relational DB — session state only in Redis (10-min TTL)
- Issues HS256 JWTs (600s expiry) for challenge flows; verifies via `JwtTokenProvider`
- Calls back `core-payment` on challenge completion via `CallbackNotifier`

## Key Patterns

**Payment state machine** — enforced by code and a DB CHECK constraint:
```
CREATED → VALIDATED → PROCESSING → COMPLETED
                    ↘ CHALLENGE_PENDING → AUTHENTICATED → PROCESSING
                                                        → DECLINED
                                                        → UNKNOWN → (reconciliation) → COMPLETED/DECLINED/FAILED
```

**Two-tier idempotency:**
1. Redis response cache (fast-path, 24h TTL) — checked before any DB access
2. PostgreSQL unique constraint on `(merchant_id, idempotency_key)` — safety net

**Redis serialization rule:** Always use `Jackson2JsonRedisSerializer<T>` (typed). Never use `GenericJackson2JsonRedisSerializer` — it embeds `@class` metadata that breaks deserialization across deployments.

**Audit log:** The `audit_logs` table is INSERT-ONLY; a DB trigger rejects UPDATE/DELETE. Every status transition writes an entry with old state, new state, actor, timestamp, and SHA-256 checksum.

**Reconciliation:** Payments stuck in `UNKNOWN` (acquirer timeout) are resolved by a background worker that polls Mercado Pago. Entries >5 minutes in UNKNOWN are flagged by the stale-unknown monitor.

## Spring Profiles (core-payment)

Active profiles are additive. The default set for local dev:
```
spring.profiles.active=3ds,audit-logging,risk-evaluation,webhook,mock-acquirer
```

| Profile | Purpose |
|---|---|
| `3ds` | Enables 3DS integration with 3ds-engine |
| `audit-logging` | Structured JSON audit trail |
| `risk-evaluation` | Risk-based authentication scoring |
| `webhook` | Outbox worker + webhook dispatch |
| `mock-acquirer` | Replaces Mercado Pago with a local mock (use for tests) |

## Environment Setup

Copy `.env.example` to `.env` and fill in `NGROK_AUTHTOKEN`. All other defaults work for local dev. The test merchant API key is `teste_key` (seeded by Flyway in dev/test environments).

Key non-obvious env vars:
- `CACHE_ENCRYPTION_ENABLED` — encrypts sensitive Redis values at rest
- `CIRCUIT_BREAKER_ENABLED` — Resilience4j protection around Redis calls
- `THREE_DS_BASE_URL` — must point to `http://3ds-engine:8080` inside Docker network

## Spec/Doc Index

```
spec/index.md              ← start here for any spec work
spec/vision.md
spec/user-stories/
spec/specs/                ← acceptance criteria live here
spec/adrs/                 ← architectural decisions (read before changing infra)
spec/tech-plans/
spec/tasks/<branch>/       ← execution tasks per feature branch
CONTEXT.md                 ← stack summary and environment table
ARCHITECTURE.md            ← deep dive: SLA mechanics, scaling, PCI-DSS approach
```
