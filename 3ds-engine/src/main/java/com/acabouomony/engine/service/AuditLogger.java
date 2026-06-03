package com.acabouomony.engine.service;

import java.time.Instant;

final class AuditLogger {

    private AuditLogger() {
    }

    static String auditLog(String event, String challengeId, String transactionId, String merchantId) {
        return "{\"event\":\"" + event + "\""
                + ",\"challenge_id\":\"" + (challengeId != null ? challengeId : "") + "\""
                + ",\"transaction_id\":\"" + (transactionId != null ? transactionId : "") + "\""
                + ",\"merchant_id\":\"" + (merchantId != null ? merchantId : "") + "\""
                + ",\"timestamp\":\"" + Instant.now() + "\"}";
    }
}
