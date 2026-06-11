package com.acabouomony.payment.domain.service.risk;

import com.acabouomony.payment.domain.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Risk rule: Transaction amount exceeds threshold.
 * 
 * High-risk if amount > threshold.
 * Low-risk if amount <= threshold.
 * 
 * Spec: spec-001-core-payment-processing.md - Risk Evaluation Rules
 * Task: task-018-risk-evaluation.md
 * 
 * Rationale:
 * - High-value transactions are more attractive to fraudsters
 * - Issuer may require additional verification for large amounts
 * - Threshold is configurable per merchant or globally
 */
public class AmountRiskRule implements RiskRule {
    
    private static final Logger logger = LoggerFactory.getLogger(AmountRiskRule.class);
    
    private final long amountThreshold;
    
    /**
     * Creates amount risk rule with threshold.
     * 
     * @param amountThreshold Amount threshold in cents (e.g., 50000 = 500 BRL)
     */
    public AmountRiskRule(long amountThreshold) {
        this.amountThreshold = amountThreshold;
    }
    
    @Override
    public boolean isHighRisk(Transaction transaction) {
        if (transaction == null) {
            logger.warn("Cannot evaluate amount risk for null transaction");
            return true;  // Default to high-risk for safety
        }
        
        long amount = transaction.getAmount();
        
        if (amount > amountThreshold) {
            logger.debug("HIGH-RISK: Amount exceeds threshold: transaction_id={}, amount={}, threshold={}",
                transaction.getId(), amount, amountThreshold);
            return true;
        }
        
        logger.debug("LOW-RISK: Amount within threshold: transaction_id={}, amount={}, threshold={}",
            transaction.getId(), amount, amountThreshold);
        return false;
    }
    
    @Override
    public String getName() {
        return "AmountRiskRule";
    }
}
