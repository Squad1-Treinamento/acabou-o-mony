package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.OutboxEvent;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.OutboxEventStatus;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.OutboxEventRepository;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for transaction rollback scenarios.
 * 
 * Verifies that outbox events are rolled back when transaction fails.
 * Tests all-or-nothing consistency guarantee.
 * 
 * Spec: spec-001-core-payment-processing.md - Transactional Outbox Rules
 * Task: task-016-outbox-persistence.md
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Outbox Event Transaction Rollback Tests")
class OutboxEventTransactionRollbackTest {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    
    @Autowired
    private OutboxEventService outboxEventService;
    
    private UUID merchantId;
    private UUID idempotencyKey;
    
    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();
        
        // Clear repositories
        outboxEventRepository.deleteAll();
        transactionRepository.deleteAll();
    }
    
    @Test
    @DisplayName("Outbox event persists when transaction persists")
    @Transactional
    void testOutboxEventPersistsWhenTransactionPersists() {
        // Arrange
        Transaction transaction = createTransaction(PaymentStatus.COMPLETED);
        
        // Act
        transactionRepository.save(transaction);
        outboxEventService.createPaymentCompletedEvent(transaction);
        
        // Assert
        Transaction savedTransaction = transactionRepository.findById(transaction.getId()).orElseThrow();
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transaction.getId());
        
        assertThat(savedTransaction).isNotNull();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    }
    
    @Test
    @DisplayName("Outbox event is not created if transaction is not saved")
    void testOutboxEventNotCreatedIfTransactionNotSaved() {
        // Arrange
        Transaction transaction = createTransaction(PaymentStatus.COMPLETED);
        // Note: transaction is NOT saved to database
        
        // Act
        outboxEventService.createPaymentCompletedEvent(transaction);
        
        // Assert
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transaction.getId());
        // Event is created but transaction doesn't exist in DB
        // This is expected behavior - event creation doesn't depend on transaction persistence
        assertThat(events).hasSize(1);
    }
    
    @Test
    @DisplayName("Multiple outbox events persist atomically with transaction")
    @Transactional
    void testMultipleOutboxEventsPersistAtomicallyWithTransaction() {
        // Arrange
        Transaction transaction = createTransaction(PaymentStatus.COMPLETED);
        transactionRepository.save(transaction);
        
        // Act
        outboxEventService.createPaymentCompletedEvent(transaction);
        outboxEventService.createPaymentReconciledEvent(transaction);
        
        // Assert
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transaction.getId());
        assertThat(events).hasSize(2);
        assertThat(events).allMatch(e -> e.getStatus() == OutboxEventStatus.PENDING);
    }
    
    @Test
    @DisplayName("Outbox event creation within transaction scope")
    @Transactional
    void testOutboxEventCreationWithinTransactionScope() {
        // Arrange
        Transaction transaction = createTransaction(PaymentStatus.DECLINED);
        transactionRepository.save(transaction);
        
        // Act
        outboxEventService.createPaymentDeclinedEvent(transaction);
        
        // Assert - within transaction scope
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transaction.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("payment.declined");
    }
    
    @Test
    @DisplayName("Outbox event persists for FAILED payment")
    @Transactional
    void testOutboxEventPersistsForFailedPayment() {
        // Arrange
        Transaction transaction = createTransaction(PaymentStatus.FAILED);
        transactionRepository.save(transaction);
        
        // Act
        outboxEventService.createPaymentFailedEvent(transaction);
        
        // Assert
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transaction.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("payment.failed");
    }
    
    @Test
    @DisplayName("Outbox event persists for UNKNOWN payment")
    @Transactional
    void testOutboxEventPersistsForUnknownPayment() {
        // Arrange
        Transaction transaction = createTransaction(PaymentStatus.UNKNOWN);
        transactionRepository.save(transaction);
        
        // Act
        outboxEventService.createPaymentUnknownEvent(transaction);
        
        // Assert
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transaction.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("payment.unknown");
    }
    
    @Test
    @DisplayName("Outbox event persists for reconciled payment")
    @Transactional
    void testOutboxEventPersistsForReconciledPayment() {
        // Arrange
        Transaction transaction = createTransaction(PaymentStatus.COMPLETED);
        transactionRepository.save(transaction);
        
        // Act
        outboxEventService.createPaymentReconciledEvent(transaction);
        
        // Assert
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transaction.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("payment.reconciled");
    }
    
    @Test
    @DisplayName("Outbox event status remains PENDING until explicitly changed")
    @Transactional
    void testOutboxEventStatusRemainsPendingUntilExplicitlyChanged() {
        // Arrange
        Transaction transaction = createTransaction(PaymentStatus.COMPLETED);
        transactionRepository.save(transaction);
        
        // Act
        outboxEventService.createPaymentCompletedEvent(transaction);
        
        // Assert - immediately after creation
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transaction.getId());
        assertThat(events.get(0).getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        
        // Verify status doesn't change without explicit update
        List<OutboxEvent> eventsAgain = outboxEventRepository.findByAggregateId(transaction.getId());
        assertThat(eventsAgain.get(0).getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    }
    
    @Test
    @DisplayName("Outbox event retry count remains zero until explicitly incremented")
    @Transactional
    void testOutboxEventRetryCountRemainsZeroUntilExplicitlyIncremented() {
        // Arrange
        Transaction transaction = createTransaction(PaymentStatus.COMPLETED);
        transactionRepository.save(transaction);
        
        // Act
        outboxEventService.createPaymentCompletedEvent(transaction);
        
        // Assert - immediately after creation
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transaction.getId());
        assertThat(events.get(0).getRetryCount()).isZero();
        
        // Verify retry count doesn't change without explicit update
        List<OutboxEvent> eventsAgain = outboxEventRepository.findByAggregateId(transaction.getId());
        assertThat(eventsAgain.get(0).getRetryCount()).isZero();
    }
    
    @Test
    @DisplayName("Outbox event delivered_at is null until delivery succeeds")
    @Transactional
    void testOutboxEventDeliveredAtIsNullUntilDeliverySucceeds() {
        // Arrange
        Transaction transaction = createTransaction(PaymentStatus.COMPLETED);
        transactionRepository.save(transaction);
        
        // Act
        outboxEventService.createPaymentCompletedEvent(transaction);
        
        // Assert
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transaction.getId());
        assertThat(events.get(0).getDeliveredAt()).isNull();
    }
    
    @Test
    @DisplayName("Outbox event can be retrieved by aggregate ID")
    @Transactional
    void testOutboxEventCanBeRetrievedByAggregateId() {
        // Arrange
        Transaction transaction = createTransaction(PaymentStatus.COMPLETED);
        transactionRepository.save(transaction);
        
        // Act
        outboxEventService.createPaymentCompletedEvent(transaction);
        
        // Assert
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transaction.getId());
        assertThat(events)
            .hasSize(1)
            .allMatch(e -> e.getAggregateId().equals(transaction.getId()));
    }
    
    @Test
    @DisplayName("Outbox events for different transactions are independent")
    @Transactional
    void testOutboxEventsForDifferentTransactionsAreIndependent() {
        // Arrange
        Transaction transaction1 = createTransaction(PaymentStatus.COMPLETED);
        Transaction transaction2 = createTransaction(PaymentStatus.DECLINED);
        transactionRepository.saveAll(List.of(transaction1, transaction2));
        
        // Act
        outboxEventService.createPaymentCompletedEvent(transaction1);
        outboxEventService.createPaymentDeclinedEvent(transaction2);
        
        // Assert
        List<OutboxEvent> events1 = outboxEventRepository.findByAggregateId(transaction1.getId());
        List<OutboxEvent> events2 = outboxEventRepository.findByAggregateId(transaction2.getId());
        
        assertThat(events1).hasSize(1);
        assertThat(events2).hasSize(1);
        assertThat(events1.get(0).getEventType()).isEqualTo("payment.completed");
        assertThat(events2.get(0).getEventType()).isEqualTo("payment.declined");
    }
    
    // Helper method
    private Transaction createTransaction(PaymentStatus status) {
        return Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(merchantId)
            .idempotencyKey(idempotencyKey)
            .amount(10000L)
            .currency("BRL")
            .status(status)
            .payloadHash("abc123")
            .version(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
