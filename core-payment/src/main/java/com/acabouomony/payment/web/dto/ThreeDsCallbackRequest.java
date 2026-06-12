package com.acabouomony.payment.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThreeDsCallbackRequest {
    @JsonProperty("challenge_id")      private String challengeId;
    @JsonProperty("transaction_id")    private String transactionId;
    @JsonProperty("merchant_id")       private String merchantId;
    @JsonProperty("auth_status")       private String authStatus;
    @JsonProperty("authenticated_at")  private Instant authenticatedAt;
}
