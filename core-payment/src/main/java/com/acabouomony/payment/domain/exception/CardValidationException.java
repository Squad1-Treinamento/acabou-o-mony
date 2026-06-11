package com.acabouomony.payment.domain.exception;

/**
 * Exception thrown when card validation fails.
 * 
 * Indicates that the card data is invalid and cannot be processed.
 * 
 * HTTP Status: 400 Bad Request
 * 
 * Spec: spec-001-core-payment-processing.md - Card Validation Rules
 */
public class CardValidationException extends RuntimeException {
    
    /**
     * Constructs a new CardValidationException with the specified message.
     * 
     * @param message The validation error message
     */
    public CardValidationException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new CardValidationException with the specified message and cause.
     * 
     * @param message The validation error message
     * @param cause The underlying cause
     */
    public CardValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
