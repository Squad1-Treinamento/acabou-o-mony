package com.acabouomony.engine.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

final class AuditLogger {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AuditLogger() {
    }

    static String auditLog(String event, String challengeId, String transactionId, String merchantId) {
        var map = new LinkedHashMap<String, String>();
        map.put("event", event);
        map.put("challenge_id", challengeId != null ? challengeId : "");
        map.put("transaction_id", transactionId != null ? transactionId : "");
        map.put("merchant_id", merchantId != null ? merchantId : "");
        map.put("timestamp", Instant.now().toString());
        try {
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"event\":\"" + event + "\"}";
        }
    }
}
