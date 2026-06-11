package com.acabouomony.payment.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Service for generating and verifying HMAC-SHA256 signatures for webhook payloads.
 * 
 * Webhooks are signed using merchant-specific secret key.
 * Merchants verify signature using shared secret to ensure authenticity.
 * 
 * Spec: spec-001-core-payment-processing.md - Webhook Idempotency
 * Task: task-017-webhook-dispatch-worker.md
 * 
 * Signature Algorithm:
 * - Algorithm: HMAC-SHA256
 * - Key: Merchant webhook secret (from secure configuration)
 * - Message: Webhook payload (JSON string)
 * - Encoding: Base64 (for HTTP header compatibility)
 * 
 * Header Format:
 * X-Signature: <base64-encoded-hmac-sha256>
 * 
 * Merchant Verification:
 * 1. Extract X-Signature header
 * 2. Compute HMAC-SHA256(payload, secret)
 * 3. Compare with X-Signature (constant-time comparison)
 * 4. If match: payload is authentic
 * 5. If mismatch: reject webhook (potential tampering)
 */
@Service
public class WebhookSignatureService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookSignatureService.class);
    
    private static final String ALGORITHM = "HmacSHA256";
    
    /**
     * Generates HMAC-SHA256 signature for webhook payload.
     * 
     * @param payload The webhook payload (JSON string)
     * @param secret The merchant webhook secret
     * @return Base64-encoded signature
     * @throws RuntimeException if signature generation fails
     */
    public String generateSignature(String payload, String secret) {
        try {
            // Create HMAC-SHA256 instance
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                0,
                secret.length(),
                ALGORITHM
            );
            mac.init(keySpec);
            
            // Compute signature
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            
            // Encode as Base64
            return Base64.getEncoder().encodeToString(signature);
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("HMAC-SHA256 algorithm not available: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate signature: algorithm not available", e);
        } catch (InvalidKeyException e) {
            logger.error("Invalid key for HMAC-SHA256: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate signature: invalid key", e);
        }
    }
    
    /**
     * Verifies webhook signature using constant-time comparison.
     * 
     * Prevents timing attacks by comparing all bytes even if mismatch detected early.
     * 
     * @param payload The webhook payload (JSON string)
     * @param secret The merchant webhook secret
     * @param providedSignature The signature to verify (Base64-encoded)
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(String payload, String secret, String providedSignature) {
        try {
            // Generate expected signature
            String expectedSignature = generateSignature(payload, secret);
            
            // Constant-time comparison to prevent timing attacks
            return constantTimeEquals(expectedSignature, providedSignature);
            
        } catch (Exception e) {
            logger.error("Error verifying signature: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Constant-time string comparison.
     * 
     * Compares all bytes even if mismatch detected early.
     * Prevents timing attacks where attacker could infer correct bytes.
     * 
     * @param a First string
     * @param b Second string
     * @return true if strings are equal, false otherwise
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;  // Both null or one null
        }
        
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        
        return constantTimeEquals(aBytes, bBytes);
    }
    
    /**
     * Constant-time byte array comparison.
     * 
     * @param a First byte array
     * @param b Second byte array
     * @return true if arrays are equal, false otherwise
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        
        return result == 0;
    }
}
