package com.acabouomony.payment.domain.service.risk;

import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Risk rule: Transaction velocity (too many transactions in short time).
 * 
 * High-risk if merchant has too many transactions in recent time window.
 * Low-risk if transaction velocity is normal.
 * 
 * Spec: spec-001-core-payment-processing.md - Risk Evaluation Rules
 * Task: task-018-risk-evaluation.md
 * 
 * Rationale:
 * - Sudden spike in transaction volume may indicate fraud or account compromise
 * - Velocity checks detect unusual patterns
 * - Threshold is configurable per merchant
 */
public class VelocityRiskRule implements RiskRule {
    
    private static final Logger logger = LoggerFactory.getLogger(VelocityRiskRule.class);
    
    private final TransactionRepository transactionRepository;
    private final int maxTransactionsPerMinute;
    private final int timeWindowMinutes;
    
    /**
     * Creates velocity risk rule.
     * 
     * @param transactionRepository Repository for transaction history lookup
     * @param maxTransactionsPerMinute Maximum transactions allowed per minute
     * @param timeWindowMinutes Time window to check (in minutes)
     */
    public VelocityRiskRule(
            TransactionRepository transactionRepository,
            int maxTransactionsPerMinute,
            int timeWindowMinutes) {
        this.transactionRepository = transactionRepository;
        this.maxTransactionsPerMinute = maxTransactionsPerMinute;
        this.timeWindowMinutes = timeWindowMinutes;
    }
    
    @Override
    public boolean isHighRisk(Transaction transaction) {
        if (transaction == null) {
            logger.warn("Cannot evaluate velocity risk for null transaction");
            return true;  // Default to high-risk for safety
        }
        
        try {
            // Calculate time window
            Instant now = Instant.now();
            Instant windowStart = now.minusSeconds(timeWindowMinutes * 60L);
            
            // Count transactions in time window for this merchant
            long transactionCount = transactionRepository.countByMerchantIdAndCreatedAtAfter(
                transaction.getMerchantId(),
                windowStart
            );
            
            // Calculate expected max transactions in window
            long maxTransactionsInWindow = (long) maxTransactionsPerMinute * timeWindowMinutes;
            
            if (transactionCount > maxTransactionsInWindow) {
                logger.debug("HIGH-RISK: Velocity exceeded: transaction_id={}, merchant_id={}, " +
                        "count={}, max={}, window_minutes={}",
                    transaction.getId(),
                    transaction.getMerchantId(),
                    transactionCount,
                    maxTransactionsInWindow,
                    timeWindowMinutes);
                return true;
            }
            
            logger.debug("LOW-RISK: Velocity normal: transaction_id={}, merchant_id={}, " +
                    "count={}, max={}, window_minutes={}",
                transaction.getId(),
                transaction.getMerchantId(),
                transactionCount,
                maxTransactionsInWindow,
                timeWindowMinutes);
            return false;
            
        } catch (Exception e) {
            logger.error("Error checking transaction velocity: transaction_id={}, error={}",
                transaction.getId(), e.getMessage(), e);
            // Default to high-risk on error for safety
            return true;
        }
    }
    
    @Override
    public String getName() {
        return "VelocityRiskRule";
    }
}
