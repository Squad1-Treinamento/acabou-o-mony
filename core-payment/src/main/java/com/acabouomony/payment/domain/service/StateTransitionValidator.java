package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.model.PaymentStatus;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
@Service
public class StateTransitionValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(StateTransitionValidator.class);
    
    private static final Map<PaymentStatus, java.util.Set<PaymentStatus>> VALID_TRANSITIONS = Map.ofEntries(
        Map.entry(PaymentStatus.CREATED, java.util.Set.of(PaymentStatus.VALIDATED)),
        Map.entry(PaymentStatus.VALIDATED, java.util.Set.of(PaymentStatus.CHALLENGE_PENDING, PaymentStatus.PROCESSING)),
        Map.entry(PaymentStatus.CHALLENGE_PENDING, java.util.Set.of(PaymentStatus.AUTHENTICATED, PaymentStatus.DECLINED, PaymentStatus.FAILED)),
        Map.entry(PaymentStatus.AUTHENTICATED, java.util.Set.of(PaymentStatus.PROCESSING)),
        Map.entry(PaymentStatus.PROCESSING, java.util.Set.of(PaymentStatus.COMPLETED, PaymentStatus.DECLINED, PaymentStatus.FAILED, PaymentStatus.UNKNOWN)),
        Map.entry(PaymentStatus.UNKNOWN, java.util.Set.of(PaymentStatus.COMPLETED, PaymentStatus.DECLINED, PaymentStatus.FAILED))
    );
    
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
        
        java.util.Set<PaymentStatus> allowedTransitions = VALID_TRANSITIONS.get(from);
        return allowedTransitions != null && allowedTransitions.contains(to);
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

