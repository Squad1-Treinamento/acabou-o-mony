---
id: task-011
status: planned
links:
  - spec/tasks/index.md
  - spec/specs/spec-002-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-002-cache-and-idempotency-layer.md
  - spec/tasks/task-007-implement-idempotency-service.md
  - spec/tasks/task-008-implement-idempotency-interceptor.md
  - spec/tasks/task-009-apply-idempotency-to-endpoints.md
---

# Create Integration Tests for Idempotency Layer (Optional)

## Local Context

**Target Module:** Core Payment Service (Spring Boot application)

**Files to Create:**
- `src/y/core/controller/PaymentControllerIdempotencyTest.javtest/java/com/acabouomona` (integration test)
- `src/test/java/com/acabouomony/core/controller/RefundControllerIdempotencyTest.jest)
- `sava` (integration trc/test/java/com/acabouomony/core/service/IdempotencyServiceTest.java` (unit test)
- `src/test/java/com/acabouomony/core/interceptor/IdempotencyInterceptorTest.java` (unit test)

**Dependencies:**
- `@Testcontainers` (from testcontainers-junit-jupiter)
- `GenericContainer` (from testcontainers)
- `@SpringBootTest` (from spring-boot-test)
- `@WebMvcTest` (for controller tests)
- `MockMvc` (from spring-test)
- `IdempotencyService`, `IdempotencyInterceptor` (from main code)

**Test Scenarios:**
- First request: Process transaction, save to Redis
- Duplicate request: Return cached result, header `X-Idempotent-Replayed: true`
- Error caching: Failed transaction cached, duplicate returns same error
- Header validation: Invalid header returns HTTP 400
- TTL expiration: Cache expires after 24 hours (optional, requires time mocking)

## Scope

### Part 1: PaymentControllerIdempotencyTest

1. Create test class with `@Testcontainers`, `@SpringBootTest`, and `@AutoConfigureMockMvc`
2. Add Redis container: `GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379)`
3. Configure Spring to use test Redis container
4. Inject `MockMvc` for HTTP requests
5. Implement test: `testFirstRequestProcessesTransaction()`
   - Send POST request with `Idempotency-Key: req_test123`
   - Verify HTTP 200/201 response
   - Verify header `X-Idempotent-Replayed: false`
   - Verify transaction created in database
6. Implement test: `testDuplicateRequestReturnsCachedResult()`
   - Send POST request with `Idempotency-Key: req_test123` (first request)
   - Send same request again (duplicate)
   - Verify HTTP 200/201 response (same as first)
   - Verify header `X-Idempotent-Replayed: true`
   - Verify transaction NOT created again (database called only once)
7. Implement test: `testErrorCaching()`
   - Send POST request with invalid data (e.g., card declined)
   - Verify HTTP 402 (or error status)
   - Send same request again (duplicate)
   - Verify same error returned
   - Verify header `X-Idempotent-Replayed: true`
8. Implement test: `testInvalidHeaderFormat()`
   - Send POST request with `Idempotency-Key: invalid_format`
   - Verify HTTP 400 response
   - Verify error message "Idempotency-Key must start with 'req_'"
9. Implement test: `testMissingHeader()`
   - Send POST request without `Idempotency-Key` header
   - Verify HTTP 400 response
   - Verify error message "Idempotency-Key header is required"

### Part 2: RefundControllerIdempotencyTest

1. Create test class with same structure as `PaymentControllerIdempotencyTest`
2. Implement test: `testRefundIdempotency()`
   - Send POST request to `/api/v1/refunds` with idempotency key
g()`
     - Send duplicay cached result  - Send POST request with invate request
   - Verifreturned
3. Implement test: `testRefundErrorCachinlid refund data
   - Verify error cached and returned on duplicate

### Part 3: IdempotencyServiceTest (Unit Test)

1. Create unit test class with `@ExtendWith(MockitoExtension.class)`
2. Mock `ReactiveRedisTemplate`
3. Implement test: `testCheckIdempotencyReturnsEmptyWhenKeyNotFound()`
   - Mock Redis to return empty `Mono`
   - Call `checkIdempotency("req_test123")`
   - Verify empty `Mono` returned
4. Implement test: `testCheckIdempotencyReturnsResultWhenKeyExists()`
   - Mock Redis to return `TransactionResult`
   - Call `checkIdempotency("req_test123")`
   - Verify `TransactionResult` returned
5. Implement test: `testSaveIdempotencySavesToRedisWithTTL()`
   - Mock Redis `set()` operation
   - Call `saveIdempotency("req_test123", result)`
   - Verify Redis `set()` called with correct key, value, and TTL (24h)

### Part 4: IdempotencyInterceptorTest (Unit Test)

1. Create unit test class with `@ExtendWith(MockitoExtension.class)`
2. Mock `IdempotencyService`, `HttpServletRequest`, `HttpServletResponse`
3. Implement test: `testPreHandleValidatesHeaderFormat()`
   - Mock request with invalid header
   - Call `preHandle()`
   - Verify HTTP 400 response
4. Implement test: `testPreHandleReturnsCachedResult()`
   - Mock `IdempotencyService.checkIdempotency()` to return cached result
   - Call `preHandle()`
   - Verify cached result returned
   - Verify header `X-Idempotent-Replayed: true` added
5. Implement test: `testPreHandleAllowsRequestWhenKeyNotFound()`
   - Mock `IdempotencyService.checkIdempotency()` to return empty `Mono`
   - Call `preHandle()`
   - Verify request proceeds to controller (returns true)

**Implementation Notes:**
- Use `MockMvc.perform()` for HTTP requests
- Use `@DynamicPropertySource` to configure test Redis
- Use `verify()` to assert service method calls
eEach` to reset s- Use `StepVerifier` for reactive assertions
- Use `@Befortate between tests

## Acceptance Criteria and Tests

### Success Criteria

**AC-1: PaymentControllerIdempotencyTest**
- Test `testFirstRequestProcessesTransaction()` passes
- Test `testDuplicateRequestReturnsCachedResult()` passes
- Test `testErrorCaching()` passes
- Test `testInvalidHeaderFormat()` passes
- Test `testMissingHeader()` passes
- Code coverage > 80% for `PaymentController` idempotency logic

**AC-2: RefundControllerIdempotencyTest**
- Test `testRefundIdempotency()` passes
- Test `testRefundErrorCaching()` passes
- Code coverage > 80% for `RefundController` idempotency logic

**AC-3: IdempotencyServiceTest**
- Test `testCheckIdempotencyReturnsEmptyWhenKeyNotFound()` passes
- Test `testCheckIdempotencyReturnsResultWhenKeyExists()` passes
- Test `testSaveIdempotencySavesToRedisWithTTL()` passes
- Code coverage > 80% for `IdempotencyService`

**AC-4: IdempotencyInterceptorTest**
- Test `testPreHandleValidatesHeaderFormat()` passes
- Test `testPreHandleReturnsCachedResult()` passes
- Test `testPreHandleAllowsRequestWhenKeyNotFound()` passes
- Code coverage > 80% for `IdempotencyInterceptor`

**AC-5: Test Execution**
- All tests pass with `mvn test`
- Tests run in isolation (no shared state)
- Tests clean up Redis state after execution

### Failure Cases

- Tests fail if Redis container does not start
- Tests fail if idempotency interceptor is not registered
- Tests fail if header validation is incorrect
- Tests fail if error caching does not work

### Validation Commands

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PaymentControllerIdempotencyTest

# Run with coverage report
mvn test jacoco:report

# Check coverage report
open target/site/jacoco/index.html
```

## Constraints and Negative Instructions

**DO:**
- Use `@Testcontainers` for integration tests
- Use `MockMvc` for HTTP requests
- Use `@DynamicPropertySource` to configure test Redis
- Use `verify()` to assert method calls
- Use `StepVerifier` for reactive assertions
- Clean up Redis state between tests

**DO NOT:**
- Use real Redis instance (always use Testcontainers)
- Share state between tests
- Hard-code Redis host/port
- Skip cleanup after tests
- Test implementation details (test behavior)

**Out of Scope:**
- Performance tests (load testing)
- End-to-end tests (full API tests)
- Cache layer tests - task-010
- Encryption tests - task-015
