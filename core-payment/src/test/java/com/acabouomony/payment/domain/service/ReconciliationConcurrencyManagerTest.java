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

@DisplayName("ReconciliationConcurrencyManager Tests")
class ReconciliationConcurrencyManagerTest {
    
    private ReconciliationConcurrencyManager manager;
    
    @Mock
    private AlertService alertService;
    
    private UnknownStateProperties properties;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        properties = new UnknownStateProperties();
        properties.setMaxConcurrentPerMerchant(2);
        properties.setMaxQueueDepthPerMerchant(10);
        
        manager = new ReconciliationConcurrencyManager(properties, alertService);
    }
    
    @Test
    @DisplayName("Should acquire slot when under capacity")
    void testAcquireSlotUnderCapacity() {
        UUID merchantId = UUID.randomUUID();
        
        boolean acquired = manager.tryAcquire(merchantId);
        
        assertTrue(acquired);
        assertEquals(1, manager.getConcurrentCount(merchantId));
    }
    
    @Test
    @DisplayName("Should acquire multiple slots up to limit")
    void testAcquireMultipleSlotsUpToLimit() {
        UUID merchantId = UUID.randomUUID();
        
        boolean first = manager.tryAcquire(merchantId);
        boolean second = manager.tryAcquire(merchantId);
        boolean third = manager.tryAcquire(merchantId);
        
        assertTrue(first);
        assertTrue(second);
        assertFalse(third);  // Should fail at capacity
        assertEquals(2, manager.getConcurrentCount(merchantId));
    }
    
    @Test
    @DisplayName("Should release slot and decrement count")
    void testReleaseSlot() {
        UUID merchantId = UUID.randomUUID();
        
        manager.tryAcquire(merchantId);
        manager.tryAcquire(merchantId);
        assertEquals(2, manager.getConcurrentCount(merchantId));
        
        manager.release(merchantId);
        assertEquals(1, manager.getConcurrentCount(merchantId));
        
        manager.release(merchantId);
        assertEquals(0, manager.getConcurrentCount(merchantId));
    }
    
    @Test
    @DisplayName("Should queue transaction when at capacity")
    void testQueueTransactionAtCapacity() {
        UUID merchantId = UUID.randomUUID();
        Transaction transaction = createTestTransaction(merchantId);
        
        boolean queued = manager.queue(transaction);
        
        assertTrue(queued);
        assertEquals(1, manager.getQueueSize(merchantId));
    }
    
    @Test
    @DisplayName("Should reject queue when at max depth")
    void testRejectQueueAtMaxDepth() {
        UUID merchantId = UUID.randomUUID();
        
        // Fill queue to max depth
        for (int i = 0; i < 10; i++) {
            Transaction transaction = createTestTransaction(merchantId);
            manager.queue(transaction);
        }
        
        // Try to add one more
        Transaction overflow = createTestTransaction(merchantId);
        boolean queued = manager.queue(overflow);
        
        assertFalse(queued);
        assertEquals(10, manager.getQueueSize(merchantId));
        verify(alertService, times(1)).alertReconciliationQueueOverflow(eq(merchantId), eq(10));
    }
    
    @Test
    @DisplayName("Should poll transaction from queue")
    void testPollTransactionFromQueue() {
        UUID merchantId = UUID.randomUUID();
        Transaction transaction = createTestTransaction(merchantId);
        
        manager.queue(transaction);
        assertEquals(1, manager.getQueueSize(merchantId));
        
        Transaction polled = manager.poll(merchantId);
        
        assertNotNull(polled);
        assertEquals(transaction.getId(), polled.getId());
        assertEquals(0, manager.getQueueSize(merchantId));
    }
    
    @Test
    @DisplayName("Should return null when polling empty queue")
    void testPollEmptyQueue() {
        UUID merchantId = UUID.randomUUID();
        
        Transaction polled = manager.poll(merchantId);
        
        assertNull(polled);
    }
    
    @Test
    @DisplayName("Should isolate merchants")
    void testMerchantIsolation() {
        UUID merchant1 = UUID.randomUUID();
        UUID merchant2 = UUID.randomUUID();
        
        manager.tryAcquire(merchant1);
        manager.tryAcquire(merchant1);
        manager.tryAcquire(merchant2);
        
        assertEquals(2, manager.getConcurrentCount(merchant1));
        assertEquals(1, manager.getConcurrentCount(merchant2));
    }
    
    private Transaction createTestTransaction(UUID merchantId) {
        return Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(merchantId)
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
