package com.acabouomony.payment.domain.entity;

import com.acabouomony.payment.domain.model.OutboxEventStatus;
import com.acabouomony.payment.infrastructure.persistence.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for OutboxEvent entity persistence.
 * 
 * Verifies:
 * - Outbox event creation with valid status
 * - Invalid status rejection (CHECK constraint)
 * - Retry count constraint (0-5 range)
 * - Status transitions
 * - Signature field persisted correctly
 * - Delivered_at nullable initially
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class OutboxEventEntityTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldCreateOutboxEventWithValidStatus() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        Instant now = Instant.now();

        OutboxEvent event = OutboxEvent.builder()
            .id(eventId)
            .eventType("payment.completed")
            .aggregateId(aggregateId)
            .payload("{\"transaction_id\": \"123\"}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .signature("hmac_signature_xyz")
            .createdAt(now)
            .updatedAt(now)
            .build();

        // Act
        OutboxEvent saved = outboxEventRepository.save(event);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(eventId);
        assertThat(saved.getEventType()).isEqualTo("payment.completed");
        assertThat(saved.getAggregateId()).isEqualTo(aggregateId);
        assertThat(saved.getPayload()).isEqualTo("{\"transaction_id\": \"123\"}");
        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(saved.getRetryCount()).isEqualTo(0);
        assertThat(saved.getSignature()).isEqualTo("hmac_signature_xyz");
        assertThat(saved.getDeliveredAt()).isNull();
    }

    @Test
    void shouldRejectOutboxEventWithInvalidStatus() {
        // This test verifies the CHECK constraint at database level
        // We can't directly test invalid enum values in Java, but we can verify
        // that only valid statuses are accepted
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(null)
            .retryCount(0)
            .signature("sig")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> outboxEventRepository.save(event))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldEnforceRetryCountMinimum() {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(-1)
            .signature("sig")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> outboxEventRepository.save(event))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldEnforceRetryCountMaximum() {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(6)
            .signature("sig")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> outboxEventRepository.save(event))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowRetryCountInValidRange() {
        // Test all valid retry counts: 0-5
        for (int retryCount = 0; retryCount <= 5; retryCount++) {
            OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .eventType("payment.completed")
                .aggregateId(UUID.randomUUID())
                .payload("{}")
                .status(OutboxEventStatus.PENDING)
                .retryCount(retryCount)
                .signature("sig")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            OutboxEvent saved = outboxEventRepository.save(event);
            assertThat(saved.getRetryCount()).isEqualTo(retryCount);
        }
    }

    @Test
    void shouldTransitionFromPendingToDelivered() {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .signature("sig")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        OutboxEvent saved = outboxEventRepository.save(event);

        // Act
        saved.setStatus(OutboxEventStatus.DELIVERED);
        Instant deliveredAt = Instant.now();
        saved.setDeliveredAt(deliveredAt);
        saved.setUpdatedAt(deliveredAt);
        OutboxEvent updated = outboxEventRepository.save(saved);

        // Assert
        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.DELIVERED);
        assertThat(updated.getDeliveredAt()).isEqualTo(deliveredAt);
    }

    @Test
    void shouldTransitionFromPendingToFailed() {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(5)
            .signature("sig")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        OutboxEvent saved = outboxEventRepository.save(event);

        // Act
        saved.setStatus(OutboxEventStatus.FAILED);
        saved.setUpdatedAt(Instant.now());
        OutboxEvent updated = outboxEventRepository.save(saved);

        // Assert
        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
    }

    @Test
    void shouldPersistSignatureCorrectly() {
        // Arrange
        String signature = "hmac_sha256_signature_abc123def456";
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .signature(signature)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act
        OutboxEvent saved = outboxEventRepository.save(event);

        // Assert
        assertThat(saved.getSignature()).isEqualTo(signature);
    }

    @Test
    void shouldAllowNullDeliveredAtInitially() {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .signature("sig")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .deliveredAt(null)
            .build();

        // Act
        OutboxEvent saved = outboxEventRepository.save(event);

        // Assert
        assertThat(saved.getDeliveredAt()).isNull();
    }

    @Test
    void shouldPopulateDeliveredAtOnDelivery() {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .signature("sig")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        OutboxEvent saved = outboxEventRepository.save(event);

        // Act
        Instant deliveredAt = Instant.now();
        saved.setStatus(OutboxEventStatus.DELIVERED);
        saved.setDeliveredAt(deliveredAt);
        saved.setUpdatedAt(deliveredAt);
        OutboxEvent updated = outboxEventRepository.save(saved);

        // Assert
        assertThat(updated.getDeliveredAt()).isEqualTo(deliveredAt);
    }

    @Test
    void shouldRejectOutboxEventWithoutEventType() {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType(null)
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .signature("sig")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> outboxEventRepository.save(event))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectOutboxEventWithoutAggregateId() {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(null)
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .signature("sig")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> outboxEventRepository.save(event))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectOutboxEventWithoutPayload() {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload(null)
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .signature("sig")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> outboxEventRepository.save(event))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectOutboxEventWithoutSignature() {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .signature(null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> outboxEventRepository.save(event))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldSupportAllValidOutboxEventStatuses() {
        // Verify that all valid outbox event statuses can be persisted
        OutboxEventStatus[] allStatuses = OutboxEventStatus.values();

        for (OutboxEventStatus status : allStatuses) {
            OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .eventType("payment.completed")
                .aggregateId(UUID.randomUUID())
                .payload("{}")
                .status(status)
                .retryCount(0)
                .signature("sig")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            OutboxEvent saved = outboxEventRepository.save(event);
            assertThat(saved.getStatus()).isEqualTo(status);
        }
    }
}
