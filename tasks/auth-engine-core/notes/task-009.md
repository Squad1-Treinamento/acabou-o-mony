# Implementation Notes — task-009: Structured Audit Logging

## Decisions
- NDJSON format built via string concatenation rather than `ObjectMapper` — avoids inject dependency, keeps it simple since all values are safe strings (UUIDs, status codes).
- `auditLog()` helper as `static` package-private method in `ChallengeSessionService` — reused by `AuthVerificationService` and `CallbackNotifier` (same package). Reduces duplication across three services.
- Event types: `challenge.expired`, `challenge.approved`, `challenge.declined`, `challenge.cached`, `callback.sent`, `callback.failed`, `callback.retry`.
- Audit tests in separate files (`ChallengeSessionServiceAuditTest`, `AuthVerificationServiceAuditTest`) using Logback `ListAppender` — avoids cluttering existing behavior tests with log assertions.

## Deviations
- `CallbackNotifier` HTTP error log (`onStatus` handler) no longer logs the response body — was removed when aligning to NDJSON. The status code is available in the error's `doOnError` handler indirectly via the exception message.
- `CallbackNotifier` retry log changed to NDJSON format: `{"event":"callback.retry","challenge_id":"...","attempt":N}`.

## Trade-offs
- String concatenation for JSON instead of `ObjectMapper` — simpler but fragile if values contain special characters. Challenge/transaction IDs are UUIDs, so safe.
- Separate audit test files add test count but keep focus. Each service has one audit test class covering all its event types.

## Risks
- `auditLog()` helper is in `ChallengeSessionService` but used by other classes — creates a dependency on that class for a utility method. If `ChallengeSessionService` is removed, compilation breaks. Mitigated: low risk since it's a core service.
- Logback `ListAppender` tests are fragile to log level changes. If a log level changes from WARN to INFO, the test fails. Acceptable — intentional.
