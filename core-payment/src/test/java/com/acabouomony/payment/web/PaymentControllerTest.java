package com.acabouomony.payment.web;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.*;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import com.acabouomony.payment.infrastructure.security.ApiKeyAuthenticationFilter;
import com.acabouomony.payment.infrastructure.security.SecurityConfig;
import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for PaymentController orchestration wiring.
 *
 * Security auto-configuration is excluded so tests focus purely on controller
 * logic — auth is tested separately in ApiKeyAuthenticationFilterTest.
 *
 * Verifies:
 * - orchestrationService.processPayment() is called once on the new-transaction path
 * - orchestrationService.processPayment() is NOT called on duplicate paths
 * - orchestrationService.processPayment() is NOT called on cache-hit paths
 *
 * Task: task-core-00
 */
@WebMvcTest(
    controllers = PaymentController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, ApiKeyAuthenticationFilter.class})
    }
)
@DisplayName("PaymentController orchestration wiring")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---- mocked controller dependencies ----

    @MockBean
    private MerchantAuthService merchantAuthService;

    @MockBean
    private PaymentRequestValidator paymentRequestValidator;

    @MockBean
    private IdempotencyService idempotencyService;

    @MockBean
    private DuplicatePaymentHandler duplicatePaymentHandler;

    @MockBean
    private DuplicateRequestRecoveryService duplicateRequestRecoveryService;

    @MockBean
    private TransactionRepository transactionRepository;

    @MockBean
    private PaymentOrchestrationService orchestrationService;

    private UUID idempotencyKey;
    private PaymentRequest request;
    private Transaction savedTx;
    private PaymentResponseDTO responseDto;

    @BeforeEach
    void setUp() {
        idempotencyKey = UUID.randomUUID();

        request = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(idempotencyKey)
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                        .cardTokenId("tok_visa_123")
                        .maskedCard("411111XXXXXX1111")
                        .build())
                .customerId(UUID.randomUUID())
                .build();

        savedTx = Transaction.builder()
                .id(UUID.randomUUID())
                .merchantId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .idempotencyKey(idempotencyKey)
                .amount(10000L)
                .currency("BRL")
                .status(PaymentStatus.COMPLETED)
                .payloadHash("abc123")
                .maskedCard("411111XXXXXX1111")
                .cardTokenId("tok_visa_123")
                .version(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        responseDto = PaymentResponseDTO.builder()
                .transactionId(savedTx.getId())
                .status(PaymentStatus.COMPLETED)
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(idempotencyKey)
                .createdAt(savedTx.getCreatedAt())
                .updatedAt(savedTx.getUpdatedAt())
                .build();
    }

    @Nested
    @DisplayName("New transaction path")
    class NewTransactionPath {

        @BeforeEach
        void setUpNewPath() {
            // No cache hit
            when(duplicateRequestRecoveryService.recoverFromCache(any(), any()))
                    .thenReturn(Optional.empty());

            // Validation passes (doNothing is default for void)
            doNothing().when(paymentRequestValidator).validate(any());

            // No duplicate
            when(idempotencyService.checkDuplicate(any(), any(), any()))
                    .thenReturn(Optional.empty());

            // preparePayloadHash returns a 64-char hex string
            when(idempotencyService.preparePayloadHash(any()))
                    .thenReturn("aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899");

            // save returns the pre-built transaction
            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

            // orchestration is void — default Mockito behaviour is doNothing for void methods
            doNothing().when(orchestrationService).processPayment(any(Transaction.class));

            // buildNewPaymentResponse returns 200 OK
            when(duplicatePaymentHandler.buildNewPaymentResponse(any(Transaction.class)))
                    .thenReturn(ResponseEntity.ok(responseDto));
        }

        @Test
        @DisplayName("calls orchestrationService.processPayment() exactly once")
        void callsProcessPaymentOnce() throws Exception {
            mockMvc.perform(post("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            verify(orchestrationService, times(1)).processPayment(any(Transaction.class));
        }

        @Test
        @DisplayName("returns 2xx for valid new payment")
        void returnsSuccessForNewPayment() throws Exception {
            mockMvc.perform(post("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is2xxSuccessful());
        }
    }

    @Nested
    @DisplayName("Duplicate paths — orchestration must NOT run")
    class DuplicatePaths {

        @BeforeEach
        void setUpDuplicatePath() {
            // No cache hit
            when(duplicateRequestRecoveryService.recoverFromCache(any(), any()))
                    .thenReturn(Optional.empty());

            // Validation passes
            doNothing().when(paymentRequestValidator).validate(any());

            Transaction existingTx = Transaction.builder()
                    .id(UUID.randomUUID())
                    .merchantId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                    .idempotencyKey(idempotencyKey)
                    .amount(10000L)
                    .currency("BRL")
                    .status(PaymentStatus.COMPLETED)
                    .payloadHash("abc123")
                    .maskedCard("411111XXXXXX1111")
                    .cardTokenId("tok_visa_123")
                    .version(1)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            // Duplicate found
            when(idempotencyService.checkDuplicate(any(), any(), any()))
                    .thenReturn(Optional.of(existingTx));

            // Safe to return cached response
            when(idempotencyService.isSafeToReturnCachedResponse(any()))
                    .thenReturn(true);

            when(duplicatePaymentHandler.buildDuplicateResponse(any()))
                    .thenReturn(ResponseEntity.ok(responseDto));
        }

        @Test
        @DisplayName("does NOT call processPayment() when duplicate is detected")
        void doesNotCallProcessPaymentOnDuplicate() throws Exception {
            mockMvc.perform(post("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            verify(orchestrationService, never()).processPayment(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("Cache-hit path — orchestration must NOT run")
    class CacheHitPath {

        @BeforeEach
        void setUpCacheHitPath() {
            // Cache hit: returns early before orchestration
            when(duplicateRequestRecoveryService.recoverFromCache(any(), any()))
                    .thenReturn(Optional.of(responseDto));
        }

        @Test
        @DisplayName("does NOT call processPayment() when cache hit")
        void doesNotCallProcessPaymentOnCacheHit() throws Exception {
            mockMvc.perform(post("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            verify(orchestrationService, never()).processPayment(any(Transaction.class));
        }
    }
}
