package com.acabouomony.engine.dto;

import java.time.Instant;

public record CallbackRequest(
        String challengeId,
        String transactionId,
        String authStatus,
        Instant authenticatedAt
) {
}
