package com.acabouomony.payment.domain.service.risk;

import com.acabouomony.payment.domain.entity.Transaction;

/**
 * Interface for risk evaluation rules.
 * 
 * Allows pluggable risk evaluation logic.
 * Each rule evaluates a specific risk factor.
 * 
 * Spec: spec-001-core-payment-processing.md - Risk Evaluation Rules
 * Task: task-018-risk-evaluation.md
 * 
 * Rule Pattern:
 * - Each rule evaluates one risk factor
 * - Returns true if risk factor indicates high-risk
 * - Returns false if risk factor indicates low-risk
 * - Rules are combined with OR logic (any high-risk rule → high-risk transaction)
 */
public interface RiskRule {
    
    /**
     * Evaluates if transaction is high-risk based on this rule.
     * 
     * @param transaction The transaction to evaluate
     * @return true if high-risk, false if low-risk
     */
    boolean isHighRisk(Transaction transaction);
    
    /**
     * Returns rule name for logging and debugging.
     * 
     * @return Rule name
     */
    String getName();
}
