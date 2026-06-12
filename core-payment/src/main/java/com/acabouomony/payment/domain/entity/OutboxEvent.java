package com.acabouomony.payment.domain.entity;

import com.acabouomony.payment.domain.model.OutboxEventStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Outbox event entity for transactional webhook delivery.
 * 
 * Outbox events are persisted atomically with transaction state changes.
 * Webhook worker polls for PENDING events and dispatches asynchronously.
 * 
 * Spec: spec-001-core-payment-processing.md - Transactional Outbox Rules
 * Task: task-016-outbox-persistence.md
 * 
 * Lifecycle:
 * 1. Event created with status PENDING (same transaction as payment update)
 * 2. Webhook worker polls for PENDING events
 * 3. Worker dispatches to merchant webhook endpoint
 * 4. On success (200 OK): mark as DELIVERED
 * 5. On failure: retry with exponential backoff (max 5 retries)
 * 6. After 5 retries: mark as FAILED and alert operator
 * 
 * Independent Versioning:
 * - Outbox events have INDEPENDENT version field
 * - NOT versioned with parent transaction
 * - Allows safe replay and concurrent updates
 */
@Entity
@Table(
    name = "outbox_events",
    indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OutboxEvent {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "eventType cannot be null")
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @NotNull(message = "aggregateId cannot be null")
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @NotNull(message = "payload cannot be null")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @NotNull(message = "status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @NotNull(message = "retryCount cannot be null")
    @Min(value = 0, message = "retryCount must be >= 0")
    @Max(value = 5, message = "retryCount must be <= 5")
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @NotNull(message = "created_at cannot be null")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull(message = "updated_at cannot be null")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "signature", length = 255)
    private String signature;
}
