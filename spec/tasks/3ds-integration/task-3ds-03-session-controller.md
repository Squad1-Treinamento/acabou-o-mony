---
id: task-3ds-03
status: planned
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-002-3ds-core-payment-integration.md
  - spec/specs/spec-002-3ds-core-payment-integration.md
  - spec/tasks/3ds-integration/task-3ds-02-session-request-dtos-and-create-session.md
---

# Create ThreeDsSessionController POST /api/v1/3ds/sessions

## Local Context

- **Directory:** `3ds-engine/`
- **Files to create:**
  - `3ds-engine/src/main/java/com/acabouomony/engine/controller/ThreeDsSessionController.java` — REST controller for session creation
  - `3ds-engine/src/test/java/com/acabouomony/engine/controller/ThreeDsSessionControllerTest.java` — WebFlux integration tests
- **Files to modify:** none
- **Local dependencies:**
  - `ThreeDsSessionRequest`, `ThreeDsSessionResponse` records (from task-3ds-02)
  - `ChallengeSessionService.createSession` (from task-3ds-02)
  - `SecurityConfig.apiKeyFilter` — already configured; public paths are `GET /challenge/*` and `/actuator/**`; all other paths (including this one) require the `X-API-Key` header automatically

## Scope

1. Create `ThreeDsSessionController` as `@RestController`:
   - Single endpoint: `POST /api/v1/3ds/sessions`
   - Accepts `@Valid @RequestBody ThreeDsSessionRequest`
   - Delegates entirely to `ChallengeSessionService.createSession(request)`
   - Returns `Mono<ResponseEntity<ThreeDsSessionResponse>>` with HTTP 201 Created
   - No UUID generation, no URL building, no JWT signing — pure transport

2. Create `ThreeDsSessionControllerTest` using `@WebFluxTest(ThreeDsSessionController.class)`:
   - **Success:** Valid request with `X-API-Key` header → 201 with `challenge_id`, `acs_url`, `jwt` in body
   - **Missing API key:** Request without `X-API-Key` → 401 Unauthorized (handled by SecurityConfig)
   - **Missing required field:** Request with blank `transactionId` → 400 Bad Request (Bean Validation)
   - **Missing required field:** Request with null `amount` → 400 Bad Request
   - Use mocked `ChallengeSessionService` in tests

## Acceptance Criteria and Tests

- `POST /api/v1/3ds/sessions` with valid body and `X-API-Key` → 201 + `ThreeDsSessionResponse` body
- Missing or invalid `X-API-Key` → 401 (no business logic executed)
- Bean Validation on `ThreeDsSessionRequest` fields → 400 for blank/null required fields
- Controller never generates UUIDs, builds URLs, or signs JWTs — all of that is in `ChallengeSessionService`

**Verification:**
```bash
cd 3ds-engine && mvn test -Dtest="ThreeDsSessionControllerTest"
```

## Constraints and Negative Instructions

- Do NOT put any business logic in the controller — pure transport only
- Do NOT manually check the API key in the controller — `SecurityConfig.apiKeyFilter` handles it automatically
- Do NOT change `SecurityConfig` or add new security configuration
- Do NOT return 200 OK — session creation returns 201 Created
- Do NOT mock security in tests with `@WithMockUser` — use the real `SecurityConfig` and provide a valid test API key, or mock only the service layer
