package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.PaymentValidationException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for IdempotencyService.
 * 
 * Tests verify:
 * - Duplicate detection using idempotency key and payload hash
 * - Payload hash validation (detect different payloads with same key)
 * - Safe cached response determination based on transaction status
 * - Payload hash preparation for new transactions
 * 
 * Spec: spec-001-core-payment-processing.md - Idempotency Rules
 * Task: task-007-payload-hashing.md
 */
@DisplayName("IdempotencyService")
class IdempotencyServiceTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private PayloadHashingService payloadHashingService;
    
    private IdempotencyService service;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new IdempotencyService(transactionRepository, payloadHashingService);
    }
    
    @Nested
    @DisplayName("Duplicate Detection")
    class DuplicateDetectionTests {
        
        @Test
        @DisplayName("Returns empty when no existing transaction found")
        void testReturnsEmptyWhenNoExistingTransaction() {
            // Arrange
            UUID merchantId = UUID.randomUUID();
            UUID idempotencyKey = UUID.randomUUID();
            PaymentRequest request = createPaymentRequest();
            
            when(transactionRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey))
                .thenReturn(Optional.empty());
            
            // Act
            Optional<Transaction> result = service.checkDuplicate(merchantId, idempotencyKey, request);
            
            // Assert
            assertTrue(result.isEmpty(), "Should return empty when no existing transaction");
        }
        
        @Test
        @DisplayName("Returns existing transaction when payload hash matches")
        void testReturnsExistingTransactionWhenPayloadHashMatches() {
            // Arrange
            UUID merchantId = UUID.randomUUID();
            UUID idempotencyKey = UUID.randomUUID();
            String payloadHash = "abc123def456";
            PaymentRequest request = createPaymentRequest();
            
            Transaction existingTx = Transaction.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .idempotencyKey(idempotencyKey)
                .amount(10000L)
                .currency("BRL")
                .status(PaymentStatus.COMPLETED)
                .payloadHash(payloadHash)
                .version(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            when(transactionRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey))
                .thenReturn(Optional.of(existingTx));
            when(payloadHashingService.computePayloadHash(request))
                .thenReturn(payloadHash);
            
            // Act
            Optional<Transaction> result = service.checkDuplicate(merchantId, idempotencyKey, request);
            
            // Assert
            assertTrue(result.isPresent(), "Should return existing transaction");
            assertEquals(existingTx.getId(), result.get().getId());
        }
        
        @Test
        @DisplayName("Throws exception when payload hash mismatches")
        void testThrowsExceptionWhenPayloadHashMismatches() {
            // Arrange
            UUID merchantId = UUID.randomUUID();
            UUID idempotencyKey = UUID.randomUUID();
            String storedHash = "abc123def456";
            String currentHash = "xyz789uvw012";
            PaymentRequest request = createPaymentRequest();
            
            Transaction existingTx = Transaction.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .idempotencyKey(idempotencyKey)
                .amount(10000L)
                .currency("BRL")
                .status(PaymentStatus.COMPLETED)
                .payloadHash(storedHash)
                .version(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            when(transactionRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey))
                .thenReturn(Optional.of(existingTx));
            when(payloadHashingService.computePayloadHash(request))
                .thenReturn(currentHash);
            
            // Act & Assert
            PaymentValidationException exception = assertThrows(
                PaymentValidationException.class,
                () -> service.checkDuplicate(merchantId, idempotencyKey, request)
            );
            
            assertTrue(exception.getMessage().contains("Idempotency key mismatch"),
                "Exception should mention idempotency key mismatch");
            assertTrue(exception.getMessage().contains("payload differs"),
                "Exception should mention payload difference");
        }
    }
    
    @Nested
    @DisplayName("Cached Response Safety")
    class CachedResponseSafetyTests {
        
        @Test
        @DisplayName("Safe to return cached response for COMPLETED transaction")
        void testSafeToReturnCachedResponseForCompleted() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.COMPLETED)
                .build();
            
            // Act
            boolean isSafe = service.isSafeToReturnCachedResponse(transaction);
            
            // Assert
            assertTrue(isSafe, "Should be safe to return cached response for COMPLETED");
        }
        
        @Test
        @DisplayName("Safe to return cached response for DECLINED transaction")
        void testSafeToReturnCachedResponseForDeclined() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.DECLINED)
                .build();
            
            // Act
            boolean isSafe = service.isSafeToReturnCachedResponse(transaction);
            
            // Assert
            assertTrue(isSafe, "Should be safe to return cached response for DECLINED");
        }
        
        @Test
        @DisplayName("Safe to return cached response for FAILED transaction")
        void testSafeToReturnCachedResponseForFailed() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.FAILED)
                .build();
            
            // Act
            boolean isSafe = service.isSafeToReturnCachedResponse(transaction);
            
            // Assert
            assertTrue(isSafe, "Should be safe to return cached response for FAILED");
        }
        
        @Test
        @DisplayName("NOT safe to return cached response for UNKNOWN transaction")
        void testNotSafeToReturnCachedResponseForUnknown() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.UNKNOWN)
                .build();
            
            // Act
            boolean isSafe = service.isSafeToReturnCachedResponse(transaction);
            
            // Assert
            assertFalse(isSafe, "Should NOT be safe to return cached response for UNKNOWN");
        }
        
        @Test
        @DisplayName("NOT safe to return cached response for PROCESSING transaction")
        void testNotSafeToReturnCachedResponseForProcessing() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.PROCESSING)
                .build();
            
            // Act
            boolean isSafe = service.isSafeToReturnCachedResponse(transaction);
            
            // Assert
            assertFalse(isSafe, "Should NOT be safe to return cached response for PROCESSING");
        }
        
        @Test
        @DisplayName("NOT safe to return cached response for VALIDATED transaction")
        void testNotSafeToReturnCachedResponseForValidated() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.VALIDATED)
                .build();
            
            // Act
            boolean isSafe = service.isSafeToReturnCachedResponse(transaction);
            
            // Assert
            assertFalse(isSafe, "Should NOT be safe to return cached response for VALIDATED");
        }
        
        @Test
        @DisplayName("NOT safe to return cached response for CHALLENGE_PENDING transaction")
        void testNotSafeToReturnCachedResponseForChallengePending() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.CHALLENGE_PENDING)
                .build();
            
            // Act
            boolean isSafe = service.isSafeToReturnCachedResponse(transaction);
            
            // Assert
            assertFalse(isSafe, "Should NOT be safe to return cached response for CHALLENGE_PENDING");
        }
        
        @Test
        @DisplayName("NOT safe to return cached response for AUTHENTICATED transaction")
        void testNotSafeToReturnCachedResponseForAuthenticated() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.AUTHENTICATED)
                .build();
            
            // Act
            boolean isSafe = service.isSafeToReturnCachedResponse(transaction);
            
            // Assert
            assertFalse(isSafe, "Should NOT be safe to return cached response for AUTHENTICATED");
        }
    }
    
    @Nested
    @DisplayName("Payload Hash Preparation")
    class PayloadHashPreparationTests {
        
        @Test
        @DisplayName("Prepares payload hash for new transaction")
        void testPreparesPayloadHashForNewTransaction() {
            // Arrange
            String expectedHash = "abc123def456";
            PaymentRequest request = createPaymentRequest();
            
            when(payloadHashingService.computePayloadHash(request))
                .thenReturn(expectedHash);
            
            // Act
            String hash = service.preparePayloadHash(request);
            
            // Assert
            assertEquals(expectedHash, hash, "Should return computed payload hash");
        }
    }
    
    // Helper method to create a valid payment request
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
