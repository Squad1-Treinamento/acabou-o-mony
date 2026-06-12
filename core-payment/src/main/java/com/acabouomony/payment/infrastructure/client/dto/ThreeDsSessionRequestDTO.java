package com.acabouomony.payment.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ThreeDsSessionRequestDTO {
    @JsonProperty("transaction_id") private String transactionId;
    @JsonProperty("merchant_id")    private String merchantId;
    @JsonProperty("amount")         private long amount;
    @JsonProperty("currency")       private String currency;
    @JsonProperty("card_token")     private String cardToken;
}
