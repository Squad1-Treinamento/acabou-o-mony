package com.acabouomony.payment.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "transactions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_merchant_id_idempotency_key", columnNames = {"merchant_id", "idempotency_key"})
    },
    indexes = {
        @Index(name = "idx_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;
    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;
    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;
    @Column(nullable = false)
    private long amount;
    @Column(length = 3, nullable = false)
    private String currency;
    @Column(length = 20, nullable = false)
    private String status;
    @Column(name = "payload_hash", length = 64, nullable = false)
    private String payloadHash;
    @Column(name = "masked_card", length = 20)
    private String maskedCard;
    @Column(name = "card_token_id", length = 100)
    private String cardTokenId;
    @Column(name = "acquirer_reference", length = 255)
    private String acquirerReference;
    @Version
    @Column(nullable = false)
    private Integer version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
