package com.acabouomony.engine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MfaVerifyResponse(
        String status,
        @JsonProperty("challenge_id") String challengeId,
        @JsonProperty("transaction_id") String transactionId
) {
}
