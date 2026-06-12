package com.acabouomony.payment.domain.entity;

import com.acabouomony.payment.domain.model.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.antlr.v4.runtime.misc.NotNull;

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

    @NotNull
    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @NotNull
    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @NotNull
    @Min(value = 1, message = "amount must be greater than 0")
    @Column(nullable = false)
    private long amount;

    @NotNull
    @Size(min = 3, max = 3, message = "currency must be exactly 3 characters (ISO 4217)")
    @Column(length = 3, nullable = false)
    private String currency;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PaymentStatus status;

    @NotNull
    @Column(name = "payload_hash", length = 64, nullable = false)
    private String payloadHash;

    @Column(name = "masked_card", length = 20)
    private String maskedCard;

    @Column(name = "card_token_id", length = 100)
    private String cardTokenId;

    @Column(name = "acquirer_reference", length = 255)
    private String acquirerReference;

    @Column(name = "challenge_id", length = 36)
    private String challengeId;

    @Column(name = "challenge_acs_url", length = 512)
    private String challengeAcsUrl;

    @Version
    @Column(nullable = false)
    private Integer version;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
