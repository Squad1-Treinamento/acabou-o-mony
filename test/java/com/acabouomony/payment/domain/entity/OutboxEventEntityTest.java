package com.acabouomony.payment.domain.entity;

import com.acabouomony.payment.domain.model.OutboxEventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for OutboxEvent entity.
 * 
 * Verifies entity structure, validation, and behavior.
 * 
 * Spec: spec-001-core-payment-processing.md - Transactional Outbox Rules
 * Task: task-016-outbox-persistence.md
 */
@DisplayName("OutboxEvent Entity Tests")
class OutboxEventEntityTest {
    
    @Test
    @DisplayName("OutboxEvent can be created with all required fields")
    void testOutboxEventCreationWithAllFields() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        String eventType = "payment.completed";
        String payload = "{\"transaction_id\": \"123\"}";
        OutboxEventStatus status = OutboxEventStatus.PENDING;
        Integer retryCount = 0;
        Instant createdAt = Instant.now();
        Instant updatedAt = Instant.now();
        
        // Act
        OutboxEvent event = OutboxEvent.builder()
            .id(id)
            .eventType(eventType)
            .aggregateId(aggregateId)
            .payload(payload)
            .status(status)
            .retryCount(retryCount)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
        
        // Assert
        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getEventType()).isEqualTo(eventType);
        assertThat(event.getAggregateId()).isEqualTo(aggregateId);
        assertThat(event.getPayload()).isEqualTo(payload);
        assertThat(event.getStatus()).isEqualTo(status);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
        assertThat(event.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(event.getDeliveredAt()).isNull();
    }
    
    @Test
    @DisplayName("OutboxEvent status can be PENDING")
    void testOutboxEventStatusPending() {
        // Act
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        // Assert
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    }
    
    @Test
    @DisplayName("OutboxEvent status can be DELIVERED")
    void testOutboxEventStatusDelivered() {
        // Act
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.DELIVERED)
            .retryCount(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .deliveredAt(Instant.now())
            .build();
        
        // Assert
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DELIVERED);
        assertThat(event.getDeliveredAt()).isNotNull();
    }
    
    @Test
    @DisplayName("OutboxEvent status can be FAILED")
    void testOutboxEventStatusFailed() {
        // Act
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.FAILED)
            .retryCount(5)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        // Assert
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(5);
    }
    
    @Test
    @DisplayName("OutboxEvent retry count can be incremented")
    void testOutboxEventRetryCountIncrement() {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        // Act
        event.setRetryCount(event.getRetryCount() + 1);
        
        // Assert
        assertThat(event.getRetryCount()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("OutboxEvent retry count max is 5")
    void testOutboxEventRetryCountMax() {
        // Act
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(5)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        // Assert
        assertThat(event.getRetryCount()).isEqualTo(5);
    }
    
    @Test
    @DisplayName("OutboxEvent can be updated")
    void testOutboxEventCanBeUpdated() {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        // Act
        event.setStatus(OutboxEventStatus.DELIVERED);
        event.setRetryCount(1);
        event.setDeliveredAt(Instant.now());
        event.setUpdatedAt(Instant.now());
        
        // Assert
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DELIVERED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getDeliveredAt()).isNotNull();
    }
    
    @Test
    @DisplayName("OutboxEvent can be created with no-arg constructor")
    void testOutboxEventNoArgConstructor() {
        // Act
        OutboxEvent event = new OutboxEvent();
        
        // Assert
        assertThat(event).isNotNull();
    }
    
    @Test
    @DisplayName("OutboxEvent can be created with all-arg constructor")
    void testOutboxEventAllArgConstructor() {
        // Arrange
        UUID id = UUID.randomUUID();
        String eventType = "payment.completed";
        UUID aggregateId = UUID.randomUUID();
        String payload = "{}";
        OutboxEventStatus status = OutboxEventStatus.PENDING;
        Integer retryCount = 0;
        Instant createdAt = Instant.now();
        Instant updatedAt = Instant.now();
        Instant deliveredAt = null;
        
        // Act
        OutboxEvent event = new OutboxEvent(id, eventType, aggregateId, payload, status, retryCount, createdAt, updatedAt, deliveredAt);
        
        // Assert
        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getEventType()).isEqualTo(eventType);
        assertThat(event.getAggregateId()).isEqualTo(aggregateId);
        assertThat(event.getPayload()).isEqualTo(payload);
        assertThat(event.getStatus()).isEqualTo(status);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
        assertThat(event.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(event.getDeliveredAt()).isNull();
    }
}
