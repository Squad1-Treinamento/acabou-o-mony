package com.acabouomony.payment.domain.entity;

import com.acabouomony.payment.domain.model.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit log entry for transaction state transitions.
 * 
 * This entity is INSERT-ONLY; UPDATE operations are prevented by database trigger.
 * Spec: spec-001-core-payment-processing.md - Audit Log Persistence
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "transaction cannot be null")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private PaymentStatus oldStatus;

    @NotNull(message = "newStatus cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20, nullable = false)
    private PaymentStatus newStatus;

    @NotNull(message = "actor cannot be null")
    @Column(nullable = false, length = 50)
    private String actor;

    @NotNull(message = "checksum cannot be null")
    @Column(nullable = false, length = 64)
    private String checksum;

    @NotNull(message = "created_at cannot be null")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
