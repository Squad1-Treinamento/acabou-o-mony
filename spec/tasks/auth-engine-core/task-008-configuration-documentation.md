---
id: task-008
status: in_progress
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-002-3ds-mfa-auth-engine.md
  - spec/specs/spec-002-3ds-mfa-auth-engine.md
---
# Document 3DS Engine Configuration Properties

## Local Context

- **Directory:** `3ds-engine/`
- **Files to modify:** `3ds-engine/src/main/resources/application.yml` (task-001)
- **Files to create (if applicable):** `3ds-engine/CONFIG.md` (if preferred over inline comments)
- **Local dependencies:** All previous tasks (properties must match actual configuration keys used)

## Scope

1. Finalize `application.yml` with all configurable 3DS Engine properties, including descriptive comments:
   - **Redis:** `spring.redis.cluster.nodes`, `spring.redis.password` (marked sensitive), `spring.redis.timeout`
   - **JWT:** `jwt.secret` (marked sensitive, no default), `jwt.expiration-seconds` (default: 600)
   - **Session:** `3ds.session-ttl-seconds` (default: 600), `3ds.auth-result-ttl-seconds` (default: 86400)
   - **Callback:** `3ds.callback-url` (no default — environment specific)
   - **Rate limit:** `3ds.rate-limit-per-second` (default: 100)
   - **Risk thresholds:** `risk.score-threshold` (default: 70), `risk.high-value-threshold` (default: 5000.00)
2. Ensure every property used across tasks 002-005 is present in `application.yml`.
3. Add YAML comments documenting each property: purpose, expected format, examples.
4. Optionally create `CONFIG.md` with a reference table of all properties, their types, defaults, and descriptions.

## Acceptance Criteria and Tests

- **Success:** All configuration properties are documented with clear descriptions, types, and defaults. Properties match the `@Value` or `@ConfigurationProperties` bindings used across the codebase.
- **Failure:** Missing or incorrect property names (detected at startup or during review).
- **Tests:** None (documentation only). Verify by reviewing that every `@Value("${...}")` or config key referenced in the code has a corresponding entry.

## Constraints and Negative Instructions

- Secrets (`jwt.secret`, `redis.password`) must be marked as sensitive with no default values.
- Do NOT include real secrets in the file — only placeholder values.
- Properties must be alphabetically grouped (Redis first, then JWT, then 3DS, then risk).
- Default values must be safe for local development (do not default to production values).
