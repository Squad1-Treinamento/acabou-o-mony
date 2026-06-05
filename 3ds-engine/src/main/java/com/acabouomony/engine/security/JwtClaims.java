package com.acabouomony.engine.security;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Mapped claims from a JWT payload.
 *
 * JWT claims use snake_case (e.g. challenge_id, transaction_id).
 * This record maps them to camelCase for idiomatic Java usage.
 */
public record JwtClaims(
        String challengeId,
        String transactionId,
        String merchantId,
        BigDecimal amount,
        Instant issuedAt,
        Instant expiresAt
) {
}
