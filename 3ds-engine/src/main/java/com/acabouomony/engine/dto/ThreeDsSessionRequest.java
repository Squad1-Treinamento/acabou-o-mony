package com.acabouomony.engine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ThreeDsSessionRequest(
        @NotBlank @JsonProperty("transaction_id") String transactionId,
        @NotBlank @JsonProperty("merchant_id")    String merchantId,
        @NotNull  @JsonProperty("amount")          Long amount,
        @NotBlank @JsonProperty("currency")        String currency,
        @NotBlank @JsonProperty("card_token")      String cardToken
) {}
