package com.acabouomony.engine.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChallengeInitRequest(
        @NotBlank @JsonProperty("transaction_id") String transactionId,
        @NotBlank @JsonProperty("merchant_id") String merchantId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotBlank @JsonProperty("card_token") String cardToken,
        @NotBlank @JsonProperty("acs_url") String acsUrl
) {
}
