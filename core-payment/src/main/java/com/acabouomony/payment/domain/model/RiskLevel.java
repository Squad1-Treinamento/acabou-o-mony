package com.acabouomony.payment.domain.model;

/**
 * Risk level enumeration for transaction risk evaluation.
 * 
 * Determines whether 3DS authentication is required.
 * 
 * Spec: spec-001-core-payment-processing.md - Risk Evaluation Rules
 * Task: task-018-risk-evaluation.md
 */
public enum RiskLevel {
    /**
     * Low-risk transaction.
     * Skip 3DS, proceed directly to PROCESSING.
     * Preserves <1 second SLA guarantee.
     */
    LOW,
    
    /**
     * High-risk transaction.
     * Require 3DS authentication.
     * Transition to CHALLENGE_PENDING.
     */
    HIGH
}
