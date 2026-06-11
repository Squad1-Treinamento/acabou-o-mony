package com.acabouomony.payment.infrastructure.monitoring;

import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LogBasedAlertService Tests")
class LogBasedAlertServiceTest {
    
    private LogBasedAlertService alertService;
    
    @BeforeEach
    void setUp() {
        alertService = new LogBasedAlertService();
    }
    
    @Test
    @DisplayName("Should not throw exception on UNKNOWN state alert")
    void testAlertUnknownStateTransition() {
        Transaction transaction = createTestTransaction();
        
        assertDoesNotThrow(() -> 
            alertService.alertUnknownStateTransition(transaction, "Test reason")
        );
    }
    
    @Test
    @DisplayName("Should not throw exception on optimistic lock failure alert")
    void testAlertOptimisticLockFailure() {
        Transaction transaction = createTestTransaction();
        
        assertDoesNotThrow(() -> 
            alertService.alertOptimisticLockFailure(transaction, 3)
        );
    }
    
    @Test
    @DisplayName("Should not throw exception on reconciliation failure alert")
    void testAlertReconciliationFailure() {
        Transaction transaction = createTestTransaction();
        
        assertDoesNotThrow(() -> 
            alertService.alertReconciliationFailure(transaction, 3)
        );
    }
    
    @Test
    @DisplayName("Should not throw exception on stale UNKNOWN alert")
    void testAlertStaleUnknownTransaction() {
        Transaction transaction = createTestTransaction();
        
        assertDoesNotThrow(() -> 
            alertService.alertStaleUnknownTransaction(transaction, 10L)
        );
    }
    
    @Test
    @DisplayName("Should not throw exception on queue overflow alert")
    void testAlertReconciliationQueueOverflow() {
        UUID merchantId = UUID.randomUUID();
        
        assertDoesNotThrow(() -> 
            alertService.alertReconciliationQueueOverflow(merchantId, 15)
        );
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
            .version(1)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
