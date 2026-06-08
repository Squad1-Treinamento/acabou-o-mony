package com.acabouomony.payment.infrastructure.client;

import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.client.exception.MercadoPagoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MercadoPagoClient Exception Handling Tests")
class MercadoPagoClientExceptionTest {
    
    private MercadoPagoClient mercadoPagoClient;
    
    @Mock
    private RestTemplate restTemplate;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mercadoPagoClient = new MercadoPagoClient(restTemplate);
    }
    
    @Test
    @DisplayName("Should throw MercadoPagoException when not implemented")
    void testNotImplementedThrowsMercadoPagoException() {
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(10000L)
            .currency("BRL")
            .status(PaymentStatus.PROCESSING)
            .payloadHash("hash123")
            .cardTokenId("tok_123")
            .version(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        // Since the actual HTTP implementation is not done (TODO),
        // it should throw MercadoPagoException with NOT_IMPLEMENTED
        assertThrows(MercadoPagoException.class, () -> {
            mercadoPagoClient.submitPayment(transaction);
        });
    }
    
    @Test
    @DisplayName("Should not accept null transaction")
    void testNullTransactionThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            mercadoPagoClient.submitPayment(null);
        });
    }
    
    @Test
    @DisplayName("Should not accept null acquirer reference for status query")
    void testNullAcquirerReferenceThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            mercadoPagoClient.queryPaymentStatus(null);
        });
    }
    
    @Test
    @DisplayName("Should not accept empty acquirer reference for status query")
    void testEmptyAcquirerReferenceThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            mercadoPagoClient.queryPaymentStatus("");
        });
    }
    
    @Test
    @DisplayName("Should return UNKNOWN for status query when not implemented")
    void testStatusQueryReturnsUnknownWhenNotImplemented() {
        PaymentStatus status = mercadoPagoClient.queryPaymentStatus("MP-12345");
        
        assertEquals(PaymentStatus.UNKNOWN, status);
    }
}
