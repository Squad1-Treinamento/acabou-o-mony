package com.acabouomony.payment.domain.exception;

/**
 * Exception thrown when payment acquirer (Mercado Pago) times out.
 * 
 * Indicates that the acquirer did not respond within the configured timeout window.
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
 */
public class PaymentTimeoutException extends RuntimeException {
    
    /**
     * Constructs a new PaymentTimeoutException with the specified message.
     * 
     * @param message The timeout error message
     */
    public PaymentTimeoutException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new PaymentTimeoutException with the specified message and cause.
     * 
     * @param message The timeout error message
     * @param cause The underlying cause (e.g., SocketTimeoutException, ConnectException)
     */
    public PaymentTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
