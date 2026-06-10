package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentResult;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.PaymentAcquirerException;
import com.acabouomony.payment.domain.exception.PaymentTimeoutException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PaymentOrchestrationService Timeout Handling Tests")
class PaymentOrchestrationServiceTimeoutTest {
    
    private PaymentOrchestrationService orchestrationService;
    
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
    
    @Mock
    private UnknownStateTransitionHandler unknownStateTransitionHandler;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orchestrationService = new PaymentOrchestrationService(
            transactionRepository,
            paymentAcquirerClient,
            riskEvaluationService,
            stateTransitionValidator,
            auditLogService,
            outboxEventService,
            unknownStateTransitionHandler
        );
    }
 
    @Test
    @DisplayName("Should handle PaymentTimeoutException and transition to UNKNOWN")
    void testHandlePaymentTimeoutException() {
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        Transaction transaction = Transaction.builder()
            .id(transactionId)
            .merchantId(merchantId)
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash123")
            .version(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        when(riskEvaluationService.isHighRisk(any())).thenReturn(false);
        when(paymentAcquirerClient.submitPayment(any()))
            .thenThrow(new PaymentTimeoutException("Mercado Pago read timeout"));
        
        orchestrationService.processPayment(transaction);
        
        verify(unknownStateTransitionHandler, times(1))
            .transitionToUnknownDueToTimeout(transaction, "Mercado Pago read timeout");
    }
 
    @Test
    @DisplayName("Should handle PaymentAcquirerException and transition to UNKNOWN")
    void testHandlePaymentAcquirerException() {
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        Transaction transaction = Transaction.builder()
            .id(transactionId)
            .merchantId(merchantId)
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash123")
            .version(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        when(riskEvaluationService.isHighRisk(any())).thenReturn(false);
        when(paymentAcquirerClient.submitPayment(any()))
            .thenThrow(new PaymentAcquirerException("Mercado Pago connection failed"));
        
        orchestrationService.processPayment(transaction);
        
        verify(unknownStateTransitionHandler, times(1))
            .transitionToUnknownDueToAcquirerError(transaction, "Mercado Pago connection failed");
    }
 
    @Test
    @DisplayName("Should handle successful payment completion")
    void testHandleSuccessfulPayment() {
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        Transaction transaction = Transaction.builder()
            .id(transactionId)
            .merchantId(merchantId)
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash123")
            .version(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        PaymentResult result = PaymentResult.builder()
            .status(PaymentStatus.COMPLETED)
            .acquirerReference("mp_123456")
            .message("Payment approved")
            .timestamp(Instant.now())
            .build();
        
        when(riskEvaluationService.isHighRisk(any())).thenReturn(false);
        when(paymentAcquirerClient.submitPayment(any())).thenReturn(result);
        
        orchestrationService.processPayment(transaction);
        
        assertEquals("mp_123456", transaction.getAcquirerReference());
        verify(outboxEventService, times(1)).createPaymentCompletedEvent(transaction);
    }
}
