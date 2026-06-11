---
id: task-009
status: planned
links:
  - spec/tasks/index.md
  - spec/specs/spec-002-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-002-cache-and-idempotency-layer.md
  - spec/tasks/task-007-implement-idempotency-service.md
  - spec/tasks/task-008-implement-idempotency-interceptor.md
---

# Apply Idempotency Interceptor to Mutation Endpoints

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Modify:**
- `src/main/java/com/acabouomony/core/config/WebConfig.java` (Spring Web configuration)
- `src/main/java/com/acabouomony/core/controller/PaymentController.java` (existing controller)
- `src/main/java/com/acabouomony/core/controller/RefundController.java` (existing controller, if exists)

**Dependencies:**
- `IdempotencyInterceptor` (from task-008)
- `WebMvcConfigurer` (from Spring Web)
- `InterceptorRegistry` (from Spring Web)

**Endpoints to Apply Idempotency:**
- `POST /api/v1/payments` (create transaction)
- `POST /api/v1/refunds` (create refund)
- `POST /api/v1/payments/{id}/capture` (capture pre-authorization)
- `POST /api/v1/payments/3ds-callback` (3DS callback)

**Endpoints to Exclude:**
- All GET requests (read-only, no idempotency needed)
- Health check endpoints (`/actuator/health`, etc.)

## Scope

1. Open or create `WebConfig.java` class in `com.acabouomony.core.config` package
2. Annotate class with `@Configuration`
3. Implement `WebMvcConfigurer` interface
4. Inject `IdempotencyInterceptor` via constructor
5. Override `addInterceptors(InterceptorRegistry registry)` method:
   - Register `IdempotencyInterceptor`
   - Add path patterns for mutation endpoints:
     - `/api/v1/payments` (POST only)
     - `/api/v1/refunds` (POST only)
     - `/api/v1/payments/*/capture` (POST only)
     - `/api/v1/payments/3ds-callback` (POST only)
   - Exclude GET requests and health checks
6. Verify interceptor is applied to correct endpoints
7. Verify idempotency works end-to-end for all mutation endpoints

**Implementation Notes:**
- Use `registry.addInterceptor(idempotencyInterceptor).addPathPatterns(...)`
- Use wildcard `*` for dynamic path segments (e.g., `/payments/*/capture`)
- Exclude patterns with `.excludePathPatterns("/actuator/**", "/api/v1/*/GET")`
- Interceptor should only apply to POST requests (not GET, PUT, DELETE)

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: WebConfig Class Structure**
- Class `WebConfig` exists in package `com.acabouomony.core.config`
- Class is annotated with `@Configuration`
- Class implements `WebMvcConfigurer`
- Constructor injects `IdempotencyInterceptor`

**AC-2: Interceptor Registration**
- Method `addInterceptors(InterceptorRegistry registry)` is overridden
- `IdempotencyInterceptor` is registered with `registry.addInterceptor()`
- Path patterns include:
  - `/api/v1/payments` (POST)
  - `/api/v1/refunds` (POST)
  - `/api/v1/payments/*/capture` (POST)
  - `/api/v1/payments/3ds-callback` (POST)
- Excluded patterns include:
  - `/actuator/**` (health checks)
  - GET requests (read-only)

**AC-3: POST /api/v1/payments Idempotency**
- First request with `Idempotency-Key: req_abc123` creates transaction
- Second request with same key returns cached result
- Header `X-Idempotent-Replayed: true` present on second request
- Transaction is not processed twice (no double charge)

**AC-4: POST /api/v1/refunds Idempotency**
- First request with `Idempotency-Key: req_refund123` creates refund
- Second request with same key returns cached result
- Header `X-Idempotent-Replayed: true` present on second request
- Refund is not processed twice

**AC-5: POST /api/v1/payments/{id}/capture Idempotency**
- First request with `Idempotency-Key: req_capture123` captures pre-authorization
- Second request with same key returns cached result
- Header `X-Idempotent-Replayed: true` present on second request
- Capture is not processed twice

**AC-6: POST /api/v1/payments/3ds-callback Idempotency**
- First request with `Idempotency-Key: req_callback123` processes 3DS callback
- Second request with same key returns cached result
- Header `X-Idempotent-Replayed: true` present on second request
- Callback is not processed twice

**AC-7: GET Requests Not Affected**
- GET requests do not require `Idempotency-Key` header
- GET requests are not intercepted by `IdempotencyInterceptor`
- GET requests work normally without idempotency logic

### Failure Cases

- HTTP 400 if `Idempotency-Key` header is missing on mutation endpoints
- HTTP 400 if `Idempotency-Key` format is invalid
- Interceptor not applied if path pattern is incorrect
- Interceptor applied to GET requests (should not happen)

### Validation Commands

```bash
# Start application and Redis
docker-compose up -d redis
mvn spring-boot:run

# Test POST /api/v1/payments idempotency
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Idempotency-Key: req_payment123" \
  -d '{"amount":1500,"currency":"BRL","merchant_id":"m_123"}'
# Expected: HTTP 200/201, header "X-Idempotent-Replayed: false"

curl -X POST http://localhost:8080/api/v1/payments \
  -H "Idempotency-Key: req_payment123" \
  -d '{"amount":1500,"currency":"BRL","merchant_id":"m_123"}'
# Expected: HTTP 200/201, same response, header "X-Idempotent-Replayed: true"

# Test POST /api/v1/refunds idempotency
curl -X POST http://localhost:8080/api/v1/refunds \
  -H "Idempotency-Key: req_refund123" \
  -d '{"transaction_id":"tx_001","amount":1500}'
# Expected: HTTP 200/201, header "X-Idempotent-Replayed: false"

curl -X POST http://localhost:8080/api/v1/refunds \
  -H "Idempotency-Key: req_refund123" \
  -d '{"transaction_id":"tx_001","amount":1500}'
# Expected: HTTP 200/201, same response, header "X-Idempotent-Replayed: true"

# Test POST /api/v1/payments/{id}/capture idempotency
curl -X POST http://localhost:8080/api/v1/payments/tx_001/capture \
  -H "Idempotency-Key: req_capture123" \
  -d '{"amount":1500}'
# Expected: HTTP 200/201, header "X-Idempotent-Replayed: false"

curl -X POST http://localhost:8080/api/v1/payments/tx_001/capture \
  -H "Idempotency-Key: req_capture123" \
  -d '{"amount":1500}'
# Expected: HTTP 200/201, same response, header "X-Idempotent-Replayed: true"

# Test GET request (should not require idempotency key)
curl -X GET http://localhost:8080/api/v1/payments/tx_001
# Expected: HTTP 200, no "X-Idempotent-Replayed" header

# Test missing header (should fail)
curl -X POST http://localhost:8080/api/v1/payments \
  -d '{"amount":1500,"currency":"BRL"}'
# Expected: HTTP 400, error message "Idempotency-Key header is required"
```

### Optional Integration Tests

If integration tests are desired:
- Test idempotency for each endpoint (payments, refunds, capture, callback)
- Test duplicate requests return cached results
- Test error caching (failed transactions)
- Test GET requests are not affected
- Test missing/invalid headers return HTTP 400

## Constraints and Negative Instructions

**DO:**
- Implement `WebMvcConfigurer` interface
- Register interceptor with `registry.addInterceptor()`
- Use path patterns for mutation endpoints only
- Exclude GET requests and health checks
- Verify interceptor is applied to correct endpoints

**DO NOT:**
- Apply interceptor to GET requests (read-only)
- Apply interceptor to health check endpoints (`/actuator/**`)
- Hard-code path patterns (use constants if available)
- Skip path pattern validation (test all endpoints)
- Implement transaction processing logic (handled in controllers)

**Out of Scope:**
- Controller implementation (transaction processing) - assumed to exist
- Integration tests - optional
- Metrics/observability - future work
