package com.acabouomony.payment.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when a duplicate payment request is detected.
 * 
 * Occurs when:
 * - Same merchant + idempotency_key combination already exists
 * - Database UNIQUE constraint violation caught
 * - Fallback recovery triggered
 * 
 * Spec: spec-001-core-payment-processing.md - Duplicate Request Rules
 * Task: task-009-db-idempotency-enforcement.md
 */
public class DuplicatePaymentException extends RuntimeException {
    
    private final UUID transactionId;
    private final UUID merchantId;
    private final UUID idempotencyKey;
    
    /**
     * Constructs DuplicatePaymentException with transaction details.
     * 
     * @param transactionId The existing transaction ID
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     */
    public DuplicatePaymentException(UUID transactionId, UUID merchantId, UUID idempotencyKey) {
        super(String.format(
            "Duplicate payment detected: merchant=%s, idempotency_key=%s, transaction_id=%s",
            merchantId, idempotencyKey, transactionId
        ));
        this.transactionId = transactionId;
        this.merchantId = merchantId;
        this.idempotencyKey = idempotencyKey;
    }
    
    /**
     * Constructs DuplicatePaymentException with custom message.
     * 
     * @param message Custom error message
     * @param transactionId The existing transaction ID
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     */
    public DuplicatePaymentException(String message, UUID transactionId, UUID merchantId, UUID idempotencyKey) {
        super(message);
        this.transactionId = transactionId;
        this.merchantId = merchantId;
        this.idempotencyKey = idempotencyKey;
    }
    
    /**
     * Constructs DuplicatePaymentException with cause.
     * 
     * @param message Error message
     * @param cause Root cause exception
     * @param transactionId The existing transaction ID
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     */
    public DuplicatePaymentException(String message, Throwable cause, UUID transactionId, UUID merchantId, UUID idempotencyKey) {
        super(message, cause);
        this.transactionId = transactionId;
        this.merchantId = merchantId;
        this.idempotencyKey = idempotencyKey;
    }
    
    // Getters
    public UUID getTransactionId() {
        return transactionId;
    }
    
    public UUID getMerchantId() {
        return merchantId;
    }
    
    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }
}
