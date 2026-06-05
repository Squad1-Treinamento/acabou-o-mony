package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.Transaction;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Risk-based authentication evaluation service.
 * 
 * Evaluates transaction risk level to determine if 3DS is required.
 * 
 * Spec: spec-001-core-payment-processing.md - Risk Evaluation Rules
 * Task: task-012-payment-orchestration.md
 * 
 * Risk Evaluation Rules:
 * - High-risk: new card, high amount, unusual geography, velocity checks
 * - Low-risk: card previously used, low amount, trusted merchant
 * 
 * If high-risk: VALIDATED -> CHALLENGE_PENDING (3DS required)
 * If low-risk: VALIDATED -> PROCESSING (skip 3DS, preserve <1s SLA)
 */
@Service
public class RiskEvaluationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskEvaluationService.class);
    
    // Risk evaluation thresholds
    private static final long HIGH_AMOUNT_THRESHOLD = 50000L;  // 500 BRL
    
    /**
     * Evaluates if transaction is high-risk.
     * 
     * Returns true if high-risk (requires 3DS).
     * Returns false if low-risk (skip 3DS).
     * 
     * @param transaction The transaction to evaluate
     * @return true if high-risk, false if low-risk
     */
    public boolean isHighRisk(Transaction transaction) {
        if (transaction == null) {
            logger.warn("Cannot evaluate risk for null transaction");
            return true;  // Default to high-risk for safety
        }
        
        // Rule 1: High amount
        if (transaction.getAmount() > HIGH_AMOUNT_THRESHOLD) {
            logger.debug("High-risk: amount exceeds threshold: transaction_id={}, amount={}",
                transaction.getId(), transaction.getAmount());
            return true;
        }
        
        // Rule 2: New card (no acquirer reference from previous transactions)
        // TODO: Implement card history check
        // For now, assume card is trusted if we have a token
        if (transaction.getCardTokenId() == null || transaction.getCardTokenId().isEmpty()) {
            logger.debug("High-risk: no card token: transaction_id={}", transaction.getId());
            return true;
        }
        
        // Rule 3: Unusual geography
        // TODO: Implement geography check
        
        // Rule 4: Velocity checks
        // TODO: Implement velocity check (too many transactions in short time)
        
        logger.debug("Low-risk: transaction_id={}, amount={}", transaction.getId(), transaction.getAmount());
        return false;
    }
    
    /**
     * Evaluates if transaction is low-risk.
     * 
     * @param transaction The transaction to evaluate
     * @return true if low-risk, false if high-risk
     */
    public boolean isLowRisk(Transaction transaction) {
        return !isHighRisk(transaction);
    }
}
