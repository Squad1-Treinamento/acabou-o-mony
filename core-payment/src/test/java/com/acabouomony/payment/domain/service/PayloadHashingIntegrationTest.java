package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.PaymentValidationException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for payload hashing with database persistence.
 * 
 * Tests verify:
 * - Payload hash persisted correctly in database
 * - Duplicate detection works with actual database queries
 * - Payload hash validation works with persisted data
 * - Idempotency enforcement at database level
 * 
 * Spec: spec-001-core-payment-processing.md - Idempotency Rules
 * Task: task-007-payload-hashing.md
 */
@DataJpaTest
@Import({PayloadHashingService.class, IdempotencyService.class})
@ActiveProfiles("test")
@DisplayName("Payload Hashing Integration Tests")
class PayloadHashingIntegrationTest {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private PayloadHashingService payloadHashingService;
    
    @Autowired
    private IdempotencyService idempotencyService;
    
    private UUID merchantId;
    private UUID idempotencyKey;
    
    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();
    }
    
    @Test
    @DisplayName("Payload hash persisted correctly in database")
    void testPayloadHashPersistedCorrectly() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("BRL")
            .idempotencyKey(idempotencyKey)
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_visa_123")
                .maskedCard("411111XXXXXX1111")
                .build())
            .customerId(UUID.randomUUID())
            .build();
        
        String expectedHash = payloadHashingService.computePayloadHash(request);
        
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(merchantId)
            .idempotencyKey(idempotencyKey)
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(PaymentStatus.CREATED)
            .payloadHash(expectedHash)
            .version(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        // Act
        Transaction saved = transactionRepository.save(transaction);
        Transaction retrieved = transactionRepository.findById(saved.getId()).orElseThrow();
        
        // Assert
        assertEquals(expectedHash, retrieved.getPayloadHash(), 
            "Payload hash should be persisted correctly");
    }
    
    @Test
    @DisplayName("Duplicate detection finds existing transaction by merchant and idempotency key")
    void testDuplicateDetectionFindsExistingTransaction() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("BRL")
            .idempotencyKey(idempotencyKey)
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_visa_123")
                .maskedCard("411111XXXXXX1111")
                .build())
            .customerId(UUID.randomUUID())
            .build();
        
        String payloadHash = payloadHashingService.computePayloadHash(request);
        
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(merchantId)
            .idempotencyKey(idempotencyKey)
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(PaymentStatus.COMPLETED)
            .payloadHash(payloadHash)
            .version(1)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        transactionRepository.save(transaction);
        
        // Act
        Optional<Transaction> found = idempotencyService.checkDuplicate(merchantId, idempotencyKey, request);
        
        // Assert
        assertTrue(found.isPresent(), "Should find existing transaction");
        assertEquals(transaction.getId(), found.get().getId());
    }
    
    @Test
    @DisplayName("Duplicate detection rejects different payload with same idempotency key")
    void testDuplicateDetectionRejectsDifferentPayload() {
        // Arrange
        PaymentRequest request1 = PaymentRequest.builder()
            .amount(10000L)
            .currency("BRL")
            .idempotencyKey(idempotencyKey)
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_visa_123")
                .maskedCard("411111XXXXXX1111")
                .build())
            .customerId(UUID.randomUUID())
            .build();
        
        String hash1 = payloadHashingService.computePayloadHash(request1);
        
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(merchantId)
            .idempotencyKey(idempotencyKey)
            .amount(request1.getAmount())
            .currency(request1.getCurrency())
            .status(PaymentStatus.COMPLETED)
            .payloadHash(hash1)
            .version(1)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        transactionRepository.save(transaction);
        
        // Different payload (different amount)
        PaymentRequest request2 = PaymentRequest.builder()
            .amount(20000L)  // Different amount
            .currency("BRL")
            .idempotencyKey(idempotencyKey)
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_visa_123")
                .maskedCard("411111XXXXXX1111")
                .build())
            .customerId(UUID.randomUUID())
            .build();
        
        // Act & Assert
        PaymentValidationException exception = assertThrows(
            PaymentValidationException.class,
            () -> idempotencyService.checkDuplicate(merchantId, idempotencyKey, request2)
        );
        
        assertTrue(exception.getMessage().contains("Idempotency key mismatch"),
            "Should reject different payload with same idempotency key");
    }
    
    @Test
    @DisplayName("Different merchants can use same idempotency key independently")
    void testDifferentMerchantsCanUseSameIdempotencyKey() {
        // Arrange
        UUID merchant1 = UUID.randomUUID();
        UUID merchant2 = UUID.randomUUID();
        UUID sharedIdempotencyKey = UUID.randomUUID();
        
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("BRL")
            .idempotencyKey(sharedIdempotencyKey)
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_visa_123")
                .maskedCard("411111XXXXXX1111")
                .build())
            .customerId(UUID.randomUUID())
            .build();
        
        String payloadHash = payloadHashingService.computePayloadHash(request);
        
        // Create transaction for merchant 1
        Transaction tx1 = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(merchant1)
            .idempotencyKey(sharedIdempotencyKey)
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(PaymentStatus.COMPLETED)
            .payloadHash(payloadHash)
            .version(1)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        transactionRepository.save(tx1);
        
        // Act: Create transaction for merchant 2 with same idempotency key
        Transaction tx2 = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(merchant2)
            .idempotencyKey(sharedIdempotencyKey)
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(PaymentStatus.COMPLETED)
            .payloadHash(payloadHash)
            .version(1)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        Transaction saved = transactionRepository.save(tx2);
        
        // Assert: Both transactions should exist independently
        assertTrue(transactionRepository.findById(tx1.getId()).isPresent(),
            "Merchant 1 transaction should exist");
        assertTrue(transactionRepository.findById(tx2.getId()).isPresent(),
            "Merchant 2 transaction should exist");
        
        // Verify they are different transactions
        assertNotEquals(tx1.getId(), tx2.getId(),
            "Different merchants should have different transaction IDs");
    }
    
    @Test
    @DisplayName("Payload hash validation works with persisted data")
    void testPayloadHashValidationWithPersistedData() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("BRL")
            .idempotencyKey(idempotencyKey)
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_visa_123")
                .maskedCard("411111XXXXXX1111")
                .build())
            .customerId(UUID.randomUUID())
            .build();
        
        String payloadHash = payloadHashingService.computePayloadHash(request);
        
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(merchantId)
            .idempotencyKey(idempotencyKey)
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(PaymentStatus.COMPLETED)
            .payloadHash(payloadHash)
            .version(1)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        transactionRepository.save(transaction);
        
        // Act
        boolean isValid = payloadHashingService.validatePayloadHash(request, payloadHash);
        
        // Assert
        assertTrue(isValid, "Payload hash validation should succeed for matching payload");
    }
    
    @Test
    @DisplayName("Idempotency key isolation prevents cross-merchant interference")
    void testIdempotencyKeyIsolationPreventsCrossMerchantInterference() {
        // Arrange
        UUID merchant1 = UUID.randomUUID();
        UUID merchant2 = UUID.randomUUID();
        UUID idempotencyKey1 = UUID.randomUUID();
        
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("BRL")
            .idempotencyKey(idempotencyKey1)
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_visa_123")
                .maskedCard("411111XXXXXX1111")
                .build())
            .customerId(UUID.randomUUID())
            .build();
        
        String payloadHash = payloadHashingService.computePayloadHash(request);
        
        Transaction tx1 = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(merchant1)
            .idempotencyKey(idempotencyKey1)
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(PaymentStatus.COMPLETED)
            .payloadHash(payloadHash)
            .version(1)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        transactionRepository.save(tx1);
        
        // Act: Check duplicate for merchant 2 with same idempotency key
        Optional<Transaction> found = transactionRepository.findByMerchantIdAndIdempotencyKey(
            merchant2, idempotencyKey1
        );
        
        // Assert: Should not find transaction from merchant 1
        assertTrue(found.isEmpty(), 
            "Merchant 2 should not see merchant 1's transaction with same idempotency key");
    }
}
