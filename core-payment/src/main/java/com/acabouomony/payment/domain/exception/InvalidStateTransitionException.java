package com.acabouomony.payment.domain.exception;

import com.acabouomony.payment.domain.model.PaymentStatus;

/**
 * Exception thrown when an invalid state transition is attempted.
 * 
 * Invalid transitions are rejected with a clear message including:
 * - Source state (from)
 * - Destination state (to)
 * - Reason for rejection
 */
public class InvalidStateTransitionException extends RuntimeException {
    
    private final PaymentStatus from;
    private final PaymentStatus to;
    private final String reason;
    
    public InvalidStateTransitionException(PaymentStatus from, PaymentStatus to, String reason) {
        super(String.format("Invalid transition: %s -> %s. Reason: %s", from, to, reason));
        this.from = from;
        this.to = to;
        this.reason = reason;
    }
    
    public PaymentStatus getFrom() {
        return from;
    }
    
    public PaymentStatus getTo() {
        return to;
    }
    
    public String getReason() {
        return reason;
    }
}
