package com.acabouomony.engine.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CallbackRequest(
        @JsonProperty("challenge_id") String challengeId,
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("merchant_id") String merchantId,
        @JsonProperty("auth_status") String authStatus,
        @JsonProperty("authenticated_at") Instant authenticatedAt
) {
}
