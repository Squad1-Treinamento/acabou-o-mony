package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.exception.PaymentValidationException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service for comprehensive payment request validation.
 * 
 * Validates:
 * - Amount (positive, non-zero)
 * - Currency (ISO 4217, exactly 3 characters)
 * - Idempotency key (UUID format)
 * - Payment method (card token required)
 * - Customer information (optional)
 * 
 * Spec: spec-001-core-payment-processing.md - Request Validation Rules
 */
@Service
public class PaymentRequestValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentRequestValidator.class);
    
    // ISO 4217 currency codes (subset for this implementation)
    private static final Set<String> VALID_CURRENCIES = Set.of(
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD",
        "MXN", "SGD", "HKD", "NOK", "KRW", "TRY", "RUB", "INR", "BRL", "ZAR"
    );
    
    // Maximum amount: 1 billion cents = $10,000,000
    private static final long MAX_AMOUNT = 100_000_000_00L;
    
    /**
     * Validates a payment request comprehensively.
     * 
     * Throws PaymentValidationException if any validation fails.
     * 
     * @param request The payment request to validate
     * @throws PaymentValidationException if validation fails
     */
    public void validate(PaymentRequest request) {
        List<String> errors = new ArrayList<>();
        
        // Validate request is not null
        if (request == null) {
            throw new PaymentValidationException("Payment request cannot be null");
        }
        
        // Validate amount
        validateAmount(request.getAmount(), errors);
        
        // Validate currency
        validateCurrency(request.getCurrency(), errors);
        
        // Validate idempotency key
        validateIdempotencyKey(request.getIdempotencyKey(), errors);
        
        // Validate payment method
        validatePaymentMethod(request.getPaymentMethod(), errors);
        
        // Validate customer information (optional)
        validateCustomerInformation(request, errors);
        
        // If any errors, throw exception with all details
        if (!errors.isEmpty()) {
            String errorMessage = String.join("; ", errors);
            logger.warn("Payment request validation failed: {}", errorMessage);
            throw new PaymentValidationException(errorMessage);
        }
        
        logger.debug("Payment request validation passed");
    }
    
    /**
     * Validates the amount field.
     * 
     * Rules:
     * - Must not be null
     * - Must be positive (> 0)
     * - Must not exceed maximum (1 billion cents)
     * 
     * @param amount The amount to validate (in cents)
     * @param errors List to accumulate validation errors
     */
    private void validateAmount(Long amount, List<String> errors) {
        if (amount == null) {
            errors.add("amount cannot be null");
            return;
        }
        
        if (amount <= 0) {
            errors.add("amount must be greater than 0 (in cents)");
        }
        
        if (amount > MAX_AMOUNT) {
            errors.add("amount must not exceed " + MAX_AMOUNT + " cents ($10,000,000)");
        }
    }
    
    /**
     * Validates the currency field.
     * 
     * Rules:
     * - Must not be null
     * - Must be exactly 3 characters
     * - Must be uppercase
     * - Must be valid ISO 4217 code
     * 
     * @param currency The currency code to validate
     * @param errors List to accumulate validation errors
     */
    private void validateCurrency(String currency, List<String> errors) {
        if (currency == null) {
            errors.add("currency cannot be null");
            return;
        }
        
        if (currency.length() != 3) {
            errors.add("currency must be exactly 3 characters (ISO 4217)");
            return;
        }
        
        if (!currency.matches("^[A-Z]{3}$")) {
            errors.add("currency must be 3 uppercase letters (ISO 4217)");
            return;
        }
        
        if (!VALID_CURRENCIES.contains(currency)) {
            errors.add("currency '" + currency + "' is not a supported ISO 4217 code");
        }
    }
    
    /**
     * Validates the idempotency key field.
     * 
     * Rules:
     * - Must not be null
     * - Must be valid UUID format
     * 
     * @param idempotencyKey The idempotency key to validate
     * @param errors List to accumulate validation errors
     */
    private void validateIdempotencyKey(Object idempotencyKey, List<String> errors) {
        if (idempotencyKey == null) {
            errors.add("idempotency_key cannot be null");
            return;
        }
        
        // If it's already a UUID object, it's valid
        if (idempotencyKey instanceof UUID) {
            return;
        }
        
        // If it's a string, try to parse as UUID
        if (idempotencyKey instanceof String) {
            try {
                UUID.fromString((String) idempotencyKey);
            } catch (IllegalArgumentException e) {
                errors.add("idempotency_key must be valid UUID format");
            }
        } else {
            errors.add("idempotency_key must be UUID format");
        }
    }
    
    /**
     * Validates the payment method field.
     * 
     * Rules:
     * - Must not be null
     * - Must contain card_token_id
     * - card_token_id must not be null or blank
     * - masked_card (if provided) must match format
     * 
     * @param paymentMethod The payment method to validate
     * @param errors List to accumulate validation errors
     */
    private void validatePaymentMethod(PaymentRequest.PaymentMethod paymentMethod, List<String> errors) {
        if (paymentMethod == null) {
            errors.add("payment_method cannot be null");
            return;
        }
        
        // Validate card_token_id
        if (paymentMethod.getCardTokenId() == null) {
            errors.add("payment_method.card_token_id cannot be null");
            return;
        }
        
        if (paymentMethod.getCardTokenId().isBlank()) {
            errors.add("payment_method.card_token_id cannot be blank");
            return;
        }
        
        if (paymentMethod.getCardTokenId().length() > 100) {
            errors.add("payment_method.card_token_id must not exceed 100 characters");
        }
        
        // Validate masked_card (optional)
        if (paymentMethod.getMaskedCard() != null) {
            if (!paymentMethod.getMaskedCard().matches("^[0-9]{6}X{6,8}[0-9]{4}$")) {
                errors.add("payment_method.masked_card must match format: 6 digits + X's + 4 digits (e.g., 411111XXXXXX1111)");
            }
        }
    }
    
    /**
     * Validates optional customer information.
     * 
     * Rules:
     * - customer_id (if provided) must be valid UUID
     * - customer_email (if provided) must be valid email format
     * 
     * @param request The payment request containing customer info
     * @param errors List to accumulate validation errors
     */
    private void validateCustomerInformation(PaymentRequest request, List<String> errors) {
        // Validate customer_id (optional)
        if (request.getCustomerId() != null) {
            // UUID is already validated by Jackson deserialization
            // No additional validation needed
        }
        
        // Validate customer_email (optional)
        if (request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank()) {
            if (!isValidEmail(request.getCustomerEmail())) {
                errors.add("customer_email must be valid email format");
            }
        }
    }
    
    /**
     * Simple email validation.
     * 
     * Uses basic regex pattern for email validation.
     * For production, consider using more robust email validation library.
     * 
     * @param email The email to validate
     * @return true if email is valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
