package com.acabouomony.payment.infrastructure.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaymentMetrics Tests")
class PaymentMetricsTest {
    
    private PaymentMetrics metrics;
    
    @BeforeEach
    void setUp() {
        metrics = new PaymentMetrics();
    }
    
    @Test
    @DisplayName("Should not throw exception when recording UNKNOWN state transition")
    void testRecordUnknownStateTransition() {
        assertDoesNotThrow(() -> metrics.recordUnknownStateTransition());
    }
    
    @Test
    @DisplayName("Should not throw exception when recording timeout error")
    void testRecordTimeoutError() {
        assertDoesNotThrow(() -> metrics.recordTimeoutError());
    }
    
    @Test
    @DisplayName("Should not throw exception when recording acquirer error")
    void testRecordAcquirerError() {
        assertDoesNotThrow(() -> metrics.recordAcquirerError());
    }
    
    @Test
    @DisplayName("Should not throw exception when recording optimistic lock conflict")
    void testRecordOptimisticLockConflict() {
        assertDoesNotThrow(() -> metrics.recordOptimisticLockConflict());
    }
    
    @Test
    @DisplayName("Should not throw exception when recording state transition duration")
    void testRecordStateTransitionDuration() {
        assertDoesNotThrow(() -> metrics.recordStateTransitionDuration(100L));
    }
    
    @Test
    @DisplayName("Should not throw exception when recording Mercado Pago call duration")
    void testRecordMercadoPagoCallDuration() {
        assertDoesNotThrow(() -> metrics.recordMercadoPagoCallDuration(250L));
    }
    
    @Test
    @DisplayName("Should handle zero duration")
    void testZeroDuration() {
        assertDoesNotThrow(() -> metrics.recordStateTransitionDuration(0L));
        assertDoesNotThrow(() -> metrics.recordMercadoPagoCallDuration(0L));
    }
    
    @Test
    @DisplayName("Should handle large duration")
    void testLargeDuration() {
        assertDoesNotThrow(() -> metrics.recordStateTransitionDuration(Long.MAX_VALUE));
        assertDoesNotThrow(() -> metrics.recordMercadoPagoCallDuration(Long.MAX_VALUE));
    }
}
