package com.acabouomony.payment.domain.service.risk;

import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
@Service
@RequiredArgsConstructor
public class NewCardRiskRule implements RiskRule {
    
    private final TransactionRepository transactionRepository;

    /**
     * A transaction is high-risk if it's the first time we've seen this card.
     * This is determined by checking if any *other* transactions exist for this card.
     */
    @Override
    public boolean isHighRisk(Transaction transaction) {
        return !transactionRepository.existsByCardTokenIdAndMerchantIdAndIdNot(
            transaction.getCardTokenId(),
            transaction.getMerchantId(),
            transaction.getId()
        );
    }

    @Override
    public String getName() {
        return "NewCardRiskRule";
    }
}
