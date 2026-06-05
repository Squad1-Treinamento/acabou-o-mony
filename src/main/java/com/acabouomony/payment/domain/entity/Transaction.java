package com.acabouomony.payment.domain.entity;

import com.acabouomony.payment.domain.model.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_merchant_id_created_at", columnList = "merchant_id, created_at")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "merchant_id cannot be null")
    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @NotNull(message = "idempotency_key cannot be null")
    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @NotNull(message = "amount cannot be null")
    @Min(value = 1, message = "amount must be greater than 0")
    @Column(nullable = false)
    private long amount;

    @NotNull(message = "currency cannot be null")
    @Size(min = 3, max = 3, message = "currency must be exactly 3 characters (ISO 4217)")
    @Column(length = 3, nullable = false)
    private String currency;

    @NotNull(message = "status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PaymentStatus status;

    @NotNull(message = "payload_hash cannot be null")
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

    @NotNull(message = "created_at cannot be null")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull(message = "updated_at cannot be null")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
