package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.exception.CardValidationException;
import com.acabouomony.payment.domain.model.CardType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CardValidator.
 * 
 * Verifies:
 * - Luhn validation for card numbers
 * - Card type detection
 * - Card length validation
 * - Expiry date validation
 * - CVV validation
 */
@SpringBootTest
@ActiveProfiles("test")
class CardValidatorTest {

    @Autowired
    private CardValidator cardValidator;

    
    // ============ Card Number Validation Tests ============

    @Test
    void shouldValidateValidVisaCard() {
        // Arrange
        String cardNumber = "4111111111111111"; // Valid Visa test card
        String expiryDate = "12/25";
        String cvv = "123";

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv));
    }

    @Test
    void shouldValidateValidMastercardCard() {
        // Arrange
        String cardNumber = "5555555555554444"; // Valid Mastercard test card
        String expiryDate = "12/25";
        String cvv = "123";

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv));
    }

    @Test
    void shouldValidateValidAmexCard() {
        // Arrange - Amex uses 4-digit CVV
        String cardNumber = "378282246310005"; // Valid Amex test card
        String expiryDate = "12/25";
        String cvv = "1234";

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv));
    }

    @Test
    void shouldRejectNullCardNumber() {
        // Arrange
        String cardNumber = null;
        String expiryDate = "12/25";
        String cvv = "123";

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("card_number cannot be null");
    }

    @Test
    void shouldRejectEmptyCardNumber() {
        // Arrange
        String cardNumber = "";
        String expiryDate = "12/25";
        String cvv = "123";

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("card_number cannot be null or empty");
    }

    @Test
    void shouldRejectCardNumberWithLetters() {
        // Arrange
        String cardNumber = "4111ABCD11111111";
        String expiryDate = "12/25";
        String cvv = "123";

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("must contain only digits");
    }

    @Test
    void shouldRejectCardNumberFailingLuhnCheck() {
        // Arrange
        String cardNumber = "4111111111111112"; // Invalid Luhn check
        String expiryDate = "12/25";
        String cvv = "123";

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("failed Luhn check");
    }

    @Test
    void shouldRejectUnrecognizedCardType() {
        // Arrange
        String cardNumber = "1234567890123456"; // Not a recognized card type
        String expiryDate = "12/25";
        String cvv = "123";

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("not a recognized card type");
    }

    @Test
    void shouldRejectCardNumberWithInvalidLength() {
        // Arrange
        String cardNumber = "41111111111111"; // Too short for Visa
        String expiryDate = "12/25";
        String cvv = "123";

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("invalid length");
    }

    @Test
    void shouldAcceptCardNumberWithSpaces() {
        // Arrange
        String cardNumber = "4111 1111 1111 1111"; // Valid Visa with spaces
        String expiryDate = "12/25";
        String cvv = "123";

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv));
    }

    @Test
    void shouldAcceptCardNumberWithDashes() {
        // Arrange
        String cardNumber = "4111-1111-1111-1111"; // Valid Visa with dashes
        String expiryDate = "12/25";
        String cvv = "123";

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv));
    }

    // ============ Expiry Date Validation Tests ============

    @Test
    void shouldRejectNullExpiryDate() {
        // Arrange
        String cardNumber = "4111111111111111";
        String expiryDate = null;
        String cvv = "123";

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("expiry_date cannot be null");
    }

    @Test
    void shouldRejectEmptyExpiryDate() {
        // Arrange
        String cardNumber = "4111111111111111";
        String expiryDate = "";
        String cvv = "123";

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("expiry_date cannot be null or empty");
    }

    @Test
    void shouldRejectExpiredCard() {
        // Arrange
        String cardNumber = "4111111111111111";
        String expiryDate = "01/20"; // Expired
        String cvv = "123";

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("expiry_date is expired");
    }

    @Test
    void shouldAcceptCurrentMonthExpiry() {
        // Arrange
        String cardNumber = "4111111111111111";
        YearMonth now = YearMonth.now();
        String expiryDate = now.format(DateTimeFormatter.ofPattern("MM/yy"));
        String cvv = "123";

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv));
    }

    @Test
    void shouldAcceptFutureExpiry() {
        // Arrange
        String cardNumber = "4111111111111111";
        YearMonth future = YearMonth.now().plusYears(2);
        String expiryDate = future.format(DateTimeFormatter.ofPattern("MM/yy"));
        String cvv = "123";

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv));
    }

    @Test
    void shouldAcceptExpiryInFullYearFormat() {
        // Arrange
        String cardNumber = "4111111111111111";
        String expiryDate = "12/2025"; // MM/yyyy format
        String cvv = "123";

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv));
    }

    @Test
    void shouldRejectInvalidExpiryFormat() {
        // Arrange
        String cardNumber = "4111111111111111";
        String expiryDate = "13/25"; // Invalid month
        String cvv = "123";

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("expiry_date");
    }

    @Test
    void shouldRejectExpiryWithWrongFormat() {
        // Arrange
        String cardNumber = "4111111111111111";
        String expiryDate = "2025-12"; // Wrong format
        String cvv = "123";

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("must be in MM/yy or MM/yyyy format");
    }

    // ============ CVV Validation Tests ============

    @Test
    void shouldRejectNullCvv() {
        // Arrange
        String cardNumber = "4111111111111111";
        String expiryDate = "12/25";
        String cvv = null;

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("cvv cannot be null");
    }

    @Test
    void shouldRejectEmptyCvv() {
        // Arrange
        String cardNumber = "4111111111111111";
        String expiryDate = "12/25";
        String cvv = "";

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("cvv cannot be null or empty");
    }

    @Test
    void shouldRejectCvvWithLetters() {
        // Arrange
        String cardNumber = "4111111111111111";
        String expiryDate = "12/25";
        String cvv = "12A";

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("cvv must contain only digits");
    }

    @Test
    void shouldRejectCvvWithWrongLengthForVisa() {
        // Arrange - Visa requires 3-digit CVV
        String cardNumber = "4111111111111111";
        String expiryDate = "12/25";
        String cvv = "1234"; // 4 digits

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("cvv must be 3 digits");
    }

    @Test
    void shouldAccept4DigitCvvForAmex() {
        // Arrange
        String cardNumber = "378282246310005"; // Amex requires 4-digit CVV
        String expiryDate = "12/25";
        String cvv = "1234";

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv));
    }

    @Test
    void shouldReject3DigitCvvForAmex() {
        // Arrange
        String cardNumber = "378282246310005"; // Amex requires 4-digit CVV
        String expiryDate = "12/25";
        String cvv = "123"; // Only 3 digits

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("cvv must be 4 digits");
    }

    // ============ Card Type Detection Tests ============

    @Test
    void shouldDetectVisaCardType() {
        // Arrange
        String cardNumber = "4111111111111111";

        // Act
        CardType cardType = cardValidator.detectCardType(cardNumber);

        // Assert
        assertThat(cardType).isEqualTo(CardType.VISA);
    }

    @Test
    void shouldDetectMastercardCardType() {
        // Arrange
        String cardNumber = "5555555555554444";

        // Act
        CardType cardType = cardValidator.detectCardType(cardNumber);

        // Assert
        assertThat(cardType).isEqualTo(CardType.MASTERCARD);
    }

    @Test
    void shouldDetectAmexCardType() {
        // Arrange
        String cardNumber = "378282246310005";

        // Act
        CardType cardType = cardValidator.detectCardType(cardNumber);

        // Assert
        assertThat(cardType).isEqualTo(CardType.AMEX);
    }

    @Test
    void shouldDetectDiscoverCardType() {
        // Arrange
        String cardNumber = "6011111111111117";

        // Act
        CardType cardType = cardValidator.detectCardType(cardNumber);

        // Assert
        assertThat(cardType).isEqualTo(CardType.DISCOVER);
    }

    @Test
    void shouldReturnNullForUnrecognizedCardType() {
        // Arrange
        String cardNumber = "1234567890123456";

        // Act
        CardType cardType = cardValidator.detectCardType(cardNumber);

        // Assert
        assertThat(cardType).isNull();
    }

    // ============ Standalone Validation Tests ============

    @Test
    void shouldValidateCardNumberStandalone() {
        // Arrange
        String cardNumber = "4111111111111111";

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cardValidator.validateCardNumber(cardNumber));
    }

    @Test
    void shouldValidateExpiryDateStandalone() {
        // Arrange
        String expiryDate = "12/25";

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cardValidator.validateExpiryDate(expiryDate));
    }

    @Test
    void shouldValidateCvvStandalone() {
        // Arrange
        String cardNumber = "4111111111111111";
        String cvv = "123";

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cardValidator.validateCvv(cardNumber, cvv));
    }

    // ============ Multiple Validation Errors Tests ============

    @Test
    void shouldReportMultipleValidationErrors() {
        // Arrange
        String cardNumber = "4111111111111112"; // Invalid Luhn
        String expiryDate = "01/20"; // Expired
        String cvv = "12A"; // Invalid CVV

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("Luhn")
            .hasMessageContaining("expired")
            .hasMessageContaining("cvv");
    }

    // ============ Edge Cases Tests ============

    @Test
    void shouldHandleCardNumberWithMixedSpacesAndDashes() {
        // Arrange
        String cardNumber = "4111 1111-1111 1111";
        String expiryDate = "12/25";
        String cvv = "123";

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv));
    }

    @Test
    void shouldRejectCvvTooShort() {
        // Arrange
        String cardNumber = "4111111111111111";
        String expiryDate = "12/25";
        String cvv = "12"; // Too short

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("cvv must be 3 digits");
    }

    @Test
    void shouldRejectCvvTooLong() {
        // Arrange
        String cardNumber = "4111111111111111";
        String expiryDate = "12/25";
        String cvv = "12345"; // Too long

        // Act & Assert
        assertThatThrownBy(() -> cardValidator.validate(cardNumber, expiryDate, cvv))
            .isInstanceOf(CardValidationException.class)
            .hasMessageContaining("cvv must be 3 digits");
    }
}
