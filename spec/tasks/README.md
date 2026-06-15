# Execution Tasks

Tasks are the finest-grained executable units of work in the Acabou o Mony SDD
pipeline. Each task maps to one or more **acceptance criteria** defined in a
behavioral spec, is sequenced by a **tech plan**, and contains enough detail
(scope, files to change, tests, validation commands) for a Software Engineer to
implement without further discovery.

```
specs/  ──>  tech-plans/  ──>  tasks/  ──>  code
(what)        (how)           (who & when)    (implementation)
```

## Directory Organization

Tasks live in `tasks/<branch>/` — one subdirectory per feature branch. Each
subdirectory corresponds to a tech plan and implements a cohesive feature area.

| Branch Directory | Tasks | Status | Implements |
|---|---|---|---|
| `core-payment/` | 19 (001–019) | 19 ✅ completed | Core Payment Processing (plan-001): schema, state machine, idempotency, Mercado Pago client, orchestration, reconciliation, outbox, webhook, risk, audit |
| `cache-idempotency-redis/` | 15 (001–015) | 15 ⏳ planned | Cache & Idempotency Layer (plan-004): Redis config, circuit breaker, idempotency service, encryption infra |
| `auth-engine-core/` | 11 (001–011) | 8 ✅ completed, 2 🔄 in progress, 1 ⏳ not started | 3DS/MFA Auth Engine (plan-002): Spring WebFlux scaffold, Redis session, JWT, challenge, MFA verification, callback |
| `3ds-integration/` | 7 (001–007) | 7 ✅ completed | 3DS ↔ Core Integration (plan-005): session creation, callback handler, payment finalizer, bidirectional HTTP wiring |

### Status Key

- ✅ COMPLETED — implemented, tested, and verified
- 🔄 IN PROGRESS — being actively worked
- ⏳ PLANNED / NOT STARTED — defined but not yet implemented

## File Convention

Each task file is named `task-NNN-descriptive-kebab-title.md`, where `NNN` is a
sequential number scoped to its branch directory (e.g., `task-001-database-schema-jpa.md`).

Every task file contains:

- **Frontmatter** — `id`, `status`, `links` referencing the tech plan, spec,
  and any related tasks.
- **Local Context** — target module, files to create or modify, local
  dependencies (Spring Boot starters, third-party libraries).
- **Scope** — numbered implementation steps, each with specific files and
  changes.
- **Acceptance Criteria & Tests** — success/failure conditions mapped to spec
  requirements, including exact test class names, test counts, and validation
  commands (`mvn test -Dtest="..."`).
- **Constraints & Negative Instructions** — explicit DOs and DO NOTs to guide
  implementation (e.g., "Do NOT add business logic to entities", "Do NOT add
  relational DB dependencies").
- **Completion Checklist** — for completed tasks, a tickbox list of every
  deliverable (schema migrations, entity fields, test scenarios).

## Entry Points

| Document | Purpose | Link |
|---|---|---|
| `index.md` | Full task list with per-task status and links — entry point for AI agents | [`index.md`](index.md) |
| `tech-plans/README.md` | Technical plans that define the sequencing of these tasks | [`../tech-plans/README.md`](../tech-plans/README.md) |
| `specs/README.md` | Behavioral specs whose acceptance criteria these tasks implement | [`../specs/README.md`](../specs/README.md) |
| `adrs/README.md` | Architecture decisions that constrain implementation choices | [`../adrs/README.md`](../adrs/README.md) |
| SDD overview | Project-level SDD folder structure and workflow | [`../README.md`](../README.md) |
| Project root | Stack, architecture, setup, CI/CD | [`../../README.md`](../../README.md) |

## Workflow

Defined in [`AGENTS.md`](../../AGENTS.md):

1. **Spec Architect** writes or updates specs → **Software Engineer** creates
   tasks if needed → implements → validates.
2. Validation commands run after every implementation step (Maven test, pytest,
   Docker Compose).
3. Spec changes must precede task changes; task changes must precede code
   changes.
4. Quick fixes (≤3 files, obvious scope) skip formal task files entirely.
