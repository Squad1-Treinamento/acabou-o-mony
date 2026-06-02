package com.acabouomony.engine.security;

import java.math.BigDecimal;
import java.time.Instant;

public record JwtClaims(
        String challengeId,
        String transactionId,
        String merchantId,
        BigDecimal amount,
        Instant issuedAt,
        Instant expiresAt
) {
}
