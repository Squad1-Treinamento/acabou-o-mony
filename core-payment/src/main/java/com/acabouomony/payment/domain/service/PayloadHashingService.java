package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Service for deterministic payload hashing.
 * 
 * Implements SHA-256 hashing of payment request payloads to support:
 * - Idempotency validation (detect duplicate requests with different payloads)
 * - Payload integrity verification
 * - Duplicate detection across retries
 * 
 * Spec: spec-001-core-payment-processing.md - Idempotency Rules
 * Task: task-007-payload-hashing.md
 * 
 * Hash computation includes:
 * - amount
 * - currency
 * - payment_method (card_token_id, masked_card)
 * - customer_id (if provided)
 * 
 * Hash is deterministic: identical payloads always produce identical hashes.
 */
@Service
public class PayloadHashingService {
    
    private static final Logger logger = LoggerFactory.getLogger(PayloadHashingService.class);
    private static final String HASH_ALGORITHM = "SHA-256";
    private final ObjectMapper objectMapper;
    
    public PayloadHashingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Computes deterministic SHA-256 hash of a payment request payload.
     * 
     * Hash includes only fields relevant to payment idempotency:
     * - amount
     * - currency
     * - payment_method.card_token_id
     * - payment_method.masked_card
     * - customer_id
     * 
     * Hash excludes:
     * - idempotency_key (varies per request)
     * - customer_email (not part of payment identity)
     * - timestamps (not part of payment identity)
     * 
     * Payload is normalized to JSON with sorted keys to ensure determinism.
     * 
     * @param request The payment request to hash
     * @return SHA-256 hash as hexadecimal string (64 characters)
     * @throws IllegalStateException if hashing fails
     */
    public String computePayloadHash(PaymentRequest request) {
        try {
            // Build canonical payload with sorted keys
            String canonicalPayload = buildCanonicalPayload(request);
            
            // Compute SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hexadecimal string
            String hash = bytesToHex(hashBytes);
            
            logger.debug("Computed payload hash: {}", hash);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new IllegalStateException("Payload hashing failed: SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Validates that a stored payload hash matches the current request payload.
     * 
     * Used to detect duplicate requests with different payloads:
     * - Same idempotency_key + same payload_hash = identical retry (safe to return cached response)
     * - Same idempotency_key + different payload_hash = different request (error)
     * 
     * @param request The current payment request
     * @param storedHash The previously stored payload hash
     * @return true if hashes match (identical payload), false otherwise
     */
    public boolean validatePayloadHash(PaymentRequest request, String storedHash) {
        String currentHash = computePayloadHash(request);
        boolean matches = currentHash.equals(storedHash);
        
        if (!matches) {
            logger.warn("Payload hash mismatch: current={}, stored={}", currentHash, storedHash);
        }
        
        return matches;
    }
    
    /**
     * Builds canonical JSON representation of payment payload for hashing.
     * 
     * Canonical form ensures determinism:
     * - JSON keys sorted alphabetically
     * - No whitespace
     * - Null values excluded
     * - Consistent field ordering
     * 
     * Payload includes only idempotency-relevant fields:
     * - amount
     * - currency
     * - payment_method.card_token_id
     * - payment_method.masked_card
     * - customer_id
     * 
     * @param request The payment request
     * @return Canonical JSON string
     */
    private String buildCanonicalPayload(PaymentRequest request) {
        ObjectNode payload = objectMapper.createObjectNode();
        
        // Add amount (required)
        if (request.getAmount() != null) {
            payload.put("amount", request.getAmount());
        }
        
        // Add currency (required)
        if (request.getCurrency() != null) {
            payload.put("currency", request.getCurrency());
        }
        
        // Add payment method fields
        if (request.getPaymentMethod() != null) {
            ObjectNode paymentMethod = objectMapper.createObjectNode();
            
            if (request.getPaymentMethod().getCardTokenId() != null) {
                paymentMethod.put("card_token_id", request.getPaymentMethod().getCardTokenId());
            }
            
            if (request.getPaymentMethod().getMaskedCard() != null) {
                paymentMethod.put("masked_card", request.getPaymentMethod().getMaskedCard());
            }
            
            payload.set("payment_method", paymentMethod);
        }
        
        // Add customer_id (optional)
        if (request.getCustomerId() != null) {
            payload.put("customer_id", request.getCustomerId().toString());
        }
        
        // Convert to JSON string (ObjectMapper sorts keys by default)
        return payload.toString();
    }
    
    /**
     * Converts byte array to hexadecimal string.
     * 
     * @param bytes The byte array to convert
     * @return Hexadecimal string representation
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
