package com.acabouomony.payment.domain.model;

import java.util.Objects;

/**
 * Value object representing a state transition.
 * 
 * Encapsulates the source and destination states of a payment transition.
 * Immutable and used primarily for validation purposes.
 */
public class StateTransition {
    
    private final PaymentStatus from;
    private final PaymentStatus to;
    
    public StateTransition(PaymentStatus from, PaymentStatus to) {
        this.from = Objects.requireNonNull(from, "from state cannot be null");
        this.to = Objects.requireNonNull(to, "to state cannot be null");
    }
    
    public PaymentStatus getFrom() {
        return from;
    }
    
    public PaymentStatus getTo() {
        return to;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateTransition that = (StateTransition) o;
        return from == that.from && to == that.to;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
    
    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
