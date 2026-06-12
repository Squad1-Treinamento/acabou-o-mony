# AGENTS

Purpose
- Provide a single, minimal index for AI agents in this SDD project.
- Specs are the source of truth; update specs before code changes.

SDD Workflow (two tracks, choose per task)

  Direct (minimum — when the task is clear and constrained):
    Vision → Specs → Tasks → Implement → Validate

  Full (when ambiguity or cross-cutting concerns exist):
    Vision → User Stories → Specs → Tech Plans → Tasks → Implement → Validate

  Quick fix (≤3 files, obvious scope):
    Implement → Validate
    Skip spec docs entirely; document only if patterns emerge.

Tasks live in `spec/tasks/<branch>/` — one subdirectory per feature branch.

Validation commands (run after every Implement):
  - Java: `cd 3ds-engine && mvn test`
  - Python: `pytest tests/`
  - Docker infra: `docker compose up -d --wait && pytest tests/test_entrypoint.py`

Agent Roles (prompt must declare the role)
- Spec Architect: writes/updates specs and acceptance criteria. No code or tests.
- Software Engineer: implements from specs, creates tasks if needed, writes tests.
- Review Agent: checks code vs specs, flags out-of-spec work, reports gaps.

Doc Index (start here)
- `spec/index.md`
- `spec/vision.md`
- `spec/user-stories/index.md`
- `spec/specs/index.md`
- `spec/tech-plans/index.md`
- `spec/tasks/index.md`
- `spec/adrs/index.md`
- `CONTEXT.md` — stack, commands, environments

Guardrails
- Do not invent requirements; follow specs and acceptance criteria only.
- Do not change behavior without updating specs first.
- Prefer simple, explicit code; avoid over-engineering.
- Create automated tests mapped to acceptance criteria.
- Validate changes (tests/build/lint) when commands exist.

When to Stop and Ask for Human Review
- Specs are missing, contradictory, or untestable.
- The change modifies scope, architecture, ADRs, or external dependencies.
- Acceptance criteria are missing or unclear.
- Validation fails without a clear fix.
- Any uncertainty about requirements or approval gates.

Crucial Questions (ask before acting)
1) What is the exact goal and where is it specified?
2) Which flow applies: Direct or Full planning?
3) Are acceptance criteria complete and testable?
4) Do specs/ADRs/tech plans conflict?
5) What is explicitly out of scope?
6) Should specs be updated before code?
7) Which tests cover each acceptance criterion?
8) What validation steps exist and where are they documented?
9) Is any required stack/architecture missing from `CONTEXT.md`?
10) Is human approval required for this change?
