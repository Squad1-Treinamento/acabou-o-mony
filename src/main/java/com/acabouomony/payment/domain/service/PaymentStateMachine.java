package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.exception.InvalidStateTransitionException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.model.StateTransition;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Payment State Machine Service
 * 
 * Implements deterministic validation of payment state transitions.
 * All transitions are explicitly defined per specification.
 * Invalid transitions are rejected with clear error messages.
 * 
 * Allowed transitions (23 total):
 * - CREATED → VALIDATED
 * - VALIDATED → CHALLENGE_PENDING | PROCESSING
 * - CHALLENGE_PENDING → AUTHENTICATED | DECLINED | FAILED
 * - AUTHENTICATED → PROCESSING
 * - PROCESSING → COMPLETED | DECLINED | UNKNOWN | FAILED
 * - UNKNOWN → COMPLETED | DECLINED | FAILED
 */
@Service
public class PaymentStateMachine {
    
    private final Map<PaymentStatus, Set<PaymentStatus>> allowedTransitions;
    
    public PaymentStateMachine() {
        this.allowedTransitions = buildAllowedTransitions();
    }
    
    /**
     * Validates that a state transition is allowed.
     * 
     * @param from Source state
     * @param to Destination state
     * @throws InvalidStateTransitionException if transition not allowed
     */
    public void validateTransition(PaymentStatus from, PaymentStatus to) {
        if (from == null) {
            throw new InvalidStateTransitionException(from, to, "from state cannot be null");
        }
        if (to == null) {
            throw new InvalidStateTransitionException(from, to, "to state cannot be null");
        }
        
        Set<PaymentStatus> allowedDestinations = allowedTransitions.get(from);
        
        if (allowedDestinations == null || !allowedDestinations.contains(to)) {
            throw new InvalidStateTransitionException(
                from, 
                to, 
                String.format("No allowed transition from %s to %s", from, to)
            );
        }
    }
    
    /**
     * Validates that a state transition is allowed.
     * 
     * @param transition The state transition to validate
     * @throws InvalidStateTransitionException if transition not allowed
     */
    public void validateTransition(StateTransition transition) {
        validateTransition(transition.getFrom(), transition.getTo());
    }
    
    /**
     * Checks if a transition is allowed without throwing an exception.
     * 
     * @param from Source state
     * @param to Destination state
     * @return true if transition is allowed, false otherwise
     */
    public boolean isTransitionAllowed(PaymentStatus from, PaymentStatus to) {
        if (from == null || to == null) {
            return false;
        }
        
        Set<PaymentStatus> allowedDestinations = allowedTransitions.get(from);
        return allowedDestinations != null && allowedDestinations.contains(to);
    }
    
    /**
     * Builds the complete set of allowed state transitions.
     * 
     * @return Map of source state to set of allowed destination states
     */
    private Map<PaymentStatus, Set<PaymentStatus>> buildAllowedTransitions() {
        Map<PaymentStatus, Set<PaymentStatus>> transitions = new HashMap<>();
        
        // CREATED can transition to VALIDATED
        transitions.put(PaymentStatus.CREATED, Set.of(
            PaymentStatus.VALIDATED
        ));
        
        // VALIDATED can transition to CHALLENGE_PENDING or PROCESSING
        transitions.put(PaymentStatus.VALIDATED, Set.of(
            PaymentStatus.CHALLENGE_PENDING,
            PaymentStatus.PROCESSING
        ));
        
        // CHALLENGE_PENDING can transition to AUTHENTICATED, DECLINED, or FAILED
        transitions.put(PaymentStatus.CHALLENGE_PENDING, Set.of(
            PaymentStatus.AUTHENTICATED,
            PaymentStatus.DECLINED,
            PaymentStatus.FAILED
        ));
        
        // AUTHENTICATED can transition to PROCESSING
        transitions.put(PaymentStatus.AUTHENTICATED, Set.of(
            PaymentStatus.PROCESSING
        ));
        
        // PROCESSING can transition to COMPLETED, DECLINED, UNKNOWN, or FAILED
        transitions.put(PaymentStatus.PROCESSING, Set.of(
            PaymentStatus.COMPLETED,
            PaymentStatus.DECLINED,
            PaymentStatus.UNKNOWN,
            PaymentStatus.FAILED
        ));
        
        // UNKNOWN can transition to COMPLETED, DECLINED, or FAILED
        transitions.put(PaymentStatus.UNKNOWN, Set.of(
            PaymentStatus.COMPLETED,
            PaymentStatus.DECLINED,
            PaymentStatus.FAILED
        ));
        
        // Terminal states: COMPLETED, DECLINED, FAILED
        // No transitions allowed from terminal states
        transitions.put(PaymentStatus.COMPLETED, Set.of());
        transitions.put(PaymentStatus.DECLINED, Set.of());
        transitions.put(PaymentStatus.FAILED, Set.of());
        
        return transitions;
    }
}
