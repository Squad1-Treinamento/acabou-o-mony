package com.acabouomony.payment.domain.entity;

import com.acabouomony.payment.domain.model.OutboxEventStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

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

    @NotNull(message = "signature cannot be null")
    @Column(nullable = false, length = 256)
    private String signature;

    @NotNull(message = "created_at cannot be null")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull(message = "updated_at cannot be null")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;
}
