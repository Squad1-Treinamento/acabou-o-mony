# Review Report — task-003: JWT Verification Utility

## Summary

The implementation is **substantially complete and correct**. `JwtClaims`, `JwtTokenProvider`, and the test suite align with the spec (`spec-001`) and task definition (`task-003`). The code is clean, idiomatic, and follows reactive WebFlux patterns. One medium-severity security concern and a few minor gaps were identified.

## Compliance Score

**90%** — All acceptance criteria are met. The issues below are minor enough that the feature is functional and safe for its intended use, but should be addressed before going to production.

### Critical (blocking issues that must be fixed)

None.

### Minor (non-blocking suggestions)

1. **Error message leaks JJWT internals** — `JwtTokenProvider.java:57`
   `"Invalid JWT: " + e.getMessage()` passes the JJWT library's exception message to the caller. Through `GlobalErrorHandler`, this is returned in the HTTP response body. While the leaked info (e.g., "JWT signature does not match locally computed signature") is not highly sensitive, it reveals failure specifics to the caller. **Recommendation:** Use a generic message like `"Invalid JWT token"` for all non-expiry failures, and log the details server-side.

2. **Silent dev fallback secret creates production risk** — `JwtTokenProvider.java:24`
   If `JWT_SECRET` is not set in production, the provider silently falls back to a well-known 256-bit string. No warning is logged. **Recommendation:** Log a `WARN` message when the fallback is activated so operators notice a missing environment variable.

3. **Missing-claims edge case not handled or tested**
   If a JWT lacks `challenge_id`, `transaction_id`, `merchant_id`, or `amount`, `JwtClaims` is constructed with `null` values. The task spec requires missing claims to map to `InvalidTokenException`, but the current code passes them through silently (failure would occur downstream, possibly with a confusing error). **Recommendation:** Validate that all required claims are non-null after parsing and emit `InvalidTokenException` if any are missing.

4. **No test for missing/blank `jwt.secret` configuration**
   The constructor's fallback logic (null/blank/short → dev secret) is untested. A single unit test constructing `JwtTokenProvider` with an empty string would cover this.

5. **`jwt.expiration-seconds` is declared but unused in the provider**
   The task defines this config property, but `JwtTokenProvider` never reads it (expiry is validated from the token's `exp` claim). This is **correct behavior** since the verifier should trust the token, not configuration. However, the unused field should be noted in config documentation so operators are not confused. Already documented in `implementation-notes.md` — no code change needed, but worth adding a comment in `application.yml`.

## Test Coverage Assessment

| Acceptance Criterion | Tested? | Test |
|---|---|---|
| **Success:** JWT verified, claims match | ✅ | `shouldVerifyValidToken` |
| **Failure:** Expired JWT → `InvalidTokenException` | ✅ | `shouldRejectExpiredToken` |
| **Failure:** Invalid signature → `InvalidTokenException` | ✅ | `shouldRejectTokenWithInvalidSignature` |
| **Failure:** Malformed JWT → `InvalidTokenException` | ✅ | `shouldRejectMalformedToken` (bonus, not in spec) |
| **Missing claims** → `InvalidTokenException` | ❌ | Not tested |

**Coverage note:** All four explicit task acceptance criteria are covered. The missing-claims case (item 3 above) is the only gap. The tests use `StepVerifier` correctly for reactive assertions. Overall test quality is good — clear, isolated, and deterministic.

## Decision Assessment

All implementer decisions from `implementation-notes.md` are sound:

| Decision | Assessment |
|---|---|
| **JJWT for JWT parsing** | Appropriate. Mature, well-maintained library with HS256 support. |
| **Java `record` for `JwtClaims`** | Good choice. Immutable, concise, fits DTO semantics. |
| **Dev fallback secret for local dev** | Reasonable trade-off. The risk is documented and acceptable for development only (flagged above as minor due to no warning log). |
| **`jwt.expiration-seconds` unused** | Correct. The provider validates expiry from the token's `exp` claim, which is the source of truth. The config value belongs on the signer side (Core Service). |
| **`application.yml` not modified** | Acceptable. Secret is env-only in production; tests inject their own secret. |

No decisions conflict with the spec or task definition.
