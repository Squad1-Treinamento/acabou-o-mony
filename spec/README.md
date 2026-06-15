# Specification-Driven Development (SDD) — Acabou o Mony

This folder is the **single source of truth** for all requirements, design, and
planning in the Acabou o Mony project. Every behavioral change, architectural
decision, and execution task traces back to a document rooted here.

## SDD Workflow

The project follows a Specification-Driven Development workflow governed by
[AGENTS.md](../AGENTS.md) at the repository root. Two tracks are available:

| Track | Flow | When to use |
|-------|------|-------------|
| **Direct** | Vision → Specs → Tasks → Implement → Validate | Clear, constrained tasks |
| **Full** | Vision → User Stories → Specs → Tech Plans → Tasks → Implement → Validate | Ambiguous or cross-cutting concerns |
| **Quick fix** | Implement → Validate | ≤3 files, obvious scope |

Key principles:
- Specs are the source of truth; update specs before code changes.
- Three agent roles enforce discipline: **Spec Architect** (writes specs),
  **Software Engineer** (implements with tests), **Review Agent** (checks
  code against specs).
- Validation commands (Maven, pytest, Docker Compose) run after every
  implementation step — see `AGENTS.md` for the exact commands.

## Folder Layout

| Directory | Purpose | Entry point |
|-----------|---------|-------------|
| [`adrs/`](adrs/README.md) | Architecture Decision Records — documented tradeoffs and rationale for every structural choice | [`adrs/index.md`](adrs/index.md) |
| [`specs/`](specs/README.md) | Behavioral specifications with acceptance criteria — one spec per feature area | [`specs/index.md`](specs/index.md) |
| [`tech-plans/`](tech-plans/README.md) | Technical implementation plans mapping specs to concrete code changes | [`tech-plans/index.md`](tech-plans/index.md) |
| [`tasks/`](tasks/README.md) | Executable task breakdowns grouped by feature branch — what to build and in what order | [`tasks/index.md`](tasks/index.md) |
| [`user-stories/`](user-stories/README.md) | User stories capturing merchant and customer needs that drive feature definition | [`user-stories/index.md`](user-stories/index.md) |

Each subfolder exposes two entry points:
- `README.md` — a human-readable overview of the folder (this file establishes
  the convention).
- `index.md` — a compact, machine-friendly index used by AI agents for quick
  document location (see `AGENTS.md` doc index).

## Top-Level Files

- **`index.md`** — Minimal entry point for AI agents. Lists every subfolder's
  index so agents can find the right document with the smallest possible
  context.
- **`RULES.md`** — Project intent and boundaries: who the system serves,
  what outcomes it optimises for, explicit non-goals, and guiding principles.
  Every spec, plan, and ADR must be consistent with this file.

## Agent Contract

[`AGENTS.md`](../AGENTS.md) at the repository root defines how AI agents
interact with this folder:
- Agents must declare their role (Spec Architect / Software Engineer /
  Review Agent).
- Specs must be updated *before* code changes that affect behavior.
- Automated tests must map to acceptance criteria defined in specs.
- When specs are missing, contradictory, or untestable, agents must stop
  and ask for human review (see the 10 crucial questions in `AGENTS.md`).

## Relationship to the Rest of the Repository

For a project-level overview (stack, architecture, setup instructions, CI/CD),
see the root [`README.md`](../README.md).
