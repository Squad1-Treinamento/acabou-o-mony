# Planning Questions

## Context

The PRD asks to:
1. **"Atualizar spec/tasks/review-report.md cobrindo tasks 001-009 com Issues no início de cada task"** — Already done. `spec/tasks/review-report.md` exists (563 lines), uses Issues-first format per task, covers all 9 tasks with IDs C-001 through m-013.
2. **"baseado no código existente em 3ds-engine/"** — Already done. Report is based on actual code.
3. **"refazer os testes unitários e de integração"** — Unclear scope.
4. **"repostar"** — Unclear what this means.

## Uncommitted Working-tree Changes

The working tree has uncommitted changes that suggest someone is actively fixing tests:

| File | Change |
|------|--------|
| `ChallengeSessionRepository.java` | Added `deleteSession()`, `deleteAuthResult()` methods |
| `CallbackNotifier.java` | Pattern matching instanceof update (`HttpStatus hs && hs.isError()`) |
| `pom.xml` | Added maven-surefire-plugin 3.2.5 with byte-buddy experimental flag |
| `ThreeDsChallengeControllerIntegrationTest.java` | Updated cleanRedis to use new delete methods; updated `notifyCore` mock to 4-arg signature (added `merchantId`) |
| `AuthVerificationServiceTest.java` | Added `findSessionById` mock in idempotency test |
| `AuthVerificationServiceAuditTest.java` | Added `findSessionById` mock in `shouldLogCachedEvent` |
| `ChallengeSessionServiceAuditTest.java` | Added missing `when` import |

## Issues in review-report.md NOT yet resolved in code

| ID | Issue | Still in code? |
|----|-------|----------------|
| M-001 | No `challengeId` in `ThreeDsException` | Yes — still only has `errorCode` |
| M-002 | Dev fallback secret hardcoded | Yes — still in `JwtTokenProvider.java:27` |
| M-003 | Blocking `ResponseEntity` in `GlobalErrorHandler` | Yes — still blocking |
| M-004 | `instanceof` + cast in `findAuthResult` | Yes — still uses `isInstance` + cast |
| M-005 | JWT camelCase/snake_case mismatch | Yes — fields named camelCase but JWT claims use snake_case |
| M-006 | Response field naming inconsistency | Yes — `MfaVerifyResponse` uses camelCase, `ErrorResponse` uses snake_case |
| M-007 | No explicit HTTP/2 on callback | Yes — WebClient not configured with H2 protocol |
| M-008 | Fire-and-forget callback may be lost during shutdown | Yes — no durable queue |
| M-009 | `Thread.sleep(2500ms)` in expiry test | Yes — still in integration test |

## Questions

### 1. What exactly does "refazer os testes" mean?
- **A)** Re-run the existing tests (they may pass/fail and we just report results)?
- **B)** Fix the broken/uncommitted test changes so they pass?
- **C)** Rewrite/restructure the tests (e.g., extract separate test classes, replace `Thread.sleep`, add missing test coverage)?
- **D)** All of the above?

### 2. What does "repostar" mean?
- **A)** `git push` the latest changes?
- **B)** Regenerate/update the review-report.md with new findings?
- **C)** Generate a fresh test report (e.g., surefire report or a markdown summary)?
- **D)** Both A and B?

### 3. Should the Issues in review-report.md be fixed first?
- The report identifies 9 major issues (M-001 through M-009) and 13 minor issues (m-001 through m-013).
- Should these issues be **fixed in code** before re-running tests, or is the scope purely **test-focused**?

### 4. What about the uncommitted working-tree changes?
- These look like partial fixes (new `deleteSession`/`deleteAuthResult` methods, mock updates).
- Should these be **committed and pushed** as part of this work, or are they unrelated?

### 5. Can Java/Maven be run in the current environment?
- `JAVA_HOME` is not set. Tests cannot run from this environment.
- Should the deliverable include commands/instructions to run tests locally, or is a different environment expected?

### 6. Are there any new test scenarios needed beyond what exists?
- Current tests cover the acceptance criteria, but the review report flags `Thread.sleep` fragility (M-009), `.block()` in test setup (m-008), and missing `TestRedisConfig` (m-009).
- Should fixing these quality concerns be in scope?
