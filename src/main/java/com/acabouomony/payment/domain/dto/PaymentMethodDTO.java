package com.acabouomony.payment.domain.dto;

import com.acabouomony.payment.domain.model.PaymentMethodType;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentMethodDTO {

    @JsonProperty("type")
    private PaymentMethodType type;

    @JsonProperty("masked_card")
    private String maskedCard;

    @JsonProperty("card_token_id")
    private String cardTokenId;

    // Getters and Setters
    public PaymentMethodType getType() {
        return type;
    }

    public void setType(PaymentMethodType type) {
        this.type = type;
    }

    public String getMaskedCard() {
        return maskedCard;
    }

    public void setMaskedCard(String maskedCard) {
        this.maskedCard = maskedCard;
    }

    public String getCardTokenId() {
        return cardTokenId;
    }

    public void setCardTokenId(String cardTokenId) {
        this.cardTokenId = cardTokenId;
    }
}