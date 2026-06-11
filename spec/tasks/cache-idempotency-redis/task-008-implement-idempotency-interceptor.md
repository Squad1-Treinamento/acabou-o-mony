---
id: task-008
status: planned
links:
  - spec/tasks/index.md
  - spec/specs/spec-002-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-002-cache-and-idempotency-layer.md
  - spec/tasks/task-007-implement-idempotency-service.md
---

# Implement IdempotencyInterceptor for Request Validation

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Create:**
- `src/main/java/com/acabouomony/core/interceptor/IdempotencyInterceptor.java` (new interceptor class)

**Dependencies:**
- `IdempotencyService` (from task-007)
- `HandlerInterceptor` (from Spring Web)
- `HttpServletRequest`, `HttpServletResponse` (from javax.servlet)
- `TransactionResult` (domain model, assumed to exist)

**HTTP Headers:**
- Request header: `Idempotency-Key` (required for mutation endpoints)
- Response header: `X-Idempotent-Replayed` (true/false)

**Validation Rules:**
- Header must be present for mutation endpoints (POST, PUT, DELETE)
- Header format: Must start with `req_` (e.g., `req_abc123`)
- Header length: Max 256 characters
- Header characters: Alphanumeric, hyphens, underscores only

## Scope

1. Create package `com.acabouomony.core.interceptor` (if not exists)
2. Create `IdempotencyInterceptor.java` class implementing `HandlerInterceptor`
3. Inject `IdempotencyService` via constructor
4. Implement `preHandle()` method:
   - Extract `Idempotency-Key` header from request
   - Validate header format (starts with `req_`, max 256 chars, valid characters)
   - If invalid, return HTTP 400 with error message
   - Check if idempotency key exists in Redis via `IdempotencyService.checkIdempotency()`
   - If exists (cache hit):
     - Return cached `TransactionResult` immediately
     - Add header `X-Idempotent-Replayed: true`
     - Skip controller execution (return false)
   - If not exists (cache miss):
     - Allow request to proceed to controller (return true)
     - Store idempotency key in request attribute for later use
5. Implement `afterCompletion()` method:
   - After successful transaction, save result to Redis via `IdempotencyService.saveIdempotency()`
   - Add header `X-Idempotent-Replayed: false`
6. Register interceptor in Spring Web configuration
7. Verify interceptor validates headers and prevents duplicate processing

**Implementation Notes:**
- Use regex for header validation: `^req_[a-zA-Z0-9_-]{1,250}$`
- Store idempotency key in request attribute: `request.setAttribute("idempotencyKey", key)`
- Return cached result with same HTTP status and body as original request
- Only apply to mutation endpoints (POST, PUT, DELETE)

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: Interceptor Class Structure**
- Class `IdempotencyInterceptor` exists in package `com.acabouomony.core.interceptor`
- Class implements `HandlerInterceptor`
- Constructor injects `IdempotencyService`

**AC-2: Header Validation**
- Validates `Idempotency-Key` header is present for mutation endpoints
- Validates header starts with `req_`
- Validates header length ≤ 256 characters
- Validates header contains only alphanumeric, hyphens, underscores
- Returns HTTP 400 if validation fails with error message:
  - `"Idempotency-Key header is required for mutation requests"`
  - `"Idempotency-Key must start with 'req_'"`
  - `"Idempotency-Key must be ≤ 256 characters"`
  - `"Idempotency-Key contains invalid characters"`

**AC-3: Idempotency Check (Cache Hit)**
- If idempotency key exists in Redis:
  - Returns cached `TransactionResult` immediately
  - Adds header `X-Idempotent-Replayed: true`
  - Returns same HTTP status and body as original request
  - Does not execute controller logic
  - Latency < 5ms (p95)

**AC-4: Idempotency Check (Cache Miss)**
- If idempotency key does not exist in Redis:
  - Allows request to proceed to controller
  - Stores idempotency key in request attribute
  - Controller processes transaction normally
  - After successful transaction, saves result to Redis
  - Adds header `X-Idempotent-Replayed: false`

**AC-5: Error Caching**
- If transaction fails (e.g., card declined, HTTP 402):
  - Error response is also cached in Redis
  - Subsequent requests with same idempotency key return cached error
  - Adds header `X-Idempotent-Replayed: true`

**AC-6: Interceptor Registration**
- Interceptor is registered in Spring Web configuration
- Applied only to mutation endpoints:
  - `POST /api/v1/payments`
  - `POST /api/v1/refunds`
  - `POST /api/v1/payments/{id}/capture`
  - `POST /api/v1/payments/3ds-callback`
- Not applied to GET requests (read-only)

### Failure Cases

- HTTP 400 if `Idempotency-Key` header is missing
- HTTP 400 if `Idempotency-Key` format is invalid
- Cached error returned if original transaction failed
- Redis unavailable → process transaction normally (no cached result to check)

### Validation Commands

```bash
# Start application and Redis
docker-compose up -d redis
mvn spring-boot:run

# Test missing header (should fail)
curl -X POST http://localhost:8080/api/v1/payments \
  -d '{"amount":1500,"currency":"BRL"}'
# Expected: HTTP 400, error message "Idempotency-Key header is required"

# Test invalid format (should fail)
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Idempotency-Key: invalid_format" \
  -d '{"amount":1500,"currency":"BRL"}'
# Expected: HTTP 400, error message "Idempotency-Key must start with 'req_'"

# Test valid first request (should succeed)
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Idempotency-Key: req_test123" \
  -d '{"amount":1500,"currency":"BRL"}'
# Expected: HTTP 200/201, header "X-Idempotent-Replayed: false"

# Test duplicate request (should return cached result)
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Idempotency-Key: req_test123" \
  -d '{"amount":1500,"currency":"BRL"}'
# Expected: HTTP 200/201, same response body, header "X-Idempotent-Replayed: true"

# Test error caching (card declined)
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Idempotency-Key: req_declined456" \
  -d '{"amount":99999999,"currency":"BRL"}'
# Expected: HTTP 402 (or error status), error message

# Test duplicate error request
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Idempotency-Key: req_declined456" \
  -d '{"amount":99999999,"currency":"BRL"}'
# Expected: HTTP 402 (same error), header "X-Idempotent-Replayed: true"
```

### Optional Unit Tests

If unit tests are desired:
- Test header validation (missing, invalid format, too long)
- Test cache hit returns cached result
- Test cache miss proceeds to controller
- Test error caching (failed transactions)
- Mock `IdempotencyService` for isolated testing

## Constraints and Negative Instructions

**DO:**
- Implement `HandlerInterceptor` interface
- Validate header format with regex: `^req_[a-zA-Z0-9_-]{1,250}$`
- Return HTTP 400 for validation errors
- Add header `X-Idempotent-Replayed` to all responses
- Cache both success and error responses
- Store idempotency key in request attribute for controller access

**DO NOT:**
- Skip header validation (always validate format)
- Cache HTTP 400 validation errors (only cache business logic errors)
- Expose internal error details in HTTP 400 messages
- Apply interceptor to GET requests (read-only, no idempotency needed)
- Implement transaction processing logic (handled in controller)

**Out of Scope:**
- Controller implementation (transaction processing) - task-009
- Endpoint registration - task-009
- Integration tests - optional
