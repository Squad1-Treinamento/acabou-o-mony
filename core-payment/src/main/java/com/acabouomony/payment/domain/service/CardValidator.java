package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.exception.CardValidationException;
import com.acabouomony.payment.domain.model.CardType;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for comprehensive card validation.
 * 
 * Validates:
 * - Card number (Luhn check, length, card type)
 * - Expiry date (not expired, valid format)
 * - CVV (length, digits only)
 * 
 * Spec: spec-001-core-payment-processing.md - Card Validation Rules
 */
@Service
public class CardValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(CardValidator.class);
    
    private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("MM/yy");
    private static final DateTimeFormatter EXPIRY_FORMATTER_FULL = DateTimeFormatter.ofPattern("MM/yyyy");
    
    /**
     * Validates a complete card.
     * 
     * Throws CardValidationException if any validation fails.
     * 
     * @param cardNumber The card number (may contain spaces/dashes)
     * @param expiryDate The expiry date (MM/yy or MM/yyyy format)
     * @param cvv The CVV code
     * @throws CardValidationException if validation fails
     */
    public void validate(String cardNumber, String expiryDate, String cvv) {
        List<String> errors = new ArrayList<>();
        
        // Validate card number
        validateCardNumber(cardNumber, errors);
        
        // Validate expiry date
        validateExpiryDate(expiryDate, errors);
        
        // Validate CVV
        validateCvv(cardNumber, cvv, errors);
        
        // If any errors, throw exception with all details
        if (!errors.isEmpty()) {
            String errorMessage = String.join("; ", errors);
            logger.warn("Card validation failed: {}", errorMessage);
            throw new CardValidationException(errorMessage);
        }
        
        logger.debug("Card validation passed");
    }
    
    /**
     * Validates card number.
     * 
     * Rules:
     * - Must not be null or empty
     * - Must contain only digits (after removing spaces/dashes)
     * - Must pass Luhn check
     * - Must be recognized card type
     * - Must have valid length for card type
     * 
     * @param cardNumber The card number
     * @param errors List to accumulate validation errors
     */
    private void validateCardNumber(String cardNumber, List<String> errors) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            errors.add("card_number cannot be null or empty");
            return;
        }
        
        // Remove spaces and dashes
        String digits = cardNumber.replaceAll("[\\s-]", "");
        
        // Check if contains only digits
        if (!digits.matches("^[0-9]+$")) {
            errors.add("card_number must contain only digits");
            return;
        }
        
        // Detect card type
        CardType cardType = CardType.detect(digits);
        if (cardType == null) {
            errors.add("card_number is not a recognized card type");
            return;
        }
        
        // Validate length for card type
        if (!cardType.isValidLength(digits)) {
            errors.add("card_number has invalid length for " + cardType.name());
            return;
        }
        
        // Luhn check
        if (!passesLuhnCheck(digits)) {
            errors.add("card_number failed Luhn check (invalid card number)");
        }
    }
    
    /**
     * Validates expiry date.
     * 
     * Rules:
     * - Must not be null or empty
     * - Must be valid format (MM/yy or MM/yyyy)
     * - Must not be expired (current month or future)
     * 
     * @param expiryDate The expiry date
     * @param errors List to accumulate validation errors
     */
    private void validateExpiryDate(String expiryDate, List<String> errors) {
        if (expiryDate == null || expiryDate.isEmpty()) {
            errors.add("expiry_date cannot be null or empty");
            return;
        }
        
        YearMonth expiry;
        try {
            // Try MM/yy format first
            if (expiryDate.matches("^\\d{2}/\\d{2}$")) {
                expiry = YearMonth.parse(expiryDate, EXPIRY_FORMATTER);
            }
            // Try MM/yyyy format
            else if (expiryDate.matches("^\\d{2}/\\d{4}$")) {
                expiry = YearMonth.parse(expiryDate, EXPIRY_FORMATTER_FULL);
            }
            else {
                errors.add("expiry_date must be in MM/yy or MM/yyyy format");
                return;
            }
        } catch (DateTimeParseException e) {
            errors.add("expiry_date is not a valid date");
            return;
        }
        
        // Check if expired
        YearMonth now = YearMonth.now();
        if (expiry.isBefore(now)) {
            errors.add("expiry_date is expired");
        }
    }
    
    /**
     * Validates CVV.
     * 
     * Rules:
     * - Must not be null or empty
     * - Must contain only digits
     * - Must have correct length for card type (3 or 4 digits)
     * 
     * @param cardNumber The card number (to detect card type)
     * @param cvv The CVV code
     * @param errors List to accumulate validation errors
     */
    private void validateCvv(String cardNumber, String cvv, List<String> errors) {
        if (cvv == null || cvv.isEmpty()) {
            errors.add("cvv cannot be null or empty");
            return;
        }
        
        // Check if contains only digits
        if (!cvv.matches("^[0-9]+$")) {
            errors.add("cvv must contain only digits");
            return;
        }
        
        // Detect card type to validate CVV length
        if (cardNumber != null) {
            String digits = cardNumber.replaceAll("[\\s-]", "");
            CardType cardType = CardType.detect(digits);
            
            if (cardType != null) {
                if (!cardType.isValidCvvLength(cvv)) {
                    errors.add("cvv must be " + cardType.getCvvLength() + " digits for " + cardType.name());
                }
            } else {
                // If card type not detected, accept 3 or 4 digits
                if (cvv.length() < 3 || cvv.length() > 4) {
                    errors.add("cvv must be 3 or 4 digits");
                }
            }
        } else {
            // If card number not provided, accept 3 or 4 digits
            if (cvv.length() < 3 || cvv.length() > 4) {
                errors.add("cvv must be 3 or 4 digits");
            }
        }
    }
    
    /**
     * Performs Luhn check on card number.
     * 
     * The Luhn algorithm:
     * 1. Starting from the rightmost digit (check digit), double every second digit
     * 2. If doubling results in a two-digit number, add the digits together
     * 3. Sum all the digits
     * 4. If the sum is divisible by 10, the number is valid
     * 
     * @param cardNumber The card number (digits only)
     * @return true if passes Luhn check, false otherwise
     */
    private boolean passesLuhnCheck(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return false;
        }
        
        int sum = 0;
        boolean alternate = false;
        
        // Process digits from right to left
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return (sum % 10 == 0);
    }
    
    /**
     * Validates only card number (for standalone validation).
     * 
     * @param cardNumber The card number
     * @throws CardValidationException if validation fails
     */
    public void validateCardNumber(String cardNumber) {
        List<String> errors = new ArrayList<>();
        validateCardNumber(cardNumber, errors);
        
        if (!errors.isEmpty()) {
            String errorMessage = String.join("; ", errors);
            logger.warn("Card number validation failed: {}", errorMessage);
            throw new CardValidationException(errorMessage);
        }
    }
    
    /**
     * Validates only expiry date (for standalone validation).
     * 
     * @param expiryDate The expiry date
     * @throws CardValidationException if validation fails
     */
    public void validateExpiryDate(String expiryDate) {
        List<String> errors = new ArrayList<>();
        validateExpiryDate(expiryDate, errors);
        
        if (!errors.isEmpty()) {
            String errorMessage = String.join("; ", errors);
            logger.warn("Expiry date validation failed: {}", errorMessage);
            throw new CardValidationException(errorMessage);
        }
    }
    
    /**
     * Validates only CVV (for standalone validation).
     * 
     * @param cardNumber The card number (to detect card type)
     * @param cvv The CVV code
     * @throws CardValidationException if validation fails
     */
    public void validateCvv(String cardNumber, String cvv) {
        List<String> errors = new ArrayList<>();
        validateCvv(cardNumber, cvv, errors);
        
        if (!errors.isEmpty()) {
            String errorMessage = String.join("; ", errors);
            logger.warn("CVV validation failed: {}", errorMessage);
            throw new CardValidationException(errorMessage);
        }
    }
    
    /**
     * Detects card type from card number.
     * 
     * @param cardNumber The card number
     * @return The detected CardType, or null if not recognized
     */
    public CardType detectCardType(String cardNumber) {
        if (cardNumber == null) {
            return null;
        }
        
        String digits = cardNumber.replaceAll("[\\s-]", "");
        return CardType.detect(digits);
    }
}
