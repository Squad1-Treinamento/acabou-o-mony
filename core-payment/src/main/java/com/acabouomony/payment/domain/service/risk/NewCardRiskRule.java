package com.acabouomony.payment.domain.service.risk;

import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Risk rule: Card is new (not previously used).
 * 
 * High-risk if card has no transaction history.
 * Low-risk if card has been used before.
 * 
 * Spec: spec-001-core-payment-processing.md - Risk Evaluation Rules
 * Task: task-018-risk-evaluation.md
 * 
 * Rationale:
 * - Cards with transaction history are more trustworthy
 * - New cards are more likely to be fraudulent
 * - Requires database lookup to check card history
 */
public class NewCardRiskRule implements RiskRule {
    
    private static final Logger logger = LoggerFactory.getLogger(NewCardRiskRule.class);
    
    private final TransactionRepository transactionRepository;
    
    /**
     * Creates new card risk rule.
     * 
     * @param transactionRepository Repository for card history lookup
     */
    public NewCardRiskRule(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    @Override
    public boolean isHighRisk(Transaction transaction) {
        if (transaction == null) {
            logger.warn("Cannot evaluate new card risk for null transaction");
            return true;  // Default to high-risk for safety
        }
        
        // If no card token, treat as new card (high-risk)
        String cardTokenId = transaction.getCardTokenId();
        if (cardTokenId == null || cardTokenId.isEmpty()) {
            logger.debug("HIGH-RISK: No card token: transaction_id={}", transaction.getId());
            return true;
        }
        
        // Check if card has been used before
        try {
            long previousTransactionCount = transactionRepository.countByCardTokenIdAndMerchantIdAndStatusCompleted(
                cardTokenId,
                transaction.getMerchantId()
            );
            
            if (previousTransactionCount == 0) {
                logger.debug("HIGH-RISK: New card (no previous transactions): transaction_id={}, card_token_id={}",
                    transaction.getId(), cardTokenId);
                return true;
            }
            
            logger.debug("LOW-RISK: Card has previous transactions: transaction_id={}, card_token_id={}, count={}",
                transaction.getId(), cardTokenId, previousTransactionCount);
            return false;
            
        } catch (Exception e) {
            logger.error("Error checking card history: transaction_id={}, error={}",
                transaction.getId(), e.getMessage(), e);
            // Default to high-risk on error for safety
            return true;
        }
    }
    
    @Override
    public String getName() {
        return "NewCardRiskRule";
    }
}
