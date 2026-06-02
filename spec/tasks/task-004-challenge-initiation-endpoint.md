---
id: task-004
status: planned
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-001-3ds-mfa-auth-engine.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
---
# Implement 3DS Landing Page (Bank Redirect)

## Local Context

- **Directory:** `3ds-engine/`
- **Files to create:**
  - `3ds-engine/src/main/java/com/acabouomony/engine/controller/LandingPageController.java` — Landing page controller
  - `3ds-engine/src/main/java/com/acabouomony/engine/service/ChallengeSessionService.java` — session lookup + expiry check business logic
- **Files to modify:** none
- **Local dependencies:** `ChallengeSessionRepository` (task-002), `JwtTokenProvider` (task-003)

## Scope

1. Create `ChallengeSessionService` as `@Component`:
   - `resolveChallenge(challengeId)` — calls `ChallengeSessionRepository.findSessionById()`, checks expiry via `isSessionExpired()` (delegates to task-008 logic later), returns `Mono<ChallengeSession>`.
2. Create `GET /challenge/{challenge_id}` endpoint in `LandingPageController`:
   - Accepts `challenge_id` as path variable and `jwt` as required query parameter.
   - Calls `JwtTokenProvider.verify(jwt)` to validate the JWT — if invalid, returns HTTP 400 with `ErrorResponse` (error code `INVALID_TOKEN`).
   - Calls `ChallengeSessionService.resolveChallenge(challengeId)` to look up the session.
   - If session not found or expired, returns HTTP 404/410 with appropriate error.
   - If valid, reads `acs_url` from the session and returns an HTTP 302 redirect (`Location` header) pointing to the bank's ACS URL.
3. The landing page is **read-only** — no session state is mutated.

## Acceptance Criteria and Tests

- **Success:** Valid JWT + valid session returns HTTP 302 with `Location` header set to the bank's ACS URL (from `acs_url` field in Redis).
- **Failure:** Invalid JWT returns HTTP 400 with error code `INVALID_TOKEN`. Session not found returns HTTP 404. Expired session returns HTTP 410 with error code `3DS_CHALLENGE_EXPIRED`.
- **Tests:** `WebTestClient` integration test: mock a session in Redis, send request with valid JWT, assert 302 + Location header. Test invalid JWT returns 400. Test missing session returns 404.

## Constraints and Negative Instructions

- Must be fully reactive — controller returns `Mono<ResponseEntity<Void>>` (redirect with empty body).
- JWT is extracted from query param `?jwt=<token>`, never from headers or body.
- Never mutate session state on the landing page — read-only.
- The `acs_url` field is set by Core Service at initiation time; the Engine just reads it.
- Do not expose raw error details to the browser (security).
