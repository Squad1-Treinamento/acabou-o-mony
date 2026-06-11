package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PayloadHashingService.
 * 
 * Tests verify:
 * - Deterministic hashing (identical payloads produce identical hashes)
 * - Hash format (SHA-256 hex string, 64 characters)
 * - Payload hash validation (detect different payloads)
 * - Field inclusion/exclusion in canonical payload
 * 
 * Spec: spec-001-core-payment-processing.md - Idempotency Rules
 * Task: task-007-payload-hashing.md
 */
@DisplayName("PayloadHashingService")
class PayloadHashingServiceTest {
    
    private PayloadHashingService service;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new PayloadHashingService(objectMapper);
    }
    
    @Nested
    @DisplayName("Deterministic Hashing")
    class DeterministicHashingTests {
        
        @Test
        @DisplayName("Identical payloads produce identical hashes")
        void testIdenticalPayloadsProduceIdenticalHashes() {
            // Arrange
            UUID merchantId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            PaymentRequest request1 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            PaymentRequest request2 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())  // Different idempotency key
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            // Act
            String hash1 = service.computePayloadHash(request1);
            String hash2 = service.computePayloadHash(request2);
            
            // Assert
            assertEquals(hash1, hash2, "Identical payloads should produce identical hashes");
        }
        
        @Test
        @DisplayName("Different amounts produce different hashes")
        void testDifferentAmountsProduceDifferentHashes() {
            // Arrange
            UUID customerId = UUID.randomUUID();
            PaymentRequest request1 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            PaymentRequest request2 = PaymentRequest.builder()
                .amount(20000L)  // Different amount
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            // Act
            String hash1 = service.computePayloadHash(request1);
            String hash2 = service.computePayloadHash(request2);
            
            // Assert
            assertNotEquals(hash1, hash2, "Different amounts should produce different hashes");
        }
        
        @Test
        @DisplayName("Different currencies produce different hashes")
        void testDifferentCurrenciesProduceDifferentHashes() {
            // Arrange
            UUID customerId = UUID.randomUUID();
            PaymentRequest request1 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            PaymentRequest request2 = PaymentRequest.builder()
                .amount(10000L)
                .currency("USD")  // Different currency
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            // Act
            String hash1 = service.computePayloadHash(request1);
            String hash2 = service.computePayloadHash(request2);
            
            // Assert
            assertNotEquals(hash1, hash2, "Different currencies should produce different hashes");
        }
        
        @Test
        @DisplayName("Different card tokens produce different hashes")
        void testDifferentCardTokensProduceDifferentHashes() {
            // Arrange
            UUID customerId = UUID.randomUUID();
            PaymentRequest request1 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            PaymentRequest request2 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_456")  // Different token
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            // Act
            String hash1 = service.computePayloadHash(request1);
            String hash2 = service.computePayloadHash(request2);
            
            // Assert
            assertNotEquals(hash1, hash2, "Different card tokens should produce different hashes");
        }
        
        @Test
        @DisplayName("Different masked cards produce different hashes")
        void testDifferentMaskedCardsProduceDifferentHashes() {
            // Arrange
            UUID customerId = UUID.randomUUID();
            PaymentRequest request1 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            PaymentRequest request2 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("511111XXXXXX2222")  // Different masked card
                    .build())
                .customerId(customerId)
                .build();
            
            // Act
            String hash1 = service.computePayloadHash(request1);
            String hash2 = service.computePayloadHash(request2);
            
            // Assert
            assertNotEquals(hash1, hash2, "Different masked cards should produce different hashes");
        }
        
        @Test
        @DisplayName("Different customer IDs produce different hashes")
        void testDifferentCustomerIdsProduceDifferentHashes() {
            // Arrange
            PaymentRequest request1 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(UUID.randomUUID())
                .build();
            
            PaymentRequest request2 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(UUID.randomUUID())  // Different customer
                .build();
            
            // Act
            String hash1 = service.computePayloadHash(request1);
            String hash2 = service.computePayloadHash(request2);
            
            // Assert
            assertNotEquals(hash1, hash2, "Different customer IDs should produce different hashes");
        }
    }
    
    @Nested
    @DisplayName("Hash Format")
    class HashFormatTests {
        
        @Test
        @DisplayName("Hash is 64-character hexadecimal string (SHA-256)")
        void testHashFormatIsSha256Hex() {
            // Arrange
            PaymentRequest request = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(UUID.randomUUID())
                .build();
            
            // Act
            String hash = service.computePayloadHash(request);
            
            // Assert
            assertEquals(64, hash.length(), "SHA-256 hash should be 64 characters");
            assertTrue(hash.matches("^[0-9a-f]{64}$"), "Hash should be hexadecimal (lowercase)");
        }
    }
    
    @Nested
    @DisplayName("Payload Hash Validation")
    class PayloadHashValidationTests {
        
        @Test
        @DisplayName("Validates matching payload hashes")
        void testValidatesMatchingPayloadHashes() {
            // Arrange
            UUID customerId = UUID.randomUUID();
            PaymentRequest request = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            String storedHash = service.computePayloadHash(request);
            
            // Act
            boolean isValid = service.validatePayloadHash(request, storedHash);
            
            // Assert
            assertTrue(isValid, "Matching hashes should validate successfully");
        }
        
        @Test
        @DisplayName("Detects mismatched payload hashes")
        void testDetectsMismatchedPayloadHashes() {
            // Arrange
            UUID customerId = UUID.randomUUID();
            PaymentRequest request1 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            PaymentRequest request2 = PaymentRequest.builder()
                .amount(20000L)  // Different amount
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            String storedHash = service.computePayloadHash(request1);
            
            // Act
            boolean isValid = service.validatePayloadHash(request2, storedHash);
            
            // Assert
            assertFalse(isValid, "Mismatched hashes should fail validation");
        }
    }
    
    @Nested
    @DisplayName("Field Inclusion/Exclusion")
    class FieldInclusionExclusionTests {
        
        @Test
        @DisplayName("Idempotency key is excluded from hash (different keys, same hash)")
        void testIdempotencyKeyExcludedFromHash() {
            // Arrange
            UUID customerId = UUID.randomUUID();
            PaymentRequest request1 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            PaymentRequest request2 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())  // Different idempotency key
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .build();
            
            // Act
            String hash1 = service.computePayloadHash(request1);
            String hash2 = service.computePayloadHash(request2);
            
            // Assert
            assertEquals(hash1, hash2, "Idempotency key should not affect hash");
        }
        
        @Test
        @DisplayName("Customer email is excluded from hash (different emails, same hash)")
        void testCustomerEmailExcludedFromHash() {
            // Arrange
            UUID customerId = UUID.randomUUID();
            PaymentRequest request1 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .customerEmail("customer1@example.com")
                .build();
            
            PaymentRequest request2 = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(customerId)
                .customerEmail("customer2@example.com")  // Different email
                .build();
            
            // Act
            String hash1 = service.computePayloadHash(request1);
            String hash2 = service.computePayloadHash(request2);
            
            // Assert
            assertEquals(hash1, hash2, "Customer email should not affect hash");
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Handles null customer ID")
        void testHandlesNullCustomerId() {
            // Arrange
            PaymentRequest request = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard("411111XXXXXX1111")
                    .build())
                .customerId(null)  // No customer ID
                .build();
            
            // Act
            String hash = service.computePayloadHash(request);
            
            // Assert
            assertNotNull(hash, "Should compute hash even without customer ID");
            assertEquals(64, hash.length(), "Hash should be valid SHA-256");
        }
        
        @Test
        @DisplayName("Handles null masked card")
        void testHandlesNullMaskedCard() {
            // Arrange
            PaymentRequest request = PaymentRequest.builder()
                .amount(10000L)
                .currency("BRL")
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_visa_123")
                    .maskedCard(null)  // No masked card
                    .build())
                .customerId(UUID.randomUUID())
                .build();
            
            // Act
            String hash = service.computePayloadHash(request);
            
            // Assert
            assertNotNull(hash, "Should compute hash even without masked card");
            assertEquals(64, hash.length(), "Hash should be valid SHA-256");
        }
    }
}
