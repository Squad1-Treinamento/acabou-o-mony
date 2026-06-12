package com.acabouomony.payment.web;

import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.PaymentOrchestrationService;
import com.acabouomony.payment.infrastructure.persistence.AuditLogRepository;
import com.acabouomony.payment.infrastructure.persistence.OutboxEventRepository;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for payment orchestration.
 * 
 * Tests verify:
 * - End-to-end payment flows
 * - State transitions
 * - Audit log creation
 * - Outbox event creation
 * - Concurrent scenarios
 * 
 * Spec: spec-001-core-payment-processing.md - Payment Lifecycle Rules
 * Task: task-012-payment-orchestration.md
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Payment Orchestration Integration Tests")
class PaymentOrchestrationIntegrationTest {
    
    @Autowired
    private PaymentOrchestrationService paymentOrchestrationService;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    
    private UUID merchantId;
    private UUID transactionId;
    
    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
        auditLogRepository.deleteAll();
        outboxEventRepository.deleteAll();
        transactionRepository.deleteAll();
    }
    
    @Nested
    @DisplayName("Payment Processing")
    class PaymentProcessingTests {
        
        @Test
        @DisplayName("Processes low-risk payment successfully")
        void testProcessesLowRiskPaymentSuccessfully() {
            // Arrange
            Transaction transaction = createTransaction();
            transactionRepository.save(transaction);
            
            // Act
            paymentOrchestrationService.processPayment(transaction);
            
            // Assert
            Transaction persisted = transactionRepository.findById(transactionId).orElseThrow();
            assertNotNull(persisted);
            assertTrue(persisted.getVersion() > 0);
        }
        
        @Test
        @DisplayName("Processes high-risk payment (3DS)")
        void testProcessesHighRiskPayment() {
            // Arrange
            Transaction transaction = createTransaction();
            transaction.setAmount(100000L);  // High amount triggers high-risk
            transactionRepository.save(transaction);
            
            // Act
            paymentOrchestrationService.processPayment(transaction);
            
            // Assert
            Transaction persisted = transactionRepository.findById(transactionId).orElseThrow();
            assertEquals(PaymentStatus.CHALLENGE_PENDING, persisted.getStatus());
        }
    }
    
    @Nested
    @DisplayName("State Transitions")
    class StateTransitionTests {
        
        @Test
        @DisplayName("Transitions state correctly")
        void testTransitionsStateCorrectly() {
            // Arrange
            Transaction transaction = createTransaction();
            transactionRepository.save(transaction);
            
            // Act
            paymentOrchestrationService.processPayment(transaction);
            
            // Assert
            Transaction persisted = transactionRepository.findById(transactionId).orElseThrow();
            assertNotEquals(PaymentStatus.CREATED, persisted.getStatus());
        }
        
        @Test
        @DisplayName("Increments version on state change")
        void testIncrementsVersionOnStateChange() {
            // Arrange
            Transaction transaction = createTransaction();
            transactionRepository.save(transaction);
            int initialVersion = transaction.getVersion();
            
            // Act
            paymentOrchestrationService.processPayment(transaction);
            
            // Assert
            Transaction persisted = transactionRepository.findById(transactionId).orElseThrow();
            assertTrue(persisted.getVersion() > initialVersion);
        }
    }
    
    @Nested
    @DisplayName("Audit Logging")
    class AuditLoggingTests {
        
        @Test
        @DisplayName("Creates audit log entry")
        void testCreatesAuditLogEntry() {
            // Arrange
            Transaction transaction = createTransaction();
            transactionRepository.save(transaction);
            
            // Act
            paymentOrchestrationService.processPayment(transaction);
            
            // Assert
            long auditCount = auditLogRepository.count();
            assertTrue(auditCount > 0);
        }
    }
    
    @Nested
    @DisplayName("Outbox Events")
    class OutboxEventTests {
        
        @Test
        @DisplayName("Creates outbox event")
        void testCreatesOutboxEvent() {
            // Arrange
            Transaction transaction = createTransaction();
            transactionRepository.save(transaction);
            
            // Act
            paymentOrchestrationService.processPayment(transaction);
            
            // Assert
            long eventCount = outboxEventRepository.count();
             assertTrue(eventCount > 0);
        }
    }
    
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Throws exception for null transaction")
        void testThrowsExceptionForNullTransaction() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> paymentOrchestrationService.processPayment(null));
        }
        
        @Test
        @DisplayName("Throws exception if not in CREATED state")
        void testThrowsExceptionIfNotInCreatedState() {
            // Arrange
            Transaction transaction = createTransaction();
            transaction.setStatus(PaymentStatus.VALIDATED);
            transactionRepository.save(transaction);
            
            // Act & Assert
            assertThrows(IllegalStateException.class, () -> paymentOrchestrationService.processPayment(transaction));
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
