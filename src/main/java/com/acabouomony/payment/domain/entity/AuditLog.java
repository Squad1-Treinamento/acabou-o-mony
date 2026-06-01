package com.acabouomony.payment.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;
    @Column(name = "old_status", length = 20)
    private String oldStatus;
    @Column(name = "new_status", length = 20, nullable = false)
    private String newStatus;
    @Column(nullable = false, length = 50)
    private String actor;
    @Column(nullable = false, length = 64)
    private String checksum;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
