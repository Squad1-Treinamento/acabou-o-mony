---
id: task-005
status: completed
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-005-3ds-core-payment-integration.md
  - spec/specs/spec-005-3ds-core-payment-integration.md
  - spec/tasks/3ds-integration/task-003-session-controller.md
  - spec/tasks/3ds-integration/task-004-wire-payment-controller.md
---

# ThreeDsClient, challengeId column, and PaymentResponseDTO update

## Local Context

- **Directory:** `core-payment/`
- **Files to create:**
  - `core-payment/src/main/java/com/acabouomony/payment/infrastructure/client/dto/ThreeDsSessionRequestDTO.java` — outbound request to 3DS Engine
  - `core-payment/src/main/java/com/acabouomony/payment/infrastructure/client/dto/ThreeDsSessionResponseDTO.java` — inbound response from 3DS Engine
  - `core-payment/src/main/java/com/acabouomony/payment/infrastructure/client/ThreeDsClient.java` — HTTP client for 3DS Engine
  - `core-payment/src/test/java/com/acabouomony/payment/infrastructure/client/ThreeDsClientTest.java` — WireMock tests
  - `core-payment/src/main/resources/db/migration/V4__add_challenge_id_to_transactions.sql` — Flyway migration

- **Files to modify:**
  - `core-payment/src/main/java/com/acabouomony/payment/domain/entity/Transaction.java` — add `challengeId` and `challengeAcsUrl` fields
  - `core-payment/src/main/java/com/acabouomony/payment/web/dto/PaymentResponseDTO.java` — add `challengeId` and `acsUrl` fields
  - `core-payment/src/main/java/com/acabouomony/payment/infrastructure/config/AppConfig.java` — configure `RestTemplate` with timeouts for `ThreeDsClient`
  - `core-payment/src/main/java/com/acabouomony/payment/domain/service/PaymentOrchestrationService.java` — inject `ThreeDsClient`, call it in HIGH-risk branch of `processPayment()`, store `challengeId` and `challengeAcsUrl` on transaction

- **Local dependencies:**
  - `RestTemplate` bean from `AppConfig` — already exists, needs timeout configuration
  - `RiskLevel` enum — already exists at `core-payment/src/main/java/com/acabouomony/payment/domain/model/RiskLevel.java`
  - `PaymentAcquirerException` — already exists at `core-payment/src/main/java/com/acabouomony/payment/domain/exception/PaymentAcquirerException.java`, thrown on HIGH-risk 3DS timeout
  - `PaymentStatus.CHALLENGE_PENDING` — already in enum
  - `3ds.base-url` and `3ds.api-key` — new `@Value` properties to add to `application.properties`

## Scope

1. Create `ThreeDsSessionRequestDTO` in `com.acabouomony.payment.infrastructure.client.dto`:
   - Lombok annotations: `@Data @NoArgsConstructor @AllArgsConstructor @Builder`
   - Fields with `@JsonProperty` snake_case: `transactionId`, `merchantId`, `amount` (`long`), `currency`, `cardToken`

2. Create `ThreeDsSessionResponseDTO` in `com.acabouomony.payment.infrastructure.client.dto`:
   - Lombok annotations: `@Data @NoArgsConstructor @AllArgsConstructor @Builder`
   - Fields: `challengeId` (`@JsonProperty("challenge_id")`), `acsUrl` (`@JsonProperty("acs_url")`), `jwt`

3. Configure `RestTemplate` in `AppConfig` with timeouts:
   - Update existing `restTemplate()` bean to use `SimpleClientHttpRequestFactory` with `connectTimeout = 2000` ms and `readTimeout = 5000` ms

4. Create `ThreeDsClient` as `@Service` in `com.acabouomony.payment.infrastructure.client`:
   - Constructor: inject `RestTemplate restTemplate`, `@Value("${3ds.base-url}") String threeDsBaseUrl`, `@Value("${3ds.api-key}") String apiKey`
   - Method: `public Optional<ThreeDsSessionResponseDTO> createSession(ThreeDsSessionRequestDTO request, RiskLevel risk)`:
     - Sets `X-API-Key: {apiKey}` header on every request
     - POSTs to `{threeDsBaseUrl}/api/v1/3ds/sessions`
     - On success: returns `Optional.of(response.getBody())`
     - On `ResourceAccessException` (timeout/connection refused): 
       - if `risk == RiskLevel.LOW` → return `Optional.empty()` (frictionless fallback)
       - if `risk == RiskLevel.HIGH` → throw `new PaymentAcquirerException("3DS service unavailable")`

5. Add DB migration `V4__add_challenge_id_to_transactions.sql`:
   ```sql
   ALTER TABLE transactions ADD COLUMN challenge_id VARCHAR(36);
   ALTER TABLE transactions ADD COLUMN challenge_acs_url VARCHAR(512);
   ```

6. Add `challengeId` and `challengeAcsUrl` fields to `Transaction` entity:
   - `@Column(name = "challenge_id", length = 36) private String challengeId;`
   - `@Column(name = "challenge_acs_url", length = 512) private String challengeAcsUrl;`

7. Add `challengeId` and `acsUrl` fields to `PaymentResponseDTO`:
   - `@JsonProperty("challenge_id") private String challengeId;`
   - `@JsonProperty("acs_url") private String acsUrl;`

8. Wire `ThreeDsClient` into `PaymentOrchestrationService.processPayment()` HIGH-risk branch:
   - Inject `ThreeDsClient threeDsClient` via constructor
   - In HIGH-risk branch: build `ThreeDsSessionRequestDTO` from transaction fields
   - Call `threeDsClient.createSession(requestDTO, RiskLevel.HIGH)`
   - If `Optional.isPresent()`: store `challengeId` and `acsUrl` on transaction via setters, then `transitionState(transaction, PaymentStatus.CHALLENGE_PENDING, "system")` and return
   - If `Optional.isEmpty()` (frictionless fallback): proceed to PROCESSING path

9. Create `ThreeDsClientTest` using WireMock:
   - **Success:** WireMock returns 201 + valid JSON → `Optional.isPresent()` with correct fields
   - **Timeout + LOW risk:** WireMock delays > 5s → returns `Optional.empty()`
   - **Timeout + HIGH risk:** WireMock delays > 5s → throws `PaymentAcquirerException`
   - **Connection refused + HIGH risk:** WireMock not started → throws `PaymentAcquirerException`
   - **API key sent:** verify `X-API-Key` header is present on every request (WireMock verification)

## Acceptance Criteria and Tests

- **Success:** `ThreeDsClient.createSession` with successful 3DS Engine response → `Optional.of(response)`
- **Timeout + LOW risk:** `ThreeDsClient.createSession` with timeout → `Optional.empty()`
- **Timeout + HIGH risk:** `ThreeDsClient.createSession` with timeout → `PaymentAcquirerException`
- **API key header:** `X-API-Key` header present on every outbound call
- **Transaction fields:** `Transaction.challengeId` and `Transaction.challengeAcsUrl` populated after HIGH-risk `processPayment()`
- **Response DTO:** `PaymentResponseDTO.challengeId` and `PaymentResponseDTO.acsUrl` available for controller to use
- **Migration:** Flyway migration runs without errors (check via test startup)

**Verification:**
```bash
cd core-payment && mvn test -Dtest="ThreeDsClientTest"
```

## Constraints and Negative Instructions

- Do NOT use `WebClient` or any reactive types — use `RestTemplate` only
- Do NOT hardcode the API key or base URL — use `@Value` properties
- Do NOT call `ThreeDsClient` for LOW-risk transactions in `processPayment()` — the 3DS call happens only in the HIGH-risk branch
- Do NOT change the `transitionState()` method — it remains private and unchanged
- Do NOT add `challenge_id` or `challenge_acs_url` as NOT NULL columns — both must be nullable (most transactions don't go through 3DS)
- Do NOT update the migration version beyond V4 — the current sequence is V1, V2, V3, so V4 is the correct next version
