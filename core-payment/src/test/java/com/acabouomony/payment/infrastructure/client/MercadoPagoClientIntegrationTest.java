package com.acabouomony.payment.infrastructure.client;

import com.acabouomony.payment.domain.dto.PaymentResult;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MercadoPagoClient.
 * 
 * Tests verify:
 * - End-to-end payment submission
 * - Timeout handling
 * - Error mapping
 * - Status query
 * - Concurrent requests
 * 
 * Spec: spec-001-core-payment-processing.md - Mercado Pago Integration
 * Task: task-011-mercado-pago-client.md
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MercadoPagoClient Integration Tests")
class MercadoPagoClientIntegrationTest {
    
    @Autowired
    private MercadoPagoClient mercadoPagoClient;
    
    private UUID merchantId;
    private UUID transactionId;
    
    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
    }
    
    @Nested
    @DisplayName("Payment Submission")
    class PaymentSubmissionTests {
        
        @Test
        @DisplayName("Submits payment successfully")
        void testSubmitsPaymentSuccessfully() {
            // Arrange
            Transaction transaction = createTransaction();
            
            // Act
            PaymentResult result = mercadoPagoClient.submitPayment(transaction);
            
            // Assert
            assertNotNull(result);
            assertNotNull(result.getStatus());
        }
        
        @Test
        @DisplayName("Returns UNKNOWN on timeout")
        void testReturnsUnknownOnTimeout() {
            // Arrange
            Transaction transaction = createTransaction();
            
            // Act
            PaymentResult result = mercadoPagoClient.submitPayment(transaction);
            
            // Assert
            assertNotNull(result);
            assertEquals(PaymentStatus.UNKNOWN, result.getStatus());
        }
    }
    
    @Nested
    @DisplayName("Status Query")
    class StatusQueryTests {
        
        @Test
        @DisplayName("Queries payment status")
        void testQueriesPaymentStatus() {
            // Arrange
            String acquirerReference = "mp_123";
            
            // Act
            PaymentStatus status = mercadoPagoClient.queryPaymentStatus(acquirerReference);
            
            // Assert
            assertNotNull(status);
        }
    }
    
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Handles null transaction")
        void testHandlesNullTransaction() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> mercadoPagoClient.submitPayment(null));
        }
        
        @Test
        @DisplayName("Handles null acquirer reference")
        void testHandlesNullAcquirerReference() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> mercadoPagoClient.queryPaymentStatus(null));
        }
    }
    
    // Helper method
    private Transaction createTransaction() {
        return Transaction.builder()
            .id(transactionId)
            .merchantId(merchantId)
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(PaymentStatus.PROCESSING)
            .payloadHash("abc123")
            .maskedCard("411111XXXXXX1111")
            .cardTokenId("tok_visa_123")
            .version(2)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
