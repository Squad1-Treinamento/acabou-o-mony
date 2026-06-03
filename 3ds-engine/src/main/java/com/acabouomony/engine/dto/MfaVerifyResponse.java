package com.acabouomony.engine.dto;

public record MfaVerifyResponse(
        String status,
        String challengeId,
        String transactionId
) {
}
