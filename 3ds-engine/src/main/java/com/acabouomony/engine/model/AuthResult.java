package com.acabouomony.engine.model;

import java.time.Instant;

public class AuthResult {

    private String authStatus;
    private String challengeId;
    private String transactionId;
    private Instant authenticatedAt;

    public AuthResult() {
    }

    public AuthResult(String authStatus, String challengeId,
                      String transactionId, Instant authenticatedAt) {
        this.authStatus = authStatus;
        this.challengeId = challengeId;
        this.transactionId = transactionId;
        this.authenticatedAt = authenticatedAt;
    }

    public String getAuthStatus() { return authStatus; }
    public void setAuthStatus(String authStatus) { this.authStatus = authStatus; }

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Instant getAuthenticatedAt() { return authenticatedAt; }
    public void setAuthenticatedAt(Instant authenticatedAt) { this.authenticatedAt = authenticatedAt; }

}
