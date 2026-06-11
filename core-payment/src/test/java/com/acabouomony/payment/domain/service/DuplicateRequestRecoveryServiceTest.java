package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.AuditLogRepository;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for DuplicateRequestRecoveryService.
 * 
 * Tests verify:
 * - Response caching and retrieval
 * - Mismatched payload handling
 * - UNKNOWN state recovery
 * - Cache invalidation
 * - Reconciliation scheduling
 * 
 * Spec: spec-001-core-payment-processing.md - Duplicate Request Rules
 * Task: task-010-duplicate-request-recovery.md
 */
@DisplayName("DuplicateRequestRecoveryService")
class DuplicateRequestRecoveryServiceTest {
    
    @Mock
    private PaymentResponseCache responseCache;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    @Mock
    private UnknownStateRecoveryHandler unknownStateRecoveryHandler;
    
    private DuplicateRequestRecoveryService service;
    private UUID merchantId;
    private UUID idempotencyKey;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DuplicateRequestRecoveryService(
            responseCache,
            transactionRepository,
            auditLogRepository,
            unknownStateRecoveryHandler
        );
        merchantId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();
    }
    
    @Nested
    @DisplayName("Cache Recovery")
    class CacheRecoveryTests {
        
        @Test
        @DisplayName("Recovers response from cache")
        void testRecoversResponseFromCache() {
            // Arrange
            PaymentResponseDTO cachedResponse = createPaymentResponse();
            when(responseCache.retrieve(merchantId, idempotencyKey))
                .thenReturn(Optional.of(cachedResponse));
            
            // Act
            Optional<PaymentResponseDTO> result = service.recoverFromCache(merchantId, idempotencyKey);
            
            // Assert
            assertTrue(result.isPresent());
            assertEquals(cachedResponse.getTransactionId(), result.get().getTransactionId());
        }
        
        @Test
        @DisplayName("Returns empty when cache miss")
        void testReturnsEmptyWhenCacheMiss() {
            // Arrange
            when(responseCache.retrieve(merchantId, idempotencyKey))
                .thenReturn(Optional.empty());
            
            // Act
            Optional<PaymentResponseDTO> result = service.recoverFromCache(merchantId, idempotencyKey);
            
            // Assert
            assertTrue(result.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Cache Response")
    class CacheResponseTests {
        
        @Test
        @DisplayName("Caches payment response")
        void testCachesPaymentResponse() {
            // Arrange
            PaymentResponseDTO response = createPaymentResponse();
            
            // Act
            service.cacheResponse(merchantId, idempotencyKey, response);
            
            // Assert
            verify(responseCache).cache(any(), any(), any(), any());
        }
    }
    
    @Nested
    @DisplayName("Mismatched Payload Handling")
    class MismatchedPayloadHandlingTests {
        
        @Test
        @DisplayName("Handles mismatched payload")
        void testHandlesMismatchedPayload() {
            // Arrange
            Transaction existingTx = Transaction.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .idempotencyKey(idempotencyKey)
                .amount(10000L)
                .currency("BRL")
                .status(PaymentStatus.COMPLETED)
                .payloadHash("abc123")
                .version(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            PaymentRequest request = createPaymentRequest();
            
            when(transactionRepository.save(any())).thenReturn(existingTx);
            when(unknownStateRecoveryHandler.canRecoverFromUnknown(any())).thenReturn(true);
            
            // Act
            ResponseEntity<PaymentResponseDTO> response = service.handleMismatchedPayload(
                merchantId,
                idempotencyKey,
                request,
                existingTx
            );
            
            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(PaymentStatus.UNKNOWN, response.getBody().getStatus());
            assertTrue(response.getBody().getMessage().contains("mismatch"));
        }
        
        @Test
        @DisplayName("Transitions transaction to UNKNOWN")
        void testTransitionsTransactionToUnknown() {
            // Arrange
            Transaction existingTx = Transaction.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .idempotencyKey(idempotencyKey)
                .amount(10000L)
                .currency("BRL")
                .status(PaymentStatus.COMPLETED)
                .payloadHash("abc123")
                .version(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            PaymentRequest request = createPaymentRequest();
            
            when(transactionRepository.save(any())).thenReturn(existingTx);
            when(unknownStateRecoveryHandler.canRecoverFromUnknown(any())).thenReturn(true);
            
            // Act
            service.handleMismatchedPayload(merchantId, idempotencyKey, request, existingTx);
            
            // Assert
            assertEquals(PaymentStatus.UNKNOWN, existingTx.getStatus());
            verify(transactionRepository).save(existingTx);
        }
        
        @Test
        @DisplayName("Schedules reconciliation")
        void testSchedulesReconciliation() {
            // Arrange
            Transaction existingTx = Transaction.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .idempotencyKey(idempotencyKey)
                .amount(10000L)
                .currency("BRL")
                .status(PaymentStatus.COMPLETED)
                .payloadHash("abc123")
                .version(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            PaymentRequest request = createPaymentRequest();
            
            when(transactionRepository.save(any())).thenReturn(existingTx);
            when(unknownStateRecoveryHandler.canRecoverFromUnknown(any())).thenReturn(true);
            
            // Act
            service.handleMismatchedPayload(merchantId, idempotencyKey, request, existingTx);
            
            // Assert
            verify(unknownStateRecoveryHandler).scheduleReconciliation(any());
        }
    }
    
    @Nested
    @DisplayName("UNKNOWN State Recovery")
    class UnknownStateRecoveryTests {
        
        @Test
        @DisplayName("Handles UNKNOWN state recovery")
        void testHandlesUnknownStateRecovery() {
            // Arrange
            Transaction unknownTx = Transaction.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .idempotencyKey(idempotencyKey)
                .amount(10000L)
                .currency("BRL")
                .status(PaymentStatus.UNKNOWN)
                .payloadHash("abc123")
                .version(2)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            PaymentResponseDTO unknownResponse = PaymentResponseDTO.builder()
                .transactionId(unknownTx.getId())
                .status(PaymentStatus.UNKNOWN)
                .amount(unknownTx.getAmount())
                .currency(unknownTx.getCurrency())
                .message("Payment outcome uncertain")
                .createdAt(unknownTx.getCreatedAt())
                .updatedAt(unknownTx.getUpdatedAt())
                .build();
            
            when(unknownStateRecoveryHandler.canRecoverFromUnknown(unknownTx)).thenReturn(true);
            when(unknownStateRecoveryHandler.buildUnknownRecoveryResponse(unknownTx))
                .thenReturn(ResponseEntity.accepted().body(unknownResponse));
            
            // Act
            ResponseEntity<PaymentResponseDTO> response = service.handleUnknownStateRecovery(
                merchantId,
                idempotencyKey,
                unknownTx
            );
            
            // Assert
            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(PaymentStatus.UNKNOWN, response.getBody().getStatus());
        }
        
        @Test
        @DisplayName("Schedules reconciliation for UNKNOWN state")
        void testSchedulesReconciliationForUnknownState() {
            // Arrange
            Transaction unknownTx = Transaction.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .idempotencyKey(idempotencyKey)
                .amount(10000L)
                .currency("BRL")
                .status(PaymentStatus.UNKNOWN)
                .payloadHash("abc123")
                .version(2)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            when(unknownStateRecoveryHandler.canRecoverFromUnknown(unknownTx)).thenReturn(true);
            when(unknownStateRecoveryHandler.buildUnknownRecoveryResponse(unknownTx))
                .thenReturn(ResponseEntity.accepted().build());
            
            // Act
            service.handleUnknownStateRecovery(merchantId, idempotencyKey, unknownTx);
            
            // Assert
            verify(unknownStateRecoveryHandler).scheduleReconciliation(unknownTx);
        }
    }
    
    @Nested
    @DisplayName("Cache Invalidation")
    class CacheInvalidationTests {
        
        @Test
        @DisplayName("Invalidates cache")
        void testInvalidatesCache() {
            // Act
            service.invalidateCache(merchantId, idempotencyKey);
            
            // Assert
            verify(responseCache).invalidate(merchantId, idempotencyKey);
        }
    }
    
    // Helper methods
    private PaymentResponseDTO createPaymentResponse() {
        return PaymentResponseDTO.builder()
            .transactionId(UUID.randomUUID())
            .status(PaymentStatus.COMPLETED)
            .amount(10000L)
            .currency("BRL")
            .maskedCard("411111XXXXXX1111")
            .message("Payment approved")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
    
    private PaymentRequest createPaymentRequest() {
        return PaymentRequest.builder()
            .amount(10000L)
            .currency("BRL")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_visa_123")
                .maskedCard("411111XXXXXX1111")
                .build())
            .customerId(UUID.randomUUID())
            .build();
    }
}
