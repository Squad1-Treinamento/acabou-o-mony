# task-004 — Implement 3DS Landing Page (Bank Redirect)

## Implementation Notes

- **Decisions:** `ChallengeSessionService.resolveChallenge()` returns `Mono<ChallengeSession>` (empty if not found, error if expired) — controller handles 404 vs 410 via `defaultIfEmpty` vs `GlobalErrorHandler`. Controller uses `WebTestClient.bindToController()` with manual `controllerAdvice(new GlobalErrorHandler())` to test error handling without full Spring context.
- **Deviations:** Session not found returns HTTP 404 via `defaultIfEmpty` (empty Mono → 404). Session expired throws `ChallengeExpiredException` → handled by `GlobalErrorHandler` → HTTP 410. Invalid JWT throws `InvalidTokenException` → `GlobalErrorHandler` → HTTP 400. All match the task spec.
- **Trade-offs:** Separated redirect logic into `redirectToAcs()` helper method to avoid inline lambda type inference issues with `Mono<ResponseEntity<Void>>`.
- **Risks:** No Redis connection needed for tests — all dependencies mocked via Mockito. Service expiry check uses `Instant.now()` which makes the test time-dependent (acceptable for unit tests with recent session creation).
