---
id: task-003
status: planned
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-001-3ds-mfa-auth-engine.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
---
# Implement JWT Verification Utility for Challenge Tokens

## Local Context

- **Directory:** `3ds-engine/`
- **Files to create:**
  - `3ds-engine/src/main/java/com/acabouomony/engine/security/JwtTokenProvider.java` — JWT verify-only component
  - `3ds-engine/src/main/java/com/acabouomony/engine/security/JwtClaims.java` — Claims DTO
- **Dependencies:** `jjwt-api`, `jjwt-impl`, `jjwt-jackson`, `application.yml` (`jwt.secret`, `jwt.expiration-seconds`)

## Scope

1. Create `JwtClaims` record: `challenge_id`, `transaction_id`, `merchant_id`, `amount` (BigDecimal), `issued_at` (Instant), `expires_at` (Instant).
2. Create `JwtTokenProvider` as `@Component` with **verify-only** semantics:
   - Configure shared secret from `jwt.secret` (HS256, same secret used by Core Service for signing).
   - `verify(token)` — parses and validates JWT (signature + expiry), returns `Mono<JwtClaims>` on success or `Mono.error(InvalidTokenException)` on failure (expired, tampered, malformed).
   - **No `sign()` method.** JWT signing is done exclusively by the Core Service.
3. Read `jwt.secret` and `jwt.expiration-seconds` from `application.yml`.
4. Handle edge cases: expired token, invalid signature, missing claims — all map to `InvalidTokenException` with clear error message. Extract `challenge_id` from claims for session lookup.

## Acceptance Criteria and Tests

- **Success:** JWT signed by Core Service is verified and decoded correctly by the Engine. Claims match original input.
- **Failure:** Expired JWT throws `InvalidTokenException`. Tampered JWT throws `InvalidTokenException`. Malformed JWT throws `InvalidTokenException`.
- **Tests:** Unit test for verify-only (sign a JWT using the same secret inline, then verify it). Unit test for expired token detection. Unit test for invalid signature detection.

## Constraints and Negative Instructions

- Secret must be configurable via `application.yml` — never hardcoded.
- Use HS256 algorithm (RS256 later if multi-service verification is needed).
- Must include `exp`, `iat`, `challenge_id`, `transaction_id`, `merchant_id`, `amount` claims.
- Do not include PII or card data in JWT claims.
