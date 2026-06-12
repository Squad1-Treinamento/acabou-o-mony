package com.acabouomony.payment.domain.exception;

/**
 * Exception thrown when payment request validation fails.
 * 
 * Indicates that the request payload is invalid and cannot be processed.
 * 
 * HTTP Status: 400 Bad Request
 * 
 * Spec: spec-001-core-payment-processing.md - Failure Handling Rules
 */
public class PaymentValidationException extends RuntimeException {
    
    /**
     * Constructs a new PaymentValidationException with the specified message.
     * 
     * @param message The validation error message
     */
    public PaymentValidationException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new PaymentValidationException with the specified message and cause.
     * 
     * @param message The validation error message
     * @param cause The underlying cause
     */
    public PaymentValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
