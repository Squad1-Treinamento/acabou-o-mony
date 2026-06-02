package com.acabouomony.engine.model;

import java.math.BigDecimal;
import java.time.Instant;

public class ChallengeSession {

    private String transactionId;
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private String cardToken;
    private String acsUrl;
    private String status;
    private Instant createdAt;
    private long ttl;

    public ChallengeSession() {
    }

    public ChallengeSession(String transactionId, String merchantId, BigDecimal amount,
                            String currency, String cardToken, String acsUrl,
                            String status, Instant createdAt, long ttl) {
        this.transactionId = transactionId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.cardToken = cardToken;
        this.acsUrl = acsUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.ttl = ttl;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }

    public String getAcsUrl() { return acsUrl; }
    public void setAcsUrl(String acsUrl) { this.acsUrl = acsUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public long getTtl() { return ttl; }
    public void setTtl(long ttl) { this.ttl = ttl; }

}
