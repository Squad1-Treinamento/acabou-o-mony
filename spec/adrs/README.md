# Architecture Decision Records (ADRs)

An Architecture Decision Record (ADR) is a short document that captures a
significant architectural decision, its context, the alternatives considered,
and the consequences — both positive and negative. ADRs serve as the permanent
record of *why* the system is built the way it is, so future contributors (and
future selves) can understand the reasoning behind each architectural choice
without rediscovering it.

Every ADR follows the same structure:

1. **Title** — what was decided
2. **Status** — Accepted, Proposed, Deprecated, or Superseded
3. **Context** — the problem or forces that drove the decision
4. **Decision** — what was chosen and how it works
5. **Consequences** — positive outcomes, trade-offs, and risks

ADRs are immutable once accepted. If a decision is revisited, a new ADR is
created that supersedes the old one.

## Creating a New ADR

Create a new ADR when you make a significant architectural choice that has
lasting effect on the system — framework selection, service boundaries,
infrastructure topology, security model, data flow design.

**Naming convention:**

    adr-NNN-title-with-hyphens.md

where `NNN` is the next sequential number (e.g., `adr-004-...`).

**Template:** Copy an existing ADR and fill in the sections. Every ADR must
contain a frontmatter block with `id`, `status`, and `links`.

Place the file in this directory (`spec/adrs/`) and add an entry to `index.md`.

## Existing ADRs

### ADR-001 — ngrok as API Gateway with Nginx Reverse Proxy

Use ngrok for public HTTPS exposure (TLS termination, public endpoint) and
Nginx as internal reverse proxy and load balancer. This avoids provisioning
cloud infrastructure for local/staging testing while simulating production
traffic flow. Trade-off: dependency on ngrok auth token and unencrypted
traffic between ngrok and Nginx.

### ADR-002 — 3DS / MFA Auth Engine as a Separate Microservice

Extract 3D Secure 2.x and MFA step-up authentication into its own reactive
microservice (Java 21, Spring WebFlux, Netty) with state persisted in Redis,
isolated from the Core Payment Processing Service. This preserves the Core's
<1s SLA for standard transactions and allows independent scaling, at the cost
of operational complexity and eventual-consistency coordination via HTTP/2
callbacks and Redis fallback polling.

### ADR-003 — Cache and Idempotency Layer with Redis Standalone

Implement a unified cache-aside layer (Spring `@Cacheable`) and idempotency
mechanism (`Idempotency-Key` header) on Redis Standalone, shared between the
3DS Engine and Core Service under isolated namespaces (`3ds:*`, `core:*`).
Includes configurable TTLs per data type, Resilience4j circuit breaker for
graceful fallback to PostgreSQL, and a policy to never cache sensitive data
(PAN, CVV, secrets). Redis Cluster and Protobuf serialization were rejected
for the MVP to keep operational complexity low.

## Navigation

- [index.md](./index.md) — entry point to browse all ADRs
- [specs/README.md](../specs/README.md) — behavioral specs that implement
  these architectural decisions
- [spec/README.md](../README.md) — SDD overview for the full project
