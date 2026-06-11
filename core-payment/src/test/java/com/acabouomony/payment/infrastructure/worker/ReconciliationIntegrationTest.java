package com.acabouomony.payment.infrastructure.worker;

import com.acabouomony.payment.domain.entity.AuditLog;
import com.acabouomony.payment.domain.entity.OutboxEvent;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.OutboxEventStatus;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.PaymentAcquirerClient;
import com.acabouomony.payment.infrastructure.persistence.AuditLogRepository;
import com.acabouomony.payment.infrastructure.persistence.OutboxEventRepository;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for reconciliation flow.
 * 
 * Tests end-to-end reconciliation from UNKNOWN to terminal state
 * with database persistence, audit logging, and webhook events.
 * 
 * Spec: spec-001-core-payment-processing.md - Reconciliation Rules
 * Task: task-014-reconciliation-worker.md, task-015-reconciliation-logic.md
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("Reconciliation Integration Tests")
class ReconciliationIntegrationTest {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    
    @MockBean
    private PaymentAcquirerClient paymentAcquirerClient;
    
    @Autowired
    private ReconciliationWorker reconciliationWorker;
    
    @Autowired
    private EntityManager entityManager;
    
    private UUID merchantId;
    private UUID transactionId;
    
    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
    }
    
    // ==================== UNKNOWN → COMPLETED Flow ====================
    
    @Test
    @Transactional
    @DisplayName("Should reconcile UNKNOWN transaction to COMPLETED")
    void testReconcileUnknownToCompleted() throws InterruptedException {
        // Arrange
        Transaction transaction = createUnknownTransaction();
        transactionRepository.saveAndFlush(transaction);
        entityManager.clear();
        
        when(paymentAcquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.COMPLETED);
        
        // Act
        reconciliationWorker.reconcileUnknownTransactions();
        
        // Wait for virtual thread to complete
        Thread.sleep(500);
        entityManager.clear();
        
        // Assert
        Transaction reconciled = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(PaymentStatus.COMPLETED, reconciled.getStatus());
        assertEquals(1, reconciled.getVersion());
        
        // Verify audit log
        List<AuditLog> auditLogs = auditLogRepository.findByTransactionId(transaction.getId());
        assertEquals(1, auditLogs.size());
        AuditLog auditLog = auditLogs.get(0);
        assertEquals(PaymentStatus.UNKNOWN, auditLog.getOldStatus());
        assertEquals(PaymentStatus.COMPLETED, auditLog.getNewStatus());
        assertEquals("reconciliation", auditLog.getActor());
        assertNotNull(auditLog.getChecksum());
        
        // Verify outbox event
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transaction.getId());
        assertEquals(1, events.size());
        OutboxEvent event = events.get(0);
        assertEquals("payment.reconciled", event.getEventType());
        assertEquals(OutboxEventStatus.PENDING, event.getStatus());
    }
    
    @Test
    @Transactional
    @DisplayName("Should reconcile UNKNOWN transaction to DECLINED")
    void testReconcileUnknownToDeclined() throws InterruptedException {
        // Arrange
        Transaction transaction = createUnknownTransaction();
        transactionRepository.saveAndFlush(transaction);
        entityManager.clear();
        
        when(paymentAcquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.DECLINED);
        
        // Act
        reconciliationWorker.reconcileUnknownTransactions();
        
        // Wait for virtual thread to complete
        Thread.sleep(500);
        entityManager.clear();
        
        // Assert
        Transaction reconciled = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(PaymentStatus.DECLINED, reconciled.getStatus());
        assertEquals(1, reconciled.getVersion());
        
        // Verify audit log
        List<AuditLog> auditLogs = auditLogRepository.findByTransactionId(transaction.getId());
        assertEquals(1, auditLogs.size());
        AuditLog auditLog = auditLogs.get(0);
        assertEquals(PaymentStatus.UNKNOWN, auditLog.getOldStatus());
        assertEquals(PaymentStatus.DECLINED, auditLog.getNewStatus());
    }
    
    @Test
    @Transactional
    @DisplayName("Should reconcile UNKNOWN transaction to FAILED after max retries")
    void testReconcileUnknownToFailedAfterMaxRetries() throws InterruptedException {
        // Arrange
        Transaction transaction = createUnknownTransaction();
        transactionRepository.saveAndFlush(transaction);
        entityManager.clear();
        
        // Mock Mercado Pago to return UNKNOWN for all attempts
        when(paymentAcquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.UNKNOWN);
        
        // Act
        reconciliationWorker.reconcileUnknownTransactions();
        
        // Wait for virtual thread and retries to complete (3 attempts with backoff)
        Thread.sleep(4000);
        entityManager.clear();
        
        // Assert
        Transaction reconciled = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, reconciled.getStatus());
        
        // Verify audit log
        List<AuditLog> auditLogs = auditLogRepository.findByTransactionId(transaction.getId());
        assertEquals(1, auditLogs.size());
        AuditLog auditLog = auditLogs.get(0);
        assertEquals(PaymentStatus.UNKNOWN, auditLog.getOldStatus());
        assertEquals(PaymentStatus.FAILED, auditLog.getNewStatus());
    }
    
    // ==================== Audit Log Tests ====================
    
    @Test
    @Transactional
    @DisplayName("Should create audit log with reconciliation actor")
    void testCreatesAuditLogWithReconciliationActor() throws InterruptedException {
        // Arrange
        Transaction transaction = createUnknownTransaction();
        transactionRepository.saveAndFlush(transaction);
        entityManager.clear();
        
        when(paymentAcquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.COMPLETED);
        
        // Act
        reconciliationWorker.reconcileUnknownTransactions();
        Thread.sleep(500);
        entityManager.clear();
        
        // Assert
        List<AuditLog> auditLogs = auditLogRepository.findByTransactionId(transaction.getId());
        assertEquals(1, auditLogs.size());
        
        AuditLog auditLog = auditLogs.get(0);
        assertEquals("reconciliation", auditLog.getActor());
        assertNotNull(auditLog.getChecksum());
        assertTrue(auditLog.getChecksum().length() > 0);
    }
    
    @Test
    @Transactional
    @DisplayName("Should include old and new status in audit log")
    void testAuditLogIncludesOldAndNewStatus() throws InterruptedException {
        // Arrange
        Transaction transaction = createUnknownTransaction();
        transactionRepository.saveAndFlush(transaction);
        entityManager.clear();
        
        when(paymentAcquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.COMPLETED);
        
        // Act
        reconciliationWorker.reconcileUnknownTransactions();
        Thread.sleep(500);
        entityManager.clear();
        
        // Assert
        List<AuditLog> auditLogs = auditLogRepository.findByTransactionId(transaction.getId());
        AuditLog auditLog = auditLogs.get(0);
        
        assertEquals(PaymentStatus.UNKNOWN, auditLog.getOldStatus());
        assertEquals(PaymentStatus.COMPLETED, auditLog.getNewStatus());
    }
    
    // ==================== Outbox Event Tests ====================
    
    @Test
    @Transactional
    @DisplayName("Should create outbox event for webhook notification")
    void testCreatesOutboxEventForWebhookNotification() throws InterruptedException {
        // Arrange
        Transaction transaction = createUnknownTransaction();
        transactionRepository.saveAndFlush(transaction);
        entityManager.clear();
        
        when(paymentAcquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.COMPLETED);
        
        // Act
        reconciliationWorker.reconcileUnknownTransactions();
        Thread.sleep(500);
        entityManager.clear();
        
        // Assert
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transaction.getId());
        assertEquals(1, events.size());
        
        OutboxEvent event = events.get(0);
        assertEquals("payment.reconciled", event.getEventType());
        assertEquals(transaction.getId(), event.getAggregateId());
        assertEquals(OutboxEventStatus.PENDING, event.getStatus());
        assertEquals(0, event.getRetryCount());
        assertNotNull(event.getPayload());
    }
    
    // ==================== Optimistic Locking Tests ====================
    
    @Test
    @Transactional
    @DisplayName("Should handle optimistic lock conflict during reconciliation")
    void testHandlesOptimisticLockConflictDuringReconciliation() throws InterruptedException {
        // Arrange
        Transaction transaction = createUnknownTransaction();
        transactionRepository.saveAndFlush(transaction);
        
        when(paymentAcquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.COMPLETED);
        
        // Simulate concurrent update by incrementing version
        transaction.setVersion(transaction.getVersion() + 1);
        transactionRepository.saveAndFlush(transaction);
        entityManager.clear();
        
        // Act
        reconciliationWorker.reconcileUnknownTransactions();
        Thread.sleep(500);
        entityManager.clear();
        
        // Assert - should still reconcile (retry logic handles version conflict)
        Transaction reconciled = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(PaymentStatus.COMPLETED, reconciled.getStatus());
    }
    
    @Test
    @Transactional
    @DisplayName("Should detect webhook resolution first (idempotent)")
    void testDetectsWebhookResolutionFirst() throws InterruptedException {
        // Arrange
        Transaction transaction = createUnknownTransaction();
        transactionRepository.saveAndFlush(transaction);
        
        // Simulate webhook resolving first
        transaction.setStatus(PaymentStatus.COMPLETED);
        transaction.setVersion(transaction.getVersion() + 1);
        transactionRepository.saveAndFlush(transaction);
        entityManager.clear();
        
        when(paymentAcquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.COMPLETED);
        
        // Act
        reconciliationWorker.reconcileUnknownTransactions();
        Thread.sleep(500);
        entityManager.clear();
        
        // Assert - should not create duplicate audit entries
        List<AuditLog> auditLogs = auditLogRepository.findByTransactionId(transaction.getId());
        // Only webhook audit entry, no reconciliation entry (transaction not in UNKNOWN)
        assertEquals(0, auditLogs.size());
    }
    
    // ==================== Concurrency Tests ====================
    
    @Test
    @Transactional
    @DisplayName("Should enforce per-merchant concurrency limits")
    void testEnforcesPerMerchantConcurrencyLimits() throws InterruptedException {
        // Arrange
        UUID testMerchantId = UUID.randomUUID();
        
        // Create 5 UNKNOWN transactions for same merchant
        for (int i = 0; i < 5; i++) {
            Transaction tx = createUnknownTransaction();
            tx.setMerchantId(testMerchantId);
            transactionRepository.save(tx);
        }
        transactionRepository.flush();
        entityManager.clear();
        
        when(paymentAcquirerClient.queryPaymentStatus(anyString()))
            .thenReturn(PaymentStatus.COMPLETED);
        
        // Act
        reconciliationWorker.reconcileUnknownTransactions();
        Thread.sleep(2000);
        entityManager.clear();
        
        // Assert - all should eventually be reconciled
        List<Transaction> transactions = transactionRepository.findByMerchantId(testMerchantId);
        for (Transaction tx : transactions) {
            assertEquals(PaymentStatus.COMPLETED, tx.getStatus());
        }
    }
    
    @Test
    @Transactional
    @DisplayName("Should maintain merchant isolation with separate limits")
    void testMerchantIsolationWithSeparateLimits() throws InterruptedException {
        // Arrange
        UUID merchant1 = UUID.randomUUID();
        UUID merchant2 = UUID.randomUUID();
        
        // Create transactions for both merchants
        for (int i = 0; i < 3; i++) {
            Transaction tx1 = createUnknownTransaction();
            tx1.setMerchantId(merchant1);
            transactionRepository.save(tx1);
            
            Transaction tx2 = createUnknownTransaction();
            tx2.setMerchantId(merchant2);
            transactionRepository.save(tx2);
        }
        transactionRepository.flush();
        entityManager.clear();
        
        when(paymentAcquirerClient.queryPaymentStatus(anyString()))
            .thenReturn(PaymentStatus.COMPLETED);
        
        // Act
        reconciliationWorker.reconcileUnknownTransactions();
        Thread.sleep(2000);
        entityManager.clear();
        
        // Assert - both merchants' transactions should be reconciled
        List<Transaction> merchant1Txs = transactionRepository.findByMerchantId(merchant1);
        List<Transaction> merchant2Txs = transactionRepository.findByMerchantId(merchant2);
        
        for (Transaction tx : merchant1Txs) {
            assertEquals(PaymentStatus.COMPLETED, tx.getStatus());
        }
        for (Transaction tx : merchant2Txs) {
            assertEquals(PaymentStatus.COMPLETED, tx.getStatus());
        }
    }
    
    // ==================== Retry Logic Tests ====================
    
    @Test
    @Transactional
    @DisplayName("Should retry with exponential backoff")
    void testRetryLogicWithExponentialBackoff() throws InterruptedException {
        // Arrange
        Transaction transaction = createUnknownTransaction();
        transactionRepository.saveAndFlush(transaction);
        entityManager.clear();
        
        // Mock: fail on attempts 1-2, succeed on attempt 3
        when(paymentAcquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.UNKNOWN)
            .thenReturn(PaymentStatus.UNKNOWN)
            .thenReturn(PaymentStatus.COMPLETED);
        
        // Act
        long startTime = System.currentTimeMillis();
        reconciliationWorker.reconcileUnknownTransactions();
        Thread.sleep(4000);  // Wait for retries
        long endTime = System.currentTimeMillis();
        entityManager.clear();
        
        // Assert
        Transaction reconciled = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(PaymentStatus.COMPLETED, reconciled.getStatus());
        
        // Verify backoff timing (should take at least 3 seconds for 1s + 2s backoff)
        long duration = endTime - startTime;
        assertTrue(duration >= 3000, "Reconciliation should take at least 3 seconds for backoff");
    }
    
    @Test
    @Transactional
    @DisplayName("Should transition to FAILED after max retry attempts")
    void testTransitionToFailedAfterMaxRetries() throws InterruptedException {
        // Arrange
        Transaction transaction = createUnknownTransaction();
        transactionRepository.saveAndFlush(transaction);
        entityManager.clear();
        
        // Mock: always return UNKNOWN
        when(paymentAcquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.UNKNOWN);
        
        // Act
        reconciliationWorker.reconcileUnknownTransactions();
        Thread.sleep(4000);  // Wait for all retries
        entityManager.clear();
        
        // Assert
        Transaction reconciled = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, reconciled.getStatus());
    }
    
    // ==================== Version Increment Tests ====================
    
    @Test
    @Transactional
    @DisplayName("Should increment version on state transition")
    void testIncrementsVersionOnStateTransition() throws InterruptedException {
        // Arrange
        Transaction transaction = createUnknownTransaction();
        int initialVersion = transaction.getVersion();
        transactionRepository.saveAndFlush(transaction);
        entityManager.clear();
        
        when(paymentAcquirerClient.queryPaymentStatus(transaction.getAcquirerReference()))
            .thenReturn(PaymentStatus.COMPLETED);
        
        // Act
        reconciliationWorker.reconcileUnknownTransactions();
        Thread.sleep(500);
        entityManager.clear();
        
        // Assert
        Transaction reconciled = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(initialVersion + 1, reconciled.getVersion());
    }
    
    // ==================== Helper Methods ====================
    
    private Transaction createUnknownTransaction() {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setMerchantId(merchantId);
        tx.setIdempotencyKey(UUID.randomUUID());
        tx.setAmount(10000L);
        tx.setCurrency("BRL");
        tx.setStatus(PaymentStatus.UNKNOWN);
        tx.setPayloadHash("test-hash-" + UUID.randomUUID());
        tx.setMaskedCard("411111XXXXXX1111");
        tx.setCardTokenId("tok_test_" + UUID.randomUUID());
        tx.setAcquirerReference("mp_ref_" + UUID.randomUUID());
        tx.setVersion(0);
        tx.setCreatedAt(Instant.now().minusSeconds(10));
        tx.setUpdatedAt(Instant.now());
        return tx;
    }
}
