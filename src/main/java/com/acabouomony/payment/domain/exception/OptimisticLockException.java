package com.acabouomony.payment.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when an optimistic lock conflict is detected.
 * 
 * Indicates that a concurrent update modified the transaction version
 * before the current operation could complete. The exception includes
 * the transaction ID, attempt number, and original cause for debugging.
 */
public class OptimisticLockException extends RuntimeException {
    
    private final UUID transactionId;
    private final int attempt;
    private final Throwable originalCause;
    
    public OptimisticLockException(UUID transactionId, int attempt, Throwable originalCause) {
        super(String.format("Optimistic lock conflict on transaction %s after %d attempts", transactionId, attempt));
        this.transactionId = transactionId;
        this.attempt = attempt;
        this.originalCause = originalCause;
        
        if (originalCause != null) {
            this.initCause(originalCause);
        }
    }
    
    public UUID getTransactionId() {
        return transactionId;
    }
    
    public int getAttempt() {
        return attempt;
    }
    
    public Throwable getOriginalCause() {
        return originalCause;
    }
}
