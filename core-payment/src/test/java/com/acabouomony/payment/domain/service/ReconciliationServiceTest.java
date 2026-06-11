package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.config.UnknownStateProperties;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ReconciliationService Tests")
class ReconciliationServiceTest {
    
    private ReconciliationService service;
    
    @Mock
    private PaymentAcquirerClient acquirerClient;
    
    @Mock
    private ReconciliationStateTransitionHandler stateTransitionHandler;
    
    @Mock
    private AlertService alertService;
    
    private UnknownStateProperties properties;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        properties = new UnknownStateProperties();
        properties.setMaxRetryAttempts(3);
        
        service = new ReconciliationService(
            acquirerClient,
            stateTransitionHandler,
            properties,
            alertService
        );
    }
    
    @Test
    @DisplayName("Should reconcile to COMPLETED when acquirer confirms")
    void testReconcileToCompleted() {
        Transaction transaction = createTestTransaction();
        when(acquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.COMPLETED);
        
        boolean result = service.attemptReconciliation(transaction);
        
        assertTrue(result);
        verify(acquirerClient, times(1)).queryPaymentStatus(transaction.getAcquirerReference());
        verify(stateTransitionHandler, times(1)).transitionToCompleted(transaction);
        verify(stateTransitionHandler, never()).transitionToDeclined(any());
        verify(stateTransitionHandler, never()).transitionToFailed(any());
    }
    
    @Test
    @DisplayName("Should reconcile to DECLINED when acquirer rejects")
    void testReconcileToDeclined() {
        Transaction transaction = createTestTransaction();
        when(acquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.DECLINED);
        
        boolean result = service.attemptReconciliation(transaction);
        
        assertTrue(result);
        verify(acquirerClient, times(1)).queryPaymentStatus(transaction.getAcquirerReference());
        verify(stateTransitionHandler, times(1)).transitionToDeclined(transaction);
        verify(stateTransitionHandler, never()).transitionToCompleted(any());
        verify(stateTransitionHandler, never()).transitionToFailed(any());
    }
    
    @Test
    @DisplayName("Should transition to FAILED after max retries with UNKNOWN")
    void testFailedAfterMaxRetries() {
        Transaction transaction = createTestTransaction();
        when(acquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.UNKNOWN);
        
        boolean result = service.attemptReconciliation(transaction);
        
        assertTrue(result);
        verify(acquirerClient, times(3)).queryPaymentStatus(transaction.getAcquirerReference());
        verify(stateTransitionHandler, times(1)).transitionToFailed(transaction);
        verify(alertService, times(1)).alertReconciliationFailure(eq(transaction), eq(3));
    }
    
    @Test
    @DisplayName("Should skip reconciliation if not in UNKNOWN state")
    void testSkipIfNotUnknown() {
        Transaction transaction = createTestTransaction();
        transaction.setStatus(PaymentStatus.COMPLETED);
        
        boolean result = service.attemptReconciliation(transaction);
        
        assertTrue(result);
        verify(acquirerClient, never()).queryPaymentStatus(anyString());
        verify(stateTransitionHandler, never()).transitionToCompleted(any());
    }
    
    @Test
    @DisplayName("Should fail if acquirer reference is missing")
    void testFailIfNoAcquirerReference() {
        Transaction transaction = createTestTransaction();
        transaction.setAcquirerReference(null);
        
        boolean result = service.attemptReconciliation(transaction);
        
        assertTrue(result);
        verify(acquirerClient, never()).queryPaymentStatus(anyString());
        verify(stateTransitionHandler, times(1)).transitionToFailed(transaction);
    }
    
    @Test
    @DisplayName("Should handle FAILED status from acquirer")
    void testHandleFailedStatus() {
        Transaction transaction = createTestTransaction();
        when(acquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.FAILED);
        
        boolean result = service.attemptReconciliation(transaction);
        
        assertTrue(result);
        verify(stateTransitionHandler, times(1)).transitionToFailed(transaction);
    }
    
    private Transaction createTestTransaction() {
        return Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(PaymentStatus.UNKNOWN)
            .payloadHash("hash123")
            .acquirerReference("MP-12345")
            .version(1)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
