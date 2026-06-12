package com.acabouomony.payment;

import com.acabouomony.payment.domain.dto.PaymentResult;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.PaymentAcquirerClient;
import com.acabouomony.payment.domain.service.PaymentOrchestrationService;
import com.acabouomony.payment.domain.service.RiskEvaluationService;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Integration tests for the Core-Payment ↔ 3DS Engine ↔ Database flow.
 *
 * Uses:
 * - Real PostgreSQL (database-payment container, localhost:5434/payments_test)
 * - MockRestServiceServer to stub the 3DS Engine HTTP API
 * - @MockBean for PaymentAcquirerClient and RiskEvaluationService
 * - No Redis required (excluded via application-integration.yml)
 *
 * Covers the full 3DS lifecycle:
 *   CREATED → CHALLENGE_PENDING → AUTHENTICATED → PROCESSING → COMPLETED / DECLINED
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
class ThreeDsCoreIntegrationTest {

    @MockBean
    private RiskEvaluationService riskEvaluationService;

    @MockBean
    private PaymentAcquirerClient paymentAcquirerClient;

    @Autowired
    private PaymentOrchestrationService orchestrationService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockRestServiceServer mockServer;

    private final List<UUID> createdTxIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer
                .bindTo(restTemplate)
                .ignoreExpectOrder(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        mockServer.reset();
        if (!createdTxIds.isEmpty()) {
            String ids = createdTxIds.stream()
                    .map(id -> "'" + id + "'")
                    .reduce((a, b) -> a + "," + b)
                    .orElse("''");
            jdbcTemplate.execute("DELETE FROM audit_logs WHERE transaction_id IN (" + ids + ")");
            jdbcTemplate.execute("DELETE FROM outbox_events WHERE aggregate_id IN (" + ids + ")");
            jdbcTemplate.execute("DELETE FROM transactions WHERE id IN (" + ids + ")");
            createdTxIds.clear();
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Test 1 — HIGH-risk: 3DS session created, transaction persisted
    // ────────────────────────────────────────────────────────────────

    @Test
    void highRiskPayment_3dsSessionCreated_persistedAsChallengePending() {
        UUID txId = UUID.randomUUID();
        Transaction tx = saveCreated(txId);

        mockServer.expect(requestTo(containsString("/api/v1/3ds/sessions")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "test-api-key"))
                .andExpect(jsonPath("$.transaction_id").value(txId.toString()))
                .andExpect(jsonPath("$.amount").value(15000))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                            {
                              "challenge_id": "ch-abc123",
                              "acs_url": "http://3ds.test/challenge/ch-abc123",
                              "jwt": "eyJ.test.token"
                            }
                        """));

        when(riskEvaluationService.isHighRisk(any())).thenReturn(true);

        orchestrationService.processPayment(tx);

        Transaction saved = transactionRepository.findById(txId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.CHALLENGE_PENDING);
        assertThat(saved.getChallengeId()).isEqualTo("ch-abc123");
        assertThat(saved.getChallengeAcsUrl()).isEqualTo("http://3ds.test/challenge/ch-abc123");

        mockServer.verify();
    }

    // ────────────────────────────────────────────────────────────────
    // Test 2 — LOW-risk: bypasses 3DS, goes straight to acquirer
    // ────────────────────────────────────────────────────────────────

    @Test
    void lowRiskPayment_no3dsCall_persistedAsCompleted() {
        UUID txId = UUID.randomUUID();
        Transaction tx = saveCreated(txId);

        when(riskEvaluationService.isHighRisk(any())).thenReturn(false);
        when(paymentAcquirerClient.submitPayment(any())).thenReturn(
                completedResult("mp-ref-low-001"));

        orchestrationService.processPayment(tx);

        Transaction saved = transactionRepository.findById(txId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(saved.getChallengeId()).isNull();

        mockServer.verify();
    }

    // ────────────────────────────────────────────────────────────────
    // Test 3 — Approved callback → full async flow → COMPLETED in DB
    // ────────────────────────────────────────────────────────────────

    @Test
    void callbackApproved_asyncFinalizer_persistedAsCompleted() {
        UUID txId = UUID.randomUUID();
        saveChallengePending(txId, "ch-approved");

        when(paymentAcquirerClient.submitPayment(any())).thenReturn(
                completedResult("mp-ref-approved-001"));

        orchestrationService.completeThreeDsAuthentication(txId, true);

        // AUTHENTICATED is committed synchronously; wait for async finalizer to push COMPLETED
        await().atMost(5, SECONDS).untilAsserted(() -> {
            Transaction persisted = transactionRepository.findById(txId).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        });

        verify(paymentAcquirerClient, times(1)).submitPayment(any());
    }

    // ────────────────────────────────────────────────────────────────
    // Test 4 — Declined callback → DECLINED, acquirer never called
    // ────────────────────────────────────────────────────────────────

    @Test
    void callbackDeclined_persistedAsDeclined_noAcquirerCall() {
        UUID txId = UUID.randomUUID();
        saveChallengePending(txId, "ch-declined");

        orchestrationService.completeThreeDsAuthentication(txId, false);

        Transaction persisted = transactionRepository.findById(txId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        verify(paymentAcquirerClient, never()).submitPayment(any());
    }

    // ────────────────────────────────────────────────────────────────
    // Test 5 — Duplicate callback is idempotent
    // ────────────────────────────────────────────────────────────────

    @Test
    void duplicateCallback_idempotent_acquirerCalledOnlyOnce() {
        UUID txId = UUID.randomUUID();
        saveChallengePending(txId, "ch-dup");

        when(paymentAcquirerClient.submitPayment(any())).thenReturn(
                completedResult("mp-ref-dup-001"));

        orchestrationService.completeThreeDsAuthentication(txId, true);
        await().atMost(5, SECONDS).untilAsserted(() -> {
            Transaction p = transactionRepository.findById(txId).orElseThrow();
            assertThat(p.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        });

        // Duplicate callback — idempotency guard in completeThreeDsAuthentication
        orchestrationService.completeThreeDsAuthentication(txId, true);

        Transaction persisted = transactionRepository.findById(txId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentAcquirerClient, times(1)).submitPayment(any());
    }

    // ────────────────────────────────────────────────────────────────
    // Test 6 — 3DS request body contains required fields and API key
    // ────────────────────────────────────────────────────────────────

    @Test
    void highRiskPayment_requestBody_containsAllMandatoryFields() {
        UUID txId = UUID.randomUUID();
        Transaction tx = saveCreated(txId);

        mockServer.expect(requestTo(containsString("/api/v1/3ds/sessions")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "test-api-key"))
                .andExpect(jsonPath("$.transaction_id").value(txId.toString()))
                .andExpect(jsonPath("$.merchant_id").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(15000))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                            {"challenge_id":"ch-fields","acs_url":"http://acs/ch-fields","jwt":"tok"}
                        """));

        when(riskEvaluationService.isHighRisk(any())).thenReturn(true);

        orchestrationService.processPayment(tx);

        mockServer.verify();
        Transaction saved = transactionRepository.findById(txId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.CHALLENGE_PENDING);
    }

    // ────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────

    private Transaction saveCreated(UUID id) {
        createdTxIds.add(id);
        Transaction tx = Transaction.builder()
                .id(id)
                .merchantId(UUID.randomUUID())
                .idempotencyKey(UUID.randomUUID())
                .amount(15000L)
                .currency("BRL")
                .status(PaymentStatus.CREATED)
                .payloadHash("hash-" + id)
                .cardTokenId("card-tok-001")
                .version(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return transactionRepository.save(tx);
    }

    private void saveChallengePending(UUID id, String challengeId) {
        createdTxIds.add(id);
        Transaction tx = Transaction.builder()
                .id(id)
                .merchantId(UUID.randomUUID())
                .idempotencyKey(UUID.randomUUID())
                .amount(15000L)
                .currency("BRL")
                .status(PaymentStatus.CHALLENGE_PENDING)
                .payloadHash("hash-" + id)
                .challengeId(challengeId)
                .version(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        transactionRepository.save(tx);
    }

    private PaymentResult completedResult(String ref) {
        return PaymentResult.builder()
                .status(PaymentStatus.COMPLETED)
                .acquirerReference(ref)
                .timestamp(Instant.now())
                .build();
    }
}
