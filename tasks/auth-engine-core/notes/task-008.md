# Implementation Notes — task-008: Configuration Documentation

## Decisions
- All properties documented inline in `application.yml` with YAML comments — no separate `CONFIG.md` created. Keeps config co-located with its values; operators only need to read one file.
- Properties grouped by domain: server → spring (redis) → jwt → 3ds → risk. Alphabetical within groups.

## Deviations
- Added `spring.application.name: 3ds-engine` — missing from original scaffold, useful for metrics/logging.
- `risk.score-threshold` and `risk.high-value-threshold` kept in `application.yml` despite being Core Service properties. They're referenced in spec and tech plan, so documenting them here avoids confusion.

## Trade-offs
- Inline YAML comments are simpler than a separate CONFIG.md but harder to reference in documentation or scripts. Acceptable for MVP.

## Risks
- `jwt.expiration-seconds` is documented but unused in Engine code. If someone changes it expecting the Engine to enforce it, they'll be surprised (the Engine reads `exp` from the token). Already noted in application.yml comment.
