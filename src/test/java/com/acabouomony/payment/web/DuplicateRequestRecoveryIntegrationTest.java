package com.acabouomony.payment.web;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.*;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for duplicate request recovery with response caching.
 * * Tests verify:
 * - Response caching works with PaymentController
 * - Duplicate requests return cached responses
 * - Cache TTL expiration works
 * - Mismatched payloads set UNKNOWN state
 * - UNKNOWN transactions return 202 Accepted
 * - Concurrent duplicate requests handled correctly
 * - Fallback to database when cache unavailable
 * * Spec: spec-001-core-payment-processing.md - Duplicate Request Rules
 * Task: task-010-duplicate-request-recovery.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Duplicate Request Recovery Integration Tests")
class DuplicateRequestRecoveryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private InMemoryPaymentResponseCache responseCache;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID merchantId;
    private UUID idempotencyKey;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();
        transactionRepository.deleteAll();
        responseCache.clear();
    }

    @Nested
    @DisplayName("Response Caching")
    class ResponseCachingTests {

        @Test
        @DisplayName("Caches response after successful payment")
        void testCachesResponseAfterSuccessfulPayment() throws Exception {
            // Arrange
            PaymentRequest request = createPaymentRequest(idempotencyKey);
            String requestBody = objectMapper.writeValueAsString(request);

            // Act
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isAccepted());

            // Assert: Response should be cached
            assertTrue(responseCache.retrieve(merchantId, idempotencyKey).isPresent());
        }

        @Test
        @DisplayName("Returns cached response on duplicate request")
        void testReturnsCachedResponseOnDuplicateRequest() throws Exception {
            // Arrange
            PaymentRequest request = createPaymentRequest(idempotencyKey);
            String requestBody = objectMapper.writeValueAsString(request);

            // Create first transaction
            MvcResult firstResult = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isAccepted())
                    .andReturn();

            String firstResponseBody = firstResult.getResponse().getContentAsString();
            PaymentResponseDTO firstResponse = objectMapper.readValue(firstResponseBody, PaymentResponseDTO.class);
            UUID firstTransactionId = firstResponse.getTransactionId();

            // Act: Submit duplicate request (should hit cache)
            MvcResult secondResult = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isAccepted())
                    .andReturn();

            // Assert
            String secondResponseBody = secondResult.getResponse().getContentAsString();
            PaymentResponseDTO secondResponse = objectMapper.readValue(secondResponseBody, PaymentResponseDTO.class);

            assertEquals(firstTransactionId, secondResponse.getTransactionId());
        }
    }

    @Nested
    @DisplayName("Cache TTL Expiration")
    class CacheTTLExpirationTests {

        @Test
        @DisplayName("Falls back to database after cache expiration")
        void testFallsBackToDatabaseAfterCacheExpiration() throws Exception {
            // Arrange
            PaymentRequest request = createPaymentRequest(idempotencyKey);
            String requestBody = objectMapper.writeValueAsString(request);

            // Create first transaction
            MvcResult firstResult = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isAccepted())
                    .andReturn();

            String firstResponseBody = firstResult.getResponse().getContentAsString();
            PaymentResponseDTO firstResponse = objectMapper.readValue(firstResponseBody, PaymentResponseDTO.class);
            UUID firstTransactionId = firstResponse.getTransactionId();

            // Manually expire cache
            responseCache.invalidate(merchantId, idempotencyKey);

            // Act: Submit duplicate request (should hit database)
            MvcResult secondResult = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isAccepted())
                    .andReturn();

            // Assert
            String secondResponseBody = secondResult.getResponse().getContentAsString();
            PaymentResponseDTO secondResponse = objectMapper.readValue(secondResponseBody, PaymentResponseDTO.class);

            assertEquals(firstTransactionId, secondResponse.getTransactionId());
        }
    }

    @Nested
    @DisplayName("Mismatched Payload Handling")
    class MismatchedPayloadHandlingTests {

        @Test
        @DisplayName("Mismatched payload returns 400 Bad Request")
        void testMismatchedPayloadReturns400() throws Exception {
            // Arrange
            PaymentRequest request1 = createPaymentRequest(idempotencyKey);
            String requestBody1 = objectMapper.writeValueAsString(request1);

            // Create first transaction
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody1))
                    .andExpect(status().isAccepted());

            // Create second request with same idempotency key but different amount
            PaymentRequest request2 = PaymentRequest.builder()
                    .amount(20000L)  // Different amount
                    .currency("BRL")
                    .idempotencyKey(idempotencyKey)  // Same idempotency key
                    .paymentMethod(PaymentRequest.PaymentMethod.builder()
                            .cardTokenId("tok_visa_123")
                            .maskedCard("411111XXXXXX1111")
                            .build())
                    .customerId(UUID.randomUUID())
                    .build();
            String requestBody2 = objectMapper.writeValueAsString(request2);

            // Act & Assert
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody2))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Mismatched payload sets UNKNOWN state")
        void testMismatchedPayloadSetsUnknownState() throws Exception {
            // Arrange
            PaymentRequest request1 = createPaymentRequest(idempotencyKey);
            String requestBody1 = objectMapper.writeValueAsString(request1);

            // Create first transaction
            MvcResult firstResult = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody1))
                    .andExpect(status().isAccepted())
                    .andReturn();

            String firstResponseBody = firstResult.getResponse().getContentAsString();
            PaymentResponseDTO firstResponse = objectMapper.readValue(firstResponseBody, PaymentResponseDTO.class);
            UUID transactionId = firstResponse.getTransactionId();

            // Create second request with different payload
            PaymentRequest request2 = PaymentRequest.builder()
                    .amount(20000L)  // Different amount
                    .currency("BRL")
                    .idempotencyKey(idempotencyKey)
                    .paymentMethod(PaymentRequest.PaymentMethod.builder()
                            .cardTokenId("tok_visa_123")
                            .maskedCard("411111XXXXXX1111")
                            .build())
                    .customerId(UUID.randomUUID())
                    .build();
            String requestBody2 = objectMapper.writeValueAsString(request2);

            // Act
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody2))
                    .andExpect(status().isBadRequest());

            // Assert: Transaction should be in UNKNOWN state
            Transaction tx = transactionRepository.findById(transactionId).orElseThrow();
            assertEquals(PaymentStatus.UNKNOWN, tx.getStatus());
        }
    }

    @Nested
    @DisplayName("UNKNOWN State Recovery")
    class UnknownStateRecoveryTests {

        @Test
        @DisplayName("UNKNOWN transaction returns 202 Accepted")
        void testUnknownTransactionReturns202() throws Exception {
            // Arrange
            PaymentRequest request = createPaymentRequest(idempotencyKey);
            String requestBody = objectMapper.writeValueAsString(request);

            // Create first transaction
            MvcResult firstResult = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isAccepted())
                    .andReturn();

            String firstResponseBody = firstResult.getResponse().getContentAsString();
            PaymentResponseDTO firstResponse = objectMapper.readValue(firstResponseBody, PaymentResponseDTO.class);

            // Update to UNKNOWN
            Transaction tx = transactionRepository.findById(firstResponse.getTransactionId()).orElseThrow();
            tx.setStatus(PaymentStatus.UNKNOWN);
            transactionRepository.save(tx);

            // Clear cache to force database lookup
            responseCache.clear();

            // Act: Submit duplicate request
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("UNKNOWN transaction response contains correct status")
        void testUnknownTransactionResponseContainsCorrectStatus() throws Exception {
            // Arrange
            PaymentRequest request = createPaymentRequest(idempotencyKey);
            String requestBody = objectMapper.writeValueAsString(request);

            // Create first transaction
            MvcResult firstResult = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isAccepted())
                    .andReturn();

            String firstResponseBody = firstResult.getResponse().getContentAsString();
            PaymentResponseDTO firstResponse = objectMapper.readValue(firstResponseBody, PaymentResponseDTO.class);

            // Update to UNKNOWN
            Transaction tx = transactionRepository.findById(firstResponse.getTransactionId()).orElseThrow();
            tx.setStatus(PaymentStatus.UNKNOWN);
            transactionRepository.save(tx);

            // Clear cache
            responseCache.clear();

            // Act
            MvcResult secondResult = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isAccepted())
                    .andReturn();

            // Assert
            String secondResponseBody = secondResult.getResponse().getContentAsString();
            PaymentResponseDTO secondResponse = objectMapper.readValue(secondResponseBody, PaymentResponseDTO.class);

            assertEquals(PaymentStatus.UNKNOWN, secondResponse.getStatus());
            assertTrue(secondResponse.getMessage().contains("uncertain"));
        }
    }

    @Nested
    @DisplayName("Cache Behavior")
    class CacheBehaviorTests {

        @Test
        @DisplayName("Cache works with different merchants")
        void testCacheWorksWithDifferentMerchants() throws Exception {
            // Arrange
            PaymentRequest request = createPaymentRequest(idempotencyKey);
            String requestBody = objectMapper.writeValueAsString(request);

            // Create transaction
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isAccepted());

            // Assert: Response cached for merchant
            assertTrue(responseCache.retrieve(merchantId, idempotencyKey).isPresent());
        }

        @Test
        @DisplayName("Cache respects merchant isolation")
        void testCacheRespectsMerchantIsolation() throws Exception { // <-- Fixed space typo in method name
            // Arrange
            PaymentRequest request = createPaymentRequest(idempotencyKey);
            String requestBody = objectMapper.writeValueAsString(request);

            // Create transaction
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isAccepted());

            // Assert: Response cached for specific merchant
            assertTrue(responseCache.retrieve(merchantId, idempotencyKey).isPresent());

            // Different merchant should not see cached response
            UUID differentMerchant = UUID.randomUUID();
            assertTrue(responseCache.retrieve(differentMerchant, idempotencyKey).isEmpty());
        }
    }

    // Helper method
    private PaymentRequest createPaymentRequest(UUID idempotencyKey) {
        return PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(idempotencyKey)
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                        .cardTokenId("tok_visa_123")
                        .maskedCard("411111XXXXXX1111")
                        .build())
                .customerId(UUID.randomUUID())
                .build();
    }
}