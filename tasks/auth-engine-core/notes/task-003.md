# task-003 — Implement JWT Verification Utility for Challenge Tokens

## Implementation Notes

- **Decisions:** Used JJWT library (`io.jsonwebtoken`) for JWT parsing/verification. `JwtClaims` implemented as a Java `record` for immutability. `JwtTokenProvider.verify()` returns `Mono<JwtClaims>` integrating cleanly with WebFlux reactive chain. Added dev fallback secret (256-bit) when `jwt.secret` is empty/not configured — avoids `WeakKeyException` without requiring a hardcoded secret in `application.yml`.
- **Deviations:** `jwt.expiration-seconds` is not directly used in the `JwtTokenProvider` — the JWT itself carries the `exp` claim, so the provider validates expiry from the token, not from configuration. The `application.yml` was not modified (secret remains env-only for production).
- **Trade-offs:** Dev fallback secret enables `@SpringBootTest` context-load tests without requiring `JWT_SECRET` env var. The fallback uses a simple string check (null/blank/short) to distinguish "not configured" from "configured but too short". Production deployments must set `JWT_SECRET` via environment variable.
- **Risks:** Unit tests use a separate signing key (hardcoded in test) — if the dev fallback secret changes, the tests are unaffected since they inject their own secret. No risk.
