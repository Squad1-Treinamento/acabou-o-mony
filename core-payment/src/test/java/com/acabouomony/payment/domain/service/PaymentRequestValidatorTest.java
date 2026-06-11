package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.exception.PaymentValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PaymentRequestValidator.
 * 
 * Verifies:
 * - Amount validation (positive, non-zero, max limit)
 * - Currency validation (ISO 4217, 3 characters, uppercase)
 * - Idempotency key validation (UUID format)
 * - Payment method validation (card token required)
 * - Customer information validation (optional)
 */
@SpringBootTest
@ActiveProfiles("test")
class PaymentRequestValidatorTest {

    @Autowired
    private PaymentRequestValidator validator;

    @Test
    void shouldValidateValidPaymentRequest() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .maskedCard("411111XXXXXX1111")
                .build())
            .customerId(UUID.randomUUID())
            .customerEmail("customer@example.com")
            .build();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldRejectNullRequest() {
        // Act & Assert
        assertThatThrownBy(() -> validator.validate(null))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("cannot be null");
    }

    // ============ Amount Validation Tests ============

    @Test
    void shouldRejectNullAmount() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(null)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("amount cannot be null");
    }

    @Test
    void shouldRejectZeroAmount() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(0L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("amount must be greater than 0");
    }

    @Test
    void shouldRejectNegativeAmount() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(-1000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("amount must be greater than 0");
    }

    @Test
    void shouldAcceptMinimumAmount() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(1L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .build();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldAcceptMaximumAmount() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(100_000_000_00L) // $10,000,000
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .build();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldRejectAmountExceedingMaximum() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(100_000_000_01L) // $10,000,000.01
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("must not exceed");
    }

    // ============ Currency Validation Tests ============

    @Test
    void shouldRejectNullCurrency() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency(null)
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("currency cannot be null");
    }

    @Test
    void shouldRejectCurrencyWithWrongLength() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("US")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("exactly 3 characters");
    }

    @Test
    void shouldRejectLowercaseCurrency() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("usd")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("uppercase letters");
    }

    @Test
    void shouldRejectInvalidCurrencyCode() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("XXX")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("not a supported");
    }

    @Test
    void shouldAcceptValidCurrencyCodes() {
        // Test multiple valid currencies
        String[] validCurrencies = {"USD", "EUR", "GBP", "JPY", "BRL"};

        for (String currency : validCurrencies) {
            PaymentRequest request = PaymentRequest.builder()
                .amount(10000L)
                .currency(currency)
                .idempotencyKey(UUID.randomUUID())
                .paymentMethod(PaymentRequest.PaymentMethod.builder()
                    .cardTokenId("tok_123456")
                    .build())
                .build();

            assertThatNoException().isThrownBy(() -> validator.validate(request));
        }
    }

    // ============ Idempotency Key Validation Tests ============

    @Test
    void shouldRejectNullIdempotencyKey() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(null)
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("idempotency_key cannot be null");
    }

    @Test
    void shouldAcceptValidUUIDIdempotencyKey() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .build();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    // ============ Payment Method Validation Tests ============

    @Test
    void shouldRejectNullPaymentMethod() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(null)
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("payment_method cannot be null");
    }

    @Test
    void shouldRejectNullCardTokenId() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId(null)
                .build())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("card_token_id cannot be null");
    }

    @Test
    void shouldRejectBlankCardTokenId() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("   ")
                .build())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("card_token_id cannot be blank");
    }

    @Test
    void shouldAcceptValidCardTokenId() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_1234567890")
                .build())
            .build();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldRejectMaskedCardWithInvalidFormat() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .maskedCard("invalid_format")
                .build())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("masked_card must match format");
    }

    @Test
    void shouldAcceptValidMaskedCard() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .maskedCard("411111XXXXXX1111")
                .build())
            .build();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldAllowNullMaskedCard() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .maskedCard(null)
                .build())
            .build();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    // ============ Customer Information Validation Tests ============

    @Test
    void shouldAllowNullCustomerId() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .customerId(null)
            .build();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldRejectInvalidEmail() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .customerEmail("invalid-email")
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("customer_email must be valid");
    }

    @Test
    void shouldAcceptValidEmail() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .customerEmail("customer@example.com")
            .build();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldAllowNullEmail() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .customerEmail(null)
            .build();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldAllowBlankEmail() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .idempotencyKey(UUID.randomUUID())
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId("tok_123456")
                .build())
            .customerEmail("   ")
            .build();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    // ============ Multiple Validation Errors Tests ============

    @Test
    void shouldReportMultipleValidationErrors() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(-1000L)
            .currency("invalid")
            .idempotencyKey(null)
            .paymentMethod(PaymentRequest.PaymentMethod.builder()
                .cardTokenId(null)
                .build())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("amount")
            .hasMessageContaining("currency")
            .hasMessageContaining("idempotency_key")
            .hasMessageContaining("card_token_id");
    }
}
