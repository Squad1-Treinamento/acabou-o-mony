package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentResult;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentOrchestrationService.
 * 
 * Tests verify:
 * - Payment processing flows
 * - State transitions
 * - Risk evaluation
 * - Audit log creation
 * - Outbox event creation
 * - Optimistic locking retry
 * 
 * Spec: spec-001-core-payment-processing.md - Payment Lifecycle Rules
 * Task: task-012-payment-orchestration.md
 */
@DisplayName("PaymentOrchestrationService")
class PaymentOrchestrationServiceTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private PaymentAcquirerClient paymentAcquirerClient;
    
    @Mock
    private RiskEvaluationService riskEvaluationService;
    
    @Mock
    private StateTransitionValidator stateTransitionValidator;
    
    @Mock
    private AuditLogService auditLogService;
    
    @Mock
    private OutboxEventService outboxEventService;
    
    private PaymentOrchestrationService service;
    private UUID merchantId;
    private UUID transactionId;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PaymentOrchestrationService(
            transactionRepository,
            paymentAcquirerClient,
            riskEvaluationService,
            stateTransitionValidator,
            auditLogService,
            outboxEventService
        );
        merchantId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
    }
    
    @Nested
    @DisplayName("Payment Processing")
    class PaymentProcessingTests {
        
        @Test
        @DisplayName("Throws exception for null transaction")
        void testThrowsExceptionForNullTransaction() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> service.processPayment(null));
        }
        
        @Test
        @DisplayName("Throws exception if not in CREATED state")
        void testThrowsExceptionIfNotInCreatedState() {
            // Arrange
            Transaction transaction = createTransaction();
            transaction.setStatus(PaymentStatus.VALIDATED);
            
            // Act & Assert
            assertThrows(IllegalStateException.class, () -> service.processPayment(transaction));
        }
        
        @Test
        @DisplayName("Processes low-risk payment successfully")
        void testProcessesLowRiskPaymentSuccessfully() {
            // Arrange
            Transaction transaction = createTransaction();
            PaymentResult result = PaymentResult.builder()
                .acquirerReference("mp_123")
                .status(PaymentStatus.COMPLETED)
                .message("Payment approved")
                .timestamp(Instant.now())
                .build();
            
            when(riskEvaluationService.isHighRisk(transaction)).thenReturn(false);
            when(paymentAcquirerClient.submitPayment(transaction)).thenReturn(result);
            when(transactionRepository.save(any())).thenReturn(transaction);
            
            // Act
            service.processPayment(transaction);
            
            // Assert
            verify(riskEvaluationService).isHighRisk(transaction);
            verify(paymentAcquirerClient).submitPayment(transaction);
            verify(outboxEventService).createPaymentCompletedEvent(any());
        }
        
        @Test
        @DisplayName("Processes high-risk payment (3DS)")
        void testProcessesHighRiskPayment() {
            // Arrange
            Transaction transaction = createTransaction();
            
            when(riskEvaluationService.isHighRisk(transaction)).thenReturn(true);
            when(transactionRepository.save(any())).thenReturn(transaction);
            
            // Act
            service.processPayment(transaction);
            
            // Assert
            verify(riskEvaluationService).isHighRisk(transaction);
            verify(paymentAcquirerClient, never()).submitPayment(any());
            assertEquals(PaymentStatus.CHALLENGE_PENDING, transaction.getStatus());
        }
    }
    
    @Nested
    @DisplayName("Payment Results")
    class PaymentResultTests {
        
        @Test
        @DisplayName("Handles payment completion")
        void testHandlesPaymentCompletion() {
            // Arrange
            Transaction transaction = createTransaction();
            transaction.setStatus(PaymentStatus.PROCESSING);
            PaymentResult result = PaymentResult.builder()
                .acquirerReference("mp_123")
                .status(PaymentStatus.COMPLETED)
                .timestamp(Instant.now())
                .build();
            
            when(riskEvaluationService.isHighRisk(transaction)).thenReturn(false);
            when(paymentAcquirerClient.submitPayment(transaction)).thenReturn(result);
            when(transactionRepository.save(any())).thenReturn(transaction);
            
            // Act
            service.processPayment(transaction);
            
            // Assert
            verify(outboxEventService).createPaymentCompletedEvent(any());
        }
        
        @Test
        @DisplayName("Handles payment decline")
        void testHandlesPaymentDecline() {
            // Arrange
            Transaction transaction = createTransaction();
            transaction.setStatus(PaymentStatus.PROCESSING);
            PaymentResult result = PaymentResult.builder()
                .status(PaymentStatus.DECLINED)
                .message("Insufficient funds")
                .timestamp(Instant.now())
                .build();
            
            when(riskEvaluationService.isHighRisk(transaction)).thenReturn(false);
            when(paymentAcquirerClient.submitPayment(transaction)).thenReturn(result);
            when(transactionRepository.save(any())).thenReturn(transaction);
            
            // Act
            service.processPayment(transaction);
            
            // Assert
            verify(outboxEventService).createPaymentDeclinedEvent(any());
        }
        
        @Test
        @DisplayName("Handles payment failure")
        void testHandlesPaymentFailure() {
            // Arrange
            Transaction transaction = createTransaction();
            transaction.setStatus(PaymentStatus.PROCESSING);
            PaymentResult result = PaymentResult.builder()
                .status(PaymentStatus.FAILED)
                .message("Internal error")
                .timestamp(Instant.now())
                .build();
            
            when(riskEvaluationService.isHighRisk(transaction)).thenReturn(false);
            when(paymentAcquirerClient.submitPayment(transaction)).thenReturn(result);
            when(transactionRepository.save(any())).thenReturn(transaction);
            
            // Act
            service.processPayment(transaction);
            
            // Assert
            verify(outboxEventService).createPaymentFailedEvent(any());
        }
        
        @Test
        @DisplayName("Handles payment timeout (UNKNOWN)")
        void testHandlesPaymentTimeout() {
            // Arrange
            Transaction transaction = createTransaction();
            transaction.setStatus(PaymentStatus.PROCESSING);
            PaymentResult result = PaymentResult.builder()
                .status(PaymentStatus.UNKNOWN)
                .message("Payment submission timeout")
                .timestamp(Instant.now())
                .build();
            
            when(riskEvaluationService.isHighRisk(transaction)).thenReturn(false);
            when(paymentAcquirerClient.submitPayment(transaction)).thenReturn(result);
            when(transactionRepository.save(any())).thenReturn(transaction);
            
            // Act
            service.processPayment(transaction);
            
            // Assert
            verify(outboxEventService).createPaymentUnknownEvent(any());
        }
    }
    
    @Nested
    @DisplayName("State Transitions")
    class StateTransitionTests {
        
        @Test
        @DisplayName("Increments version on state change")
        void testIncrementsVersionOnStateChange() {
            // Arrange
            Transaction transaction = createTransaction();
            int initialVersion = transaction.getVersion();
            
            when(riskEvaluationService.isHighRisk(transaction)).thenReturn(false);
            when(paymentAcquirerClient.submitPayment(transaction)).thenReturn(
                PaymentResult.builder()
                    .status(PaymentStatus.COMPLETED)
                    .timestamp(Instant.now())
                    .build()
            );
            when(transactionRepository.save(any())).thenReturn(transaction);
            
            // Act
            service.processPayment(transaction);
            
            // Assert
            assertTrue(transaction.getVersion() > initialVersion);
        }
        
        @Test
        @DisplayName("Creates audit log entry")
        void testCreatesAuditLogEntry() {
            // Arrange
            Transaction transaction = createTransaction();
            
            when(riskEvaluationService.isHighRisk(transaction)).thenReturn(false);
            when(paymentAcquirerClient.submitPayment(transaction)).thenReturn(
                PaymentResult.builder()
                    .status(PaymentStatus.COMPLETED)
                    .timestamp(Instant.now())
                    .build()
            );
            when(transactionRepository.save(any())).thenReturn(transaction);
            
            // Act
            service.processPayment(transaction);
            
            // Assert
            verify(auditLogService, atLeastOnce()).logStateTransition(any(), any(), any(), any());
        }
    }
    
    @Nested
    @DisplayName("Optimistic Locking")
    class OptimisticLockingTests {
        
        @Test
        @DisplayName("Retries on version conflict")
        void testRetriesOnVersionConflict() {
            // Arrange
            Transaction transaction = createTransaction();
            Transaction freshTransaction = createTransaction();
            freshTransaction.setVersion(transaction.getVersion() + 1);
            
            when(riskEvaluationService.isHighRisk(transaction)).thenReturn(false);
            when(paymentAcquirerClient.submitPayment(transaction)).thenReturn(
                PaymentResult.builder()
                    .status(PaymentStatus.COMPLETED)
                    .timestamp(Instant.now())
                    .build()
            );
            
            // First call throws exception, second succeeds
            when(transactionRepository.save(any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("Version conflict", null))
                .thenReturn(transaction);
            
            when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(freshTransaction));
            
            // Act & Assert
            // Should not throw exception due to retry logic
            assertDoesNotThrow(() -> service.processPayment(transaction));
        }
    }
    
    // Helper method
    private Transaction createTransaction() {
        return Transaction.builder()
            .id(transactionId)
            .merchantId(merchantId)
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(PaymentStatus.CREATED)
            .payloadHash("abc123")
            .maskedCard("411111XXXXXX1111")
            .cardTokenId("tok_visa_123")
            .version(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
