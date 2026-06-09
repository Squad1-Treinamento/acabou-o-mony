package com.acabouomony.payment.infrastructure.persistence;

import com.acabouomony.payment.domain.entity.OutboxEvent;
import com.acabouomony.payment.domain.model.OutboxEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for OutboxEventRepository.
 * 
 * Verifies repository query methods and persistence behavior.
 * 
 * Spec: spec-001-core-payment-processing.md - Transactional Outbox Rules
 * Task: task-016-outbox-persistence.md
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("OutboxEventRepository Tests")
class OutboxEventRepositoryTest {
    
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    
    private UUID transactionId1;
    private UUID transactionId2;
    
    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        transactionId1 = UUID.randomUUID();
        transactionId2 = UUID.randomUUID();
    }
    
    @Test
    @DisplayName("Save and retrieve outbox event by ID")
    void testSaveAndRetrieveOutboxEventById() {
        // Arrange
        OutboxEvent event = createOutboxEvent(transactionId1, "payment.completed", OutboxEventStatus.PENDING);
        
        // Act
        OutboxEvent saved = outboxEventRepository.save(event);
        Optional<OutboxEvent> retrieved = outboxEventRepository.findById(saved.getId());
        
        // Assert
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(saved.getId());
        assertThat(retrieved.get().getEventType()).isEqualTo("payment.completed");
    }
    
    @Test
    @DisplayName("Find all pending outbox events")
    void testFindAllPendingOutboxEvents() {
        // Arrange
        OutboxEvent pending1 = createOutboxEvent(transactionId1, "payment.completed", OutboxEventStatus.PENDING);
        OutboxEvent pending2 = createOutboxEvent(transactionId2, "payment.declined", OutboxEventStatus.PENDING);
        OutboxEvent delivered = createOutboxEvent(transactionId1, "payment.delivered", OutboxEventStatus.DELIVERED);
        
        outboxEventRepository.saveAll(List.of(pending1, pending2, delivered));
        
        // Act
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(OutboxEventStatus.PENDING);
        
        // Assert
        assertThat(pendingEvents).hasSize(2);
        assertThat(pendingEvents).allMatch(e -> e.getStatus() == OutboxEventStatus.PENDING);
    }
    
    @Test
    @DisplayName("Find all failed outbox events ordered by created_at")
    void testFindAllFailedOutboxEventsOrderedByCreatedAt() {
        // Arrange
        OutboxEvent failed1 = createOutboxEvent(transactionId1, "payment.failed", OutboxEventStatus.FAILED);
        OutboxEvent failed2 = createOutboxEvent(transactionId2, "payment.failed", OutboxEventStatus.FAILED);
        
        outboxEventRepository.saveAll(List.of(failed1, failed2));
        
        // Act
        List<OutboxEvent> failedEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.FAILED);
        
        // Assert
        assertThat(failedEvents).hasSize(2);
        assertThat(failedEvents).allMatch(e -> e.getStatus() == OutboxEventStatus.FAILED);
    }
    
    @Test
    @DisplayName("Find all outbox events for specific aggregate")
    void testFindAllOutboxEventsForSpecificAggregate() {
        // Arrange
        OutboxEvent event1 = createOutboxEvent(transactionId1, "payment.completed", OutboxEventStatus.PENDING);
        OutboxEvent event2 = createOutboxEvent(transactionId1, "payment.reconciled", OutboxEventStatus.PENDING);
        OutboxEvent event3 = createOutboxEvent(transactionId2, "payment.completed", OutboxEventStatus.PENDING);
        
        outboxEventRepository.saveAll(List.of(event1, event2, event3));
        
        // Act
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(transactionId1);
        
        // Assert
        assertThat(events).hasSize(2);
        assertThat(events).allMatch(e -> e.getAggregateId().equals(transactionId1));
    }
    
    @Test
    @DisplayName("Update outbox event status")
    void testUpdateOutboxEventStatus() {
        // Arrange
        OutboxEvent event = createOutboxEvent(transactionId1, "payment.completed", OutboxEventStatus.PENDING);
        OutboxEvent saved = outboxEventRepository.save(event);
        
        // Act
        saved.setStatus(OutboxEventStatus.DELIVERED);
        saved.setDeliveredAt(Instant.now());
        OutboxEvent updated = outboxEventRepository.save(saved);
        
        // Assert
        Optional<OutboxEvent> retrieved = outboxEventRepository.findById(updated.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getStatus()).isEqualTo(OutboxEventStatus.DELIVERED);
        assertThat(retrieved.get().getDeliveredAt()).isNotNull();
    }
    
    @Test
    @DisplayName("Increment outbox event retry count")
    void testIncrementOutboxEventRetryCount() {
        // Arrange
        OutboxEvent event = createOutboxEvent(transactionId1, "payment.completed", OutboxEventStatus.PENDING);
        OutboxEvent saved = outboxEventRepository.save(event);
        
        // Act
        saved.setRetryCount(saved.getRetryCount() + 1);
        OutboxEvent updated = outboxEventRepository.save(saved);
        
        // Assert
        Optional<OutboxEvent> retrieved = outboxEventRepository.findById(updated.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getRetryCount()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Delete outbox event")
    void testDeleteOutboxEvent() {
        // Arrange
        OutboxEvent event = createOutboxEvent(transactionId1, "payment.completed", OutboxEventStatus.PENDING);
        OutboxEvent saved = outboxEventRepository.save(event);
        
        // Act
        outboxEventRepository.deleteById(saved.getId());
        Optional<OutboxEvent> retrieved = outboxEventRepository.findById(saved.getId());
        
        // Assert
        assertThat(retrieved).isEmpty();
    }
    
    @Test
    @DisplayName("Find outbox events returns empty list when no matches")
    void testFindOutboxEventsReturnsEmptyListWhenNoMatches() {
        // Act
        List<OutboxEvent> events = outboxEventRepository.findByStatus(OutboxEventStatus.DELIVERED);
        
        // Assert
        assertThat(events).isEmpty();
    }
    
    @Test
    @DisplayName("Find outbox events by aggregate ID returns empty list when no matches")
    void testFindOutboxEventsByAggregateIdReturnsEmptyListWhenNoMatches() {
        // Act
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(UUID.randomUUID());
        
        // Assert
        assertThat(events).isEmpty();
    }
    
    @Test
    @DisplayName("Outbox event can be persisted with large payload")
    void testOutboxEventCanBePersistedWithLargePayload() {
        // Arrange
        StringBuilder largePayload = new StringBuilder("{");
        for (int i = 0; i < 1000; i++) {
            largePayload.append("\"key").append(i).append("\": \"value").append(i).append("\",");
        }
        largePayload.append("\"final\": \"value\"}");
        
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(transactionId1)
            .payload(largePayload.toString())
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        // Act
        OutboxEvent saved = outboxEventRepository.save(event);
        Optional<OutboxEvent> retrieved = outboxEventRepository.findById(saved.getId());
        
        // Assert
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getPayload()).isEqualTo(largePayload.toString());
    }
    
    @Test
    @DisplayName("Multiple outbox events can be saved and retrieved")
    void testMultipleOutboxEventsSavedAndRetrieved() {
        // Arrange
        List<OutboxEvent> events = List.of(
            createOutboxEvent(transactionId1, "payment.completed", OutboxEventStatus.PENDING),
            createOutboxEvent(transactionId1, "payment.reconciled", OutboxEventStatus.PENDING),
            createOutboxEvent(transactionId2, "payment.declined", OutboxEventStatus.PENDING),
            createOutboxEvent(transactionId2, "payment.failed", OutboxEventStatus.FAILED)
        );
        
        // Act
        List<OutboxEvent> saved = outboxEventRepository.saveAll(events);
        
        // Assert
        assertThat(saved).hasSize(4);
        assertThat(outboxEventRepository.findAll()).hasSize(4);
    }
    
    // Helper method
    private OutboxEvent createOutboxEvent(UUID aggregateId, String eventType, OutboxEventStatus status) {
        return OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType(eventType)
            .aggregateId(aggregateId)
            .payload("{\"test\": \"payload\"}")
            .status(status)
            .retryCount(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
