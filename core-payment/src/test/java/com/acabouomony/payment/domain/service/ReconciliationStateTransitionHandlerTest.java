package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.monitoring.PaymentMetrics;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ReconciliationStateTransitionHandler Tests")
class ReconciliationStateTransitionHandlerTest {
    
    private ReconciliationStateTransitionHandler handler;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AuditLogService auditLogService;
    
    @Mock
    private OutboxEventService outboxEventService;
    
    @Mock
    private StateTransitionValidator stateTransitionValidator;
    
    @Mock
    private PaymentMetrics paymentMetrics;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new ReconciliationStateTransitionHandler(
            transactionRepository,
            auditLogService,
            outboxEventService,
            stateTransitionValidator,
            paymentMetrics
        );
    }
    
    @Test
    @DisplayName("Should transition UNKNOWN -> COMPLETED")
    void testTransitionToCompleted() {
        Transaction transaction = createTestTransaction(PaymentStatus.UNKNOWN);
        
        handler.transitionToCompleted(transaction);
        
        assertEquals(PaymentStatus.COMPLETED, transaction.getStatus());
        assertEquals(2, transaction.getVersion());
        verify(stateTransitionValidator, times(1)).validateTransition(PaymentStatus.UNKNOWN, PaymentStatus.COMPLETED);
        verify(transactionRepository, times(1)).save(transaction);
        verify(auditLogService, times(1)).logStateTransition(transaction, PaymentStatus.UNKNOWN, PaymentStatus.COMPLETED, "reconciliation");
        verify(outboxEventService, times(1)).createPaymentReconciledEvent(transaction);
    }
    
    @Test
    @DisplayName("Should transition UNKNOWN -> DECLINED")
    void testTransitionToDeclined() {
        Transaction transaction = createTestTransaction(PaymentStatus.UNKNOWN);
        
        handler.transitionToDeclined(transaction);
        
        assertEquals(PaymentStatus.DECLINED, transaction.getStatus());
        assertEquals(2, transaction.getVersion());
        verify(stateTransitionValidator, times(1)).validateTransition(PaymentStatus.UNKNOWN, PaymentStatus.DECLINED);
        verify(transactionRepository, times(1)).save(transaction);
        verify(auditLogService, times(1)).logStateTransition(transaction, PaymentStatus.UNKNOWN, PaymentStatus.DECLINED, "reconciliation");
        verify(outboxEventService, times(1)).createPaymentReconciledEvent(transaction);
    }
    
    @Test
    @DisplayName("Should transition UNKNOWN -> FAILED")
    void testTransitionToFailed() {
        Transaction transaction = createTestTransaction(PaymentStatus.UNKNOWN);
        
        handler.transitionToFailed(transaction);
        
        assertEquals(PaymentStatus.FAILED, transaction.getStatus());
        assertEquals(2, transaction.getVersion());
        verify(stateTransitionValidator, times(1)).validateTransition(PaymentStatus.UNKNOWN, PaymentStatus.FAILED);
        verify(transactionRepository, times(1)).save(transaction);
        verify(auditLogService, times(1)).logStateTransition(transaction, PaymentStatus.UNKNOWN, PaymentStatus.FAILED, "reconciliation");
        verify(outboxEventService, times(1)).createPaymentReconciledEvent(transaction);
    }
    
    @Test
    @DisplayName("Should increment version on transition")
    void testVersionIncremented() {
        Transaction transaction = createTestTransaction(PaymentStatus.UNKNOWN);
        int originalVersion = transaction.getVersion();
        
        handler.transitionToCompleted(transaction);
        
        assertEquals(originalVersion + 1, transaction.getVersion());
    }
    
    @Test
    @DisplayName("Should use reconciliation actor")
    void testReconciliationActor() {
        Transaction transaction = createTestTransaction(PaymentStatus.UNKNOWN);
        
        handler.transitionToCompleted(transaction);
        
        verify(auditLogService, times(1)).logStateTransition(
            eq(transaction),
            eq(PaymentStatus.UNKNOWN),
            eq(PaymentStatus.COMPLETED),
            eq("reconciliation")
        );
    }
    
    private Transaction createTestTransaction(PaymentStatus status) {
        return Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(status)
            .payloadHash("hash123")
            .version(1)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
