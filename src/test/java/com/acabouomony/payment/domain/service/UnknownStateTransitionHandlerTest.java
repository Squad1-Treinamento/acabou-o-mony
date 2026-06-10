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

@DisplayName("UnknownStateTransitionHandler Tests")
class UnknownStateTransitionHandlerTest {
 
 private UnknownStateTransitionHandler handler;
 
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
 
 @Mock
 private AlertService alertService;
 
 @BeforeEach
 void setUp() {
 MockitoAnnotations.openMocks(this);
 handler = new UnknownStateTransitionHandler(
 transactionRepository,
 auditLogService,
 outboxEventService,
 stateTransitionValidator,
 paymentMetrics,
 alertService
 );
 }
 
    @Test
    @DisplayName("Should transition PROCESSING -> UNKNOWN on timeout")
    void testTransitionToUnknownDueToTimeout() {
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        Transaction transaction = Transaction.builder()
            .id(transactionId)
            .merchantId(merchantId)
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(PaymentStatus.PROCESSING)
            .payloadHash("hash123")
            .version(4)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        handler.transitionToUnknownDueToTimeout(transaction, "Mercado Pago read timeout");
        
        assertEquals(PaymentStatus.UNKNOWN, transaction.getStatus());
        assertEquals(5, transaction.getVersion());
        verify(paymentMetrics, times(1)).recordTimeoutError();
        verify(alertService, times(1)).alertUnknownStateTransition(eq(transaction), eq("Mercado Pago read timeout"));
        verify(stateTransitionValidator, times(1)).validateTransition(PaymentStatus.PROCESSING, PaymentStatus.UNKNOWN);
        verify(transactionRepository, times(1)).save(transaction);
        verify(auditLogService, times(1)).logStateTransition(transaction, PaymentStatus.PROCESSING, PaymentStatus.UNKNOWN, "system");
        verify(outboxEventService, times(1)).createPaymentUnknownEvent(transaction);
        verify(paymentMetrics, times(1)).recordUnknownStateTransition();
    }
 
    @Test
    @DisplayName("Should transition PROCESSING -> UNKNOWN on acquirer error")
    void testTransitionToUnknownDueToAcquirerError() {
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        Transaction transaction = Transaction.builder()
            .id(transactionId)
            .merchantId(merchantId)
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(PaymentStatus.PROCESSING)
            .payloadHash("hash123")
            .version(4)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        handler.transitionToUnknownDueToAcquirerError(transaction, "Mercado Pago connection failed");
        
        assertEquals(PaymentStatus.UNKNOWN, transaction.getStatus());
        assertEquals(5, transaction.getVersion());
        verify(paymentMetrics, times(1)).recordAcquirerError();
        verify(alertService, times(1)).alertUnknownStateTransition(eq(transaction), eq("Mercado Pago connection failed"));
        verify(stateTransitionValidator, times(1)).validateTransition(PaymentStatus.PROCESSING, PaymentStatus.UNKNOWN);
        verify(transactionRepository, times(1)).save(transaction);
        verify(auditLogService, times(1)).logStateTransition(transaction, PaymentStatus.PROCESSING, PaymentStatus.UNKNOWN, "system");
        verify(outboxEventService, times(1)).createPaymentUnknownEvent(transaction);
        verify(paymentMetrics, times(1)).recordUnknownStateTransition();
    }
 
    @Test
    @DisplayName("Should increment version on UNKNOWN transition")
    void testVersionIncrementedOnUnknownTransition() {
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(PaymentStatus.PROCESSING)
            .payloadHash("hash123")
            .version(4)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        int originalVersion = transaction.getVersion();
        handler.transitionToUnknownDueToTimeout(transaction, "Timeout");
        
        assertEquals(originalVersion + 1, transaction.getVersion());
    }
    
    @Test
    @DisplayName("Should validate state transition")
    void testStateTransitionValidation() {
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(PaymentStatus.PROCESSING)
            .payloadHash("hash123")
            .version(4)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        handler.transitionToUnknownDueToTimeout(transaction, "Timeout");
        
        verify(stateTransitionValidator, times(1))
            .validateTransition(PaymentStatus.PROCESSING, PaymentStatus.UNKNOWN);
    }
    
    @Test
    @DisplayName("Should record metrics on UNKNOWN transition")
    void testMetricsRecorded() {
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(PaymentStatus.PROCESSING)
            .payloadHash("hash123")
            .version(4)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        handler.transitionToUnknownDueToTimeout(transaction, "Timeout");
        
        verify(paymentMetrics, times(1)).recordTimeoutError();
        verify(paymentMetrics, times(1)).recordUnknownStateTransition();
    }
    
    @Test
    @DisplayName("Should alert on UNKNOWN transition")
    void testAlertTriggered() {
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(PaymentStatus.PROCESSING)
            .payloadHash("hash123")
            .version(4)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        String reason = "Test timeout reason";
        handler.transitionToUnknownDueToTimeout(transaction, reason);
        
        verify(alertService, times(1)).alertUnknownStateTransition(eq(transaction), eq(reason));
    }
}
