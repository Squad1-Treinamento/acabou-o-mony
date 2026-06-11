package com.acabouomony.payment.infrastructure.worker;

import com.acabouomony.payment.config.UnknownStateProperties;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.AlertService;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StaleUnknownMonitor.
 * 
 * Tests stale transaction detection, alerting, and operator notification.
 * 
 * Spec: spec-001-core-payment-processing.md - Stale UNKNOWN Detection
 * Task: task-013-unknown-state-handling.md
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StaleUnknownMonitor Unit Tests")
class StaleUnknownMonitorTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private UnknownStateProperties properties;
    
    @Mock
    private AlertService alertService;
    
    private StaleUnknownMonitor monitor;
    
    @BeforeEach
    void setUp() {
        monitor = new StaleUnknownMonitor(
            transactionRepository,
            properties,
            alertService
        );
    }
    
    // ==================== Detection Tests ====================
    
    @Test
    @DisplayName("Should detect UNKNOWN transactions older than threshold")
    void testDetectsUnknownTransactionsOlderThanThreshold() {
        // Arrange
        when(properties.getStaleThresholdMinutes()).thenReturn(5L);
        
        List<Transaction> staleTransactions = List.of(createStaleTransaction(10));
        when(transactionRepository.findStaleUnknownTransactions(
            any(PaymentStatus.class),
            any(Instant.class)
        )).thenReturn(staleTransactions);
        
        // Act
        monitor.detectStaleTransactions();
        
        // Assert
        verify(transactionRepository).findStaleUnknownTransactions(
            eq(PaymentStatus.UNKNOWN),
            any(Instant.class)
        );
    }
    
    @Test
    @DisplayName("Should ignore UNKNOWN transactions younger than threshold")
    void testIgnoresUnknownTransactionsYoungerThanThreshold() {
        // Arrange
        when(properties.getStaleThresholdMinutes()).thenReturn(5L);
        when(transactionRepository.findStaleUnknownTransactions(
            any(PaymentStatus.class),
            any(Instant.class)
        )).thenReturn(new ArrayList<>());
        
        // Act
        monitor.detectStaleTransactions();
        
        // Assert
        verify(alertService, never()).alertStaleUnknownTransaction(any(), anyLong());
    }
    
    @Test
    @DisplayName("Should ignore non-UNKNOWN transactions")
    void testIgnoresNonUnknownTransactions() {
        // Arrange
        when(properties.getStaleThresholdMinutes()).thenReturn(5L);
        when(transactionRepository.findStaleUnknownTransactions(
            any(PaymentStatus.class),
            any(Instant.class)
        )).thenReturn(new ArrayList<>());
        
        // Act
        monitor.detectStaleTransactions();
        
        // Assert
        verify(alertService, never()).alertStaleUnknownTransaction(any(), anyLong());
    }
    
    @Test
    @DisplayName("Should respect stale threshold configuration")
    void testRespectsStaleThresholdConfiguration() {
        // Arrange
        long thresholdMinutes = 10L;
        when(properties.getStaleThresholdMinutes()).thenReturn(thresholdMinutes);
        when(transactionRepository.findStaleUnknownTransactions(
            any(PaymentStatus.class),
            any(Instant.class)
        )).thenReturn(new ArrayList<>());
        
        Instant beforeCall = Instant.now();
        
        // Act
        monitor.detectStaleTransactions();
        
        // Assert
        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(transactionRepository).findStaleUnknownTransactions(
            any(PaymentStatus.class),
            thresholdCaptor.capture()
        );
        
        Instant capturedThreshold = thresholdCaptor.getValue();
        Instant expectedThreshold = beforeCall.minus(Duration.ofMinutes(thresholdMinutes));
        
        // Threshold should be approximately thresholdMinutes in the past
        assertTrue(capturedThreshold.isBefore(beforeCall));
        assertTrue(capturedThreshold.isAfter(expectedThreshold.minusSeconds(1)));
    }
    
    // ==================== Alerting Tests ====================
    
    @Test
    @DisplayName("Should alert operator for stale transaction")
    void testAlertsOperatorForStaleTransaction() {
        // Arrange
        when(properties.getStaleThresholdMinutes()).thenReturn(5L);
        
        Transaction staleTransaction = createStaleTransaction(10);
        List<Transaction> staleTransactions = List.of(staleTransaction);
        
        when(transactionRepository.findStaleUnknownTransactions(
            any(PaymentStatus.class),
            any(Instant.class)
        )).thenReturn(staleTransactions);
        
        // Act
        monitor.detectStaleTransactions();
        
        // Assert
        verify(alertService).alertStaleUnknownTransaction(
            eq(staleTransaction),
            anyLong()
        );
    }
    
    @Test
    @DisplayName("Should include age in minutes in alert")
    void testIncludesAgeInMinutesInAlert() {
        // Arrange
        when(properties.getStaleThresholdMinutes()).thenReturn(5L);
        
        Transaction staleTransaction = createStaleTransaction(15);
        List<Transaction> staleTransactions = List.of(staleTransaction);
        
        when(transactionRepository.findStaleUnknownTransactions(
            any(PaymentStatus.class),
            any(Instant.class)
        )).thenReturn(staleTransactions);
        
        // Act
        monitor.detectStaleTransactions();
        
        // Assert
        ArgumentCaptor<Long> ageCaptor = ArgumentCaptor.forClass(Long.class);
        verify(alertService).alertStaleUnknownTransaction(
            any(Transaction.class),
            ageCaptor.capture()
        );
        
        long capturedAge = ageCaptor.getValue();
        assertTrue(capturedAge >= 14 && capturedAge <= 16, 
            "Age should be approximately 15 minutes");
    }
    
    @Test
    @DisplayName("Should alert for multiple stale transactions")
    void testAlertsForMultipleStaleTransactions() {
        // Arrange
        when(properties.getStaleThresholdMinutes()).thenReturn(5L);
        
        List<Transaction> staleTransactions = List.of(
            createStaleTransaction(10),
            createStaleTransaction(15),
            createStaleTransaction(20)
        );
        
        when(transactionRepository.findStaleUnknownTransactions(
            any(PaymentStatus.class),
            any(Instant.class)
        )).thenReturn(staleTransactions);
        
        // Act
        monitor.detectStaleTransactions();
        
        // Assert
        verify(alertService, times(3)).alertStaleUnknownTransaction(
            any(Transaction.class),
            anyLong()
        );
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    @DisplayName("Should handle repository exception gracefully")
    void testHandlesRepositoryExceptionGracefully() {
        // Arrange
        when(properties.getStaleThresholdMinutes()).thenReturn(5L);
        when(transactionRepository.findStaleUnknownTransactions(
            any(PaymentStatus.class),
            any(Instant.class)
        )).thenThrow(new RuntimeException("Database error"));
        
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> monitor.detectStaleTransactions());
    }
    
    @Test
    @DisplayName("Should handle alert service exception gracefully")
    void testHandlesAlertServiceExceptionGracefully() {
        // Arrange
        when(properties.getStaleThresholdMinutes()).thenReturn(5L);
        
        Transaction staleTransaction = createStaleTransaction(10);
        List<Transaction> staleTransactions = List.of(staleTransaction);
        
        when(transactionRepository.findStaleUnknownTransactions(
            any(PaymentStatus.class),
            any(Instant.class)
        )).thenReturn(staleTransactions);
        
        doThrow(new RuntimeException("Alert service error"))
            .when(alertService).alertStaleUnknownTransaction(any(), anyLong());
        
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> monitor.detectStaleTransactions());
    }
    
    @Test
    @DisplayName("Should continue processing on single alert failure")
    void testContinuesProcessingOnSingleAlertFailure() {
        // Arrange
        when(properties.getStaleThresholdMinutes()).thenReturn(5L);
        
        Transaction tx1 = createStaleTransaction(10);
        Transaction tx2 = createStaleTransaction(15);
        List<Transaction> staleTransactions = List.of(tx1, tx2);
        
        when(transactionRepository.findStaleUnknownTransactions(
            any(PaymentStatus.class),
            any(Instant.class)
        )).thenReturn(staleTransactions);
        
        doThrow(new RuntimeException("Alert service error"))
            .doNothing()
            .when(alertService).alertStaleUnknownTransaction(any(), anyLong());
        
        // Act
        assertDoesNotThrow(() -> monitor.detectStaleTransactions());
        
        // Assert - both transactions should be alerted
        verify(alertService, times(2)).alertStaleUnknownTransaction(
            any(Transaction.class),
            anyLong()
        );
    }
    
    // ==================== Polling Tests ====================
    
    @Test
    @DisplayName("Should run detection when called")
    void testRunsDetectionWhenCalled() {
        // Arrange
        when(properties.getStaleThresholdMinutes()).thenReturn(5L);
        when(transactionRepository.findStaleUnknownTransactions(
            any(PaymentStatus.class),
            any(Instant.class)
        )).thenReturn(new ArrayList<>());
        
        // Act
        monitor.detectStaleTransactions();
        
        // Assert
        verify(transactionRepository).findStaleUnknownTransactions(
            any(PaymentStatus.class),
            any(Instant.class)
        );
    }
    
    // ==================== Helper Methods ====================
    
    private Transaction createStaleTransaction(int ageMinutes) {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setMerchantId(UUID.randomUUID());
        tx.setIdempotencyKey(UUID.randomUUID());
        tx.setAmount(10000L);
        tx.setCurrency("BRL");
        tx.setStatus(PaymentStatus.UNKNOWN);
        tx.setPayloadHash("test-hash-" + UUID.randomUUID());
        tx.setMaskedCard("411111XXXXXX1111");
        tx.setCardTokenId("tok_test_" + UUID.randomUUID());
        tx.setAcquirerReference("mp_ref_" + UUID.randomUUID());
        tx.setVersion(0);
        tx.setCreatedAt(Instant.now().minus(Duration.ofMinutes(ageMinutes)));
        tx.setUpdatedAt(Instant.now());
        return tx;
    }
}
