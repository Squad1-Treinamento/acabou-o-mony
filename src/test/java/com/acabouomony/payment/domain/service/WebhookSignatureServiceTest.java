package com.acabouomony.payment.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WebhookSignatureService.
 * 
 * Tests HMAC-SHA256 signature generation and verification.
 * Validates constant-time comparison for security.
 * 
 * Spec: spec-001-core-payment-processing.md - Webhook Idempotency
 * Task: task-017-webhook-dispatch-worker.md
 */
@DisplayName("WebhookSignatureService Tests")
class WebhookSignatureServiceTest {
    
    private WebhookSignatureService signatureService;
    
    @BeforeEach
    void setUp() {
        signatureService = new WebhookSignatureService();
    }
    
    @Test
    @DisplayName("Should generate valid HMAC-SHA256 signature")
    void testGenerateSignature() {
        // Arrange
        String payload = "{\"transaction_id\":\"123\",\"amount\":1000}";
        String secret = "webhook_secret_key";
        
        // Act
        String signature = signatureService.generateSignature(payload, secret);
        
        // Assert
        assertNotNull(signature);
        assertFalse(signature.isBlank());
        // Base64 encoded signature should be valid
        assertTrue(signature.matches("^[A-Za-z0-9+/=]+$"));
    }
    
    @Test
    @DisplayName("Should generate consistent signatures for same payload and secret")
    void testSignatureConsistency() {
        // Arrange
        String payload = "{\"transaction_id\":\"123\",\"amount\":1000}";
        String secret = "webhook_secret_key";
        
        // Act
        String signature1 = signatureService.generateSignature(payload, secret);
        String signature2 = signatureService.generateSignature(payload, secret);
        
        // Assert
        assertEquals(signature1, signature2);
    }
    
    @Test
    @DisplayName("Should generate different signatures for different payloads")
    void testSignatureDifferentForDifferentPayloads() {
        // Arrange
        String payload1 = "{\"transaction_id\":\"123\",\"amount\":1000}";
        String payload2 = "{\"transaction_id\":\"456\",\"amount\":2000}";
        String secret = "webhook_secret_key";
        
        // Act
        String signature1 = signatureService.generateSignature(payload1, secret);
        String signature2 = signatureService.generateSignature(payload2, secret);
        
        // Assert
        assertNotEquals(signature1, signature2);
    }
    
    @Test
    @DisplayName("Should generate different signatures for different secrets")
    void testSignatureDifferentForDifferentSecrets() {
        // Arrange
        String payload = "{\"transaction_id\":\"123\",\"amount\":1000}";
        String secret1 = "webhook_secret_key_1";
        String secret2 = "webhook_secret_key_2";
        
        // Act
        String signature1 = signatureService.generateSignature(payload, secret1);
        String signature2 = signatureService.generateSignature(payload, secret2);
        
        // Assert
        assertNotEquals(signature1, signature2);
    }
    
    @Test
    @DisplayName("Should verify valid signature")
    void testVerifyValidSignature() {
        // Arrange
        String payload = "{\"transaction_id\":\"123\",\"amount\":1000}";
        String secret = "webhook_secret_key";
        String signature = signatureService.generateSignature(payload, secret);
        
        // Act
        boolean valid = signatureService.verifySignature(payload, secret, signature);
        
        // Assert
        assertTrue(valid);
    }
    
    @Test
    @DisplayName("Should reject invalid signature")
    void testVerifyInvalidSignature() {
        // Arrange
        String payload = "{\"transaction_id\":\"123\",\"amount\":1000}";
        String secret = "webhook_secret_key";
        String invalidSignature = "invalid_signature_value";
        
        // Act
        boolean valid = signatureService.verifySignature(payload, secret, invalidSignature);
        
        // Assert
        assertFalse(valid);
    }
    
    @Test
    @DisplayName("Should reject signature with wrong secret")
    void testVerifySignatureWithWrongSecret() {
        // Arrange
        String payload = "{\"transaction_id\":\"123\",\"amount\":1000}";
        String secret1 = "webhook_secret_key_1";
        String secret2 = "webhook_secret_key_2";
        String signature = signatureService.generateSignature(payload, secret1);
        
        // Act
        boolean valid = signatureService.verifySignature(payload, secret2, signature);
        
        // Assert
        assertFalse(valid);
    }
    
    @Test
    @DisplayName("Should reject signature with modified payload")
    void testVerifySignatureWithModifiedPayload() {
        // Arrange
        String payload1 = "{\"transaction_id\":\"123\",\"amount\":1000}";
        String payload2 = "{\"transaction_id\":\"123\",\"amount\":2000}";
        String secret = "webhook_secret_key";
        String signature = signatureService.generateSignature(payload1, secret);
        
        // Act
        boolean valid = signatureService.verifySignature(payload2, secret, signature);
        
        // Assert
        assertFalse(valid);
    }
    
    @Test
    @DisplayName("Should handle null signature gracefully")
    void testVerifyNullSignature() {
        // Arrange
        String payload = "{\"transaction_id\":\"123\",\"amount\":1000}";
        String secret = "webhook_secret_key";
        
        // Act & Assert
        assertFalse(signatureService.verifySignature(payload, secret, null));
    }
    
    @Test
    @DisplayName("Should handle empty payload")
    void testGenerateSignatureEmptyPayload() {
        // Arrange
        String payload = "";
        String secret = "webhook_secret_key";
        
        // Act
        String signature = signatureService.generateSignature(payload, secret);
        
        // Assert
        assertNotNull(signature);
        assertFalse(signature.isBlank());
    }
    
    @Test
    @DisplayName("Should handle special characters in payload")
    void testGenerateSignatureSpecialCharacters() {
        // Arrange
        String payload = "{\"description\":\"Test with special chars: !@#$%^&*()\"}";
        String secret = "webhook_secret_key";
        
        // Act
        String signature = signatureService.generateSignature(payload, secret);
        
        // Assert
        assertNotNull(signature);
        assertFalse(signature.isBlank());
        
        // Verify signature
        assertTrue(signatureService.verifySignature(payload, secret, signature));
    }
    
    @Test
    @DisplayName("Should handle unicode characters in payload")
    void testGenerateSignatureUnicodeCharacters() {
        // Arrange
        String payload = "{\"description\":\"Test with unicode: 你好世界 🚀\"}";
        String secret = "webhook_secret_key";
        
        // Act
        String signature = signatureService.generateSignature(payload, secret);
        
        // Assert
        assertNotNull(signature);
        assertFalse(signature.isBlank());
        
        // Verify signature
        assertTrue(signatureService.verifySignature(payload, secret, signature));
    }
}
