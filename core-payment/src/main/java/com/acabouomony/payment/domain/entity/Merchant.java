package com.acabouomony.payment.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "merchants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_merchant_id", columnNames = {"merchant_id"})
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Merchant {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;
    @Column(name = "merchant_id", nullable = false, unique = true)
    private UUID merchantId;
    @Column(name = "api_key_hash", nullable = false, length = 255)
    private String apiKeyHash;
    @Column(name = "webhook_url", length = 255)
    private String webhookUrl;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
