package com.acabouomony.payment.domain.exception;

/**
 * Exception thrown when payment acquirer (Mercado Pago) returns an error.
 * 
 * Indicates that the acquirer returned an error response (5xx, network error, etc.).
 * This is a retryable error that triggers PROCESSING -> UNKNOWN transition.
 * 
 * Spec: spec-001-core-payment-processing.md - Timeout & Uncertain Payment Rules
 * Task: task-013-unknown-state-handling.md
 * 
 * Behavior:
 * - Transaction transitions to UNKNOWN (not FAILED)
 * - Reconciliation processing is scheduled
 * - Idempotency protection remains active
 * - Client receives 202 Accepted (processing uncertain)
 * 
 * Examples:
 * - Mercado Pago returns HTTP 500 (server error)
 * - Network connection fails during payment submission
 * - Mercado Pago returns HTTP 503 (service unavailable)
 */
public class PaymentAcquirerException extends RuntimeException {
    
    /**
     * Constructs a new PaymentAcquirerException with the specified message.
     * 
     * @param message The acquirer error message
     */
    public PaymentAcquirerException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new PaymentAcquirerException with the specified message and cause.
     * 
     * @param message The acquirer error message
     * @param cause The underlying cause (e.g., IOException, HttpServerErrorException)
     */
    public PaymentAcquirerException(String message, Throwable cause) {
        super(message, cause);
    }
}
