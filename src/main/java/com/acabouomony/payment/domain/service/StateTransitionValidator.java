package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.model.PaymentStatus;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Validates payment state transitions.
 * 
 * Enforces state machine rules from spec.
 * 
 * Spec: spec-001-core-payment-processing.md - Allowed State Transitions
 * Task: task-012-payment-orchestration.md
 * 
 * Valid transitions:
 * - CREATED -> VALIDATED
 * - VALIDATED -> CHALLENGE_PENDING (high-risk)
 * - VALIDATED -> PROCESSING (low-risk)
 * - CHALLENGE_PENDING -> AUTHENTICATED
 * - CHALLENGE_PENDING -> DECLINED
 * - CHALLENGE_PENDING -> FAILED
 * - AUTHENTICATED -> PROCESSING
 * - PROCESSING -> COMPLETED
 * - PROCESSING -> DECLINED
 * - PROCESSING -> FAILED
 * - PROCESSING -> UNKNOWN
 * - UNKNOWN -> COMPLETED
 * - UNKNOWN -> DECLINED
 * - UNKNOWN -> FAILED
 */
@Component
public class StateTransitionValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(StateTransitionValidator.class);
    
    private final Map<PaymentStatus, Set<PaymentStatus>> allowedTransitions;

    public StateTransitionValidator() {
        allowedTransitions = new HashMap<>();

        // Terminal states have no transitions
        allowedTransitions.put(PaymentStatus.COMPLETED, new HashSet<>());
        allowedTransitions.put(PaymentStatus.FAILED, new HashSet<>());
        allowedTransitions.put(PaymentStatus.VOIDED, new HashSet<>());
        allowedTransitions.put(PaymentStatus.REFUNDED, new HashSet<>());

        // Valid transitions from non-terminal states
        allowedTransitions.put(PaymentStatus.CREATED,
                new HashSet<>(Arrays.asList(PaymentStatus.AUTHORIZED, PaymentStatus.FAILED)));

        allowedTransitions.put(PaymentStatus.AUTHORIZED,
                new HashSet<>(Arrays.asList(PaymentStatus.COMPLETED, PaymentStatus.VOIDED, PaymentStatus.FAILED)));

        allowedTransitions.put(PaymentStatus.UNKNOWN,
                new HashSet<>(Arrays.asList(PaymentStatus.AUTHORIZED, PaymentStatus.COMPLETED, PaymentStatus.FAILED, PaymentStatus.VOIDED)));
    }
    /**
     * Validates if transition is allowed.
     * 
     * @param from Current status
     * @param to Target status
     * @return true if transition is valid
     */
    public boolean isValidTransition(PaymentStatus from, PaymentStatus to) {
        if (from == null || to == null) {
            return false;
        }
        
        Set<PaymentStatus> validTransitionsFromState = allowedTransitions.get(from);
        return validTransitionsFromState != null && validTransitionsFromState.contains(to);
    }
    
    /**
     * Validates transition and throws exception if invalid.
     * 
     * @param from Current status
     * @param to Target status
     * @throws IllegalStateException if transition is invalid
     */
    public void validateTransition(PaymentStatus from, PaymentStatus to) {
        if (!isValidTransition(from, to)) {
            String message = String.format("Invalid state transition: %s -> %s", from, to);
            logger.error(message);
            throw new IllegalStateException(message);
        }
    }
}

