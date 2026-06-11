package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.DuplicatePaymentException;
import com.acabouomony.payment.domain.exception.PaymentValidationException;
import com.acabouomony.payment.domain.model.PaymentStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

/**
 * Unit tests for DuplicatePaymentHandler.
 * 
 * Tests verify:
 * - Duplicate payment recovery from database
 * - Payload hash validation
 * - HTTP response building for various transaction statuses
 * - Exception handling for constraint violations
 * 
 * Spec: spec-001-core-payment-processing.md - Duplicate Request Rules
 * Task: task-009-db-idempotency-enforcement.md
 */
@DisplayName("DuplicatePaymentHandler")
class DuplicatePaymentHandlerTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private IdempotencyService idempotencyService;
    
    private DuplicatePaymentHandler handler;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new DuplicatePaymentHandler(transactionRepository, idempotencyService);
    }
    
    @Nested
    @DisplayName("Handle Duplicate Payment")
    class HandleDuplicatePaymentTests {
        
        @Test
        @DisplayName("Recovers existing transaction when duplicate detected")
        void testRecoversExistingTransaction() {
            // Arrange
            UUID merchantId = UUID.randomUUID();
            UUID idempotencyKey = UUID.randomUUID();
            PaymentRequest request = createPaymentRequest();
            
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
            
            when(transactionRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey))
                .thenReturn(Optional.of(existingTx));
            when(idempotencyService.checkDuplicate(merchantId, idempotencyKey, request))
                .thenReturn(Optional.of(existingTx));
            
            // Act
            Transaction result = handler.handleDuplicatePayment(merchantId, idempotencyKey, request);
            
            // Assert
            assertNotNull(result);
            assertEquals(existingTx.getId(), result.getId());
            assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        }
        
        @Test
        @DisplayName("Throws exception when transaction not found")
        void testThrowsExceptionWhenTransactionNotFound() {
            // Arrange
            UUID merchantId = UUID.randomUUID();
            UUID idempotencyKey = UUID.randomUUID();
            PaymentRequest request = createPaymentRequest();
            
            when(transactionRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey))
                .thenReturn(Optional.empty());
            
            // Act & Assert
            DuplicatePaymentException exception = assertThrows(
                DuplicatePaymentException.class,
                () -> handler.handleDuplicatePayment(merchantId, idempotencyKey, request)
            );
            
            assertTrue(exception.getMessage().contains("not found"));
        }
        
        @Test
        @DisplayName("Throws exception when payload hash mismatches")
        void testThrowsExceptionWhenPayloadHashMismatches() {
            // Arrange
            UUID merchantId = UUID.randomUUID();
            UUID idempotencyKey = UUID.randomUUID();
            PaymentRequest request = createPaymentRequest();
            
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
            
            when(transactionRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey))
                .thenReturn(Optional.of(existingTx));
            doThrow(new PaymentValidationException("Payload hash mismatch"))
                .when(idempotencyService).checkDuplicate(merchantId, idempotencyKey, request);
            
            // Act & Assert
            PaymentValidationException exception = assertThrows(
                PaymentValidationException.class,
                () -> handler.handleDuplicatePayment(merchantId, idempotencyKey, request)
            );
            
            assertTrue(exception.getMessage().contains("mismatch"));
        }
    }
    
    @Nested
    @DisplayName("Build Duplicate Response")
    class BuildDuplicateResponseTests {
        
        @Test
        @DisplayName("Returns 200 OK for COMPLETED transaction")
        void testReturns200ForCompleted() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.COMPLETED)
                .amount(10000L)
                .currency("BRL")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            // Act
            ResponseEntity<PaymentResponseDTO> response = handler.buildDuplicateResponse(transaction);
            
            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(PaymentStatus.COMPLETED, response.getBody().getStatus());
            assertTrue(response.getBody().getMessage().contains("approved"));
        }
        
        @Test
        @DisplayName("Returns 200 OK for DECLINED transaction")
        void testReturns200ForDeclined() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.DECLINED)
                .amount(10000L)
                .currency("BRL")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            // Act
            ResponseEntity<PaymentResponseDTO> response = handler.buildDuplicateResponse(transaction);
            
            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(PaymentStatus.DECLINED, response.getBody().getStatus());
            assertTrue(response.getBody().getMessage().contains("declined"));
        }
        
        @Test
        @DisplayName("Returns 200 OK for FAILED transaction")
        void testReturns200ForFailed() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.FAILED)
                .amount(10000L)
                .currency("BRL")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            // Act
            ResponseEntity<PaymentResponseDTO> response = handler.buildDuplicateResponse(transaction);
            
            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(PaymentStatus.FAILED, response.getBody().getStatus());
            assertTrue(response.getBody().getMessage().contains("failed"));
        }
        
        @Test
        @DisplayName("Returns 202 Accepted for UNKNOWN transaction")
        void testReturns202ForUnknown() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.UNKNOWN)
                .amount(10000L)
                .currency("BRL")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            // Act
            ResponseEntity<PaymentResponseDTO> response = handler.buildDuplicateResponse(transaction);
            
            // Assert
            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(PaymentStatus.UNKNOWN, response.getBody().getStatus());
            assertTrue(response.getBody().getMessage().contains("uncertain"));
        }
        
        @Test
        @DisplayName("Returns 409 Conflict for PROCESSING transaction")
        void testReturns409ForProcessing() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.PROCESSING)
                .amount(10000L)
                .currency("BRL")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            // Act
            ResponseEntity<PaymentResponseDTO> response = handler.buildDuplicateResponse(transaction);
            
            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(PaymentStatus.PROCESSING, response.getBody().getStatus());
            assertTrue(response.getBody().getMessage().contains("progress"));
        }
        
        @Test
        @DisplayName("Returns 409 Conflict for VALIDATED transaction")
        void testReturns409ForValidated() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.VALIDATED)
                .amount(10000L)
                .currency("BRL")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            // Act
            ResponseEntity<PaymentResponseDTO> response = handler.buildDuplicateResponse(transaction);
            
            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(PaymentStatus.VALIDATED, response.getBody().getStatus());
        }
        
        @Test
        @DisplayName("Returns 409 Conflict for CHALLENGE_PENDING transaction")
        void testReturns409ForChallengePending() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.CHALLENGE_PENDING)
                .amount(10000L)
                .currency("BRL")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            // Act
            ResponseEntity<PaymentResponseDTO> response = handler.buildDuplicateResponse(transaction);
            
            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(PaymentStatus.CHALLENGE_PENDING, response.getBody().getStatus());
        }
        
        @Test
        @DisplayName("Response contains transaction details")
        void testResponseContainsTransactionDetails() {
            // Arrange
            UUID transactionId = UUID.randomUUID();
            String maskedCard = "411111XXXXXX1111";
            Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(PaymentStatus.COMPLETED)
                .amount(10000L)
                .currency("BRL")
                .maskedCard(maskedCard)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            // Act
            ResponseEntity<PaymentResponseDTO> response = handler.buildDuplicateResponse(transaction);
            
            // Assert
            PaymentResponseDTO body = response.getBody();
            assertNotNull(body);
            assertEquals(transactionId, body.getTransactionId());
            assertEquals(10000L, body.getAmount());
            assertEquals("BRL", body.getCurrency());
            assertEquals(maskedCard, body.getMaskedCard());
        }
    }
    
    @Nested
    @DisplayName("Build New Payment Response")
    class BuildNewPaymentResponseTests {
        
        @Test
        @DisplayName("Returns 200 OK for COMPLETED new payment")
        void testReturns200ForCompletedNewPayment() {
            // Arrange
            UUID idempotencyKey = UUID.randomUUID();
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.COMPLETED)
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(idempotencyKey)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            // Act
            ResponseEntity<PaymentResponseDTO> response = handler.buildNewPaymentResponse(transaction);
            
            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(idempotencyKey, response.getBody().getIdempotencyKey());
        }
        
        @Test
        @DisplayName("Returns 202 Accepted for UNKNOWN new payment")
        void testReturns202ForUnknownNewPayment() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.UNKNOWN)
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            // Act
            ResponseEntity<PaymentResponseDTO> response = handler.buildNewPaymentResponse(transaction);
            
            // Assert
            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(PaymentStatus.UNKNOWN, response.getBody().getStatus());
        }
        
        @Test
        @DisplayName("Returns 202 Accepted for PROCESSING new payment")
        void testReturns202ForProcessingNewPayment() {
            // Arrange
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.PROCESSING)
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            // Act
            ResponseEntity<PaymentResponseDTO> response = handler.buildNewPaymentResponse(transaction);
            
            // Assert
            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(PaymentStatus.PROCESSING, response.getBody().getStatus());
        }
    }
    
    // Helper method
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
