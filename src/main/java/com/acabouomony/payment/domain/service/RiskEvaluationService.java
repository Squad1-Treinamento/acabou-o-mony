package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.config.RiskEvaluationProperties;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.RiskLevel;
import com.acabouomony.payment.domain.service.risk.AmountRiskRule;
import com.acabouomony.payment.domain.service.risk.NewCardRiskRule;
import com.acabouomony.payment.domain.service.risk.RiskRule;
import com.acabouomony.payment.domain.service.risk.VelocityRiskRule;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Risk-based authentication evaluation service.
 * 
 * Evaluates transaction risk level to determine if 3DS is required.
 * Uses pluggable risk rules for flexible risk evaluation.
 * 
 * Spec: spec-001-core-payment-processing.md - Risk Evaluation Rules
 * Task: task-018-risk-evaluation.md
 * 
 * Risk Evaluation Rules:
 * - High-risk: new card, high amount, unusual geography, velocity checks
 * - Low-risk: card previously used, low amount, trusted merchant
 * 
 * If high-risk: VALIDATED -> CHALLENGE_PENDING (3DS required)
 * If low-risk: VALIDATED -> PROCESSING (skip 3DS, preserve <1s SLA)
 * 
 * Rule Combination:
 * - Rules are combined with OR logic
 * - If ANY rule flags as high-risk, transaction is high-risk
 * - Only if ALL rules flag as low-risk, transaction is low-risk
 */
@Service
public class RiskEvaluationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskEvaluationService.class);
    
    private final RiskEvaluationProperties properties;
    private final TransactionRepository transactionRepository;
    private final List<RiskRule> riskRules;
    
    /**
     * Creates risk evaluation service with configurable rules.
     * 
     * @param properties Risk evaluation configuration
     * @param transactionRepository Transaction repository for card history lookup
     */
    public RiskEvaluationService(
            RiskEvaluationProperties properties,
            TransactionRepository transactionRepository) {
        this.properties = properties;
        this.transactionRepository = transactionRepository;
        this.riskRules = initializeRiskRules();
    }
    
    /**
     * Initializes risk rules based on configuration.
     * 
     * @return List of enabled risk rules
     */
    private List<RiskRule> initializeRiskRules() {
        List<RiskRule> rules = new ArrayList<>();
        
        // Amount risk rule (always enabled)
        rules.add(new AmountRiskRule(properties.getAmountThreshold()));
        
        // New card risk rule (configurable)
        if (properties.isEnableNewCardCheck()) {
            rules.add(new NewCardRiskRule(transactionRepository));
        }
        
        // Velocity risk rule (configurable)
        if (properties.isEnableVelocityCheck()) {
            rules.add(new VelocityRiskRule(
                transactionRepository,
                properties.getMaxTransactionsPerMinute(),
                properties.getVelocityWindowMinutes()
            ));
        }
        
        logger.info("Risk evaluation initialized with {} rules", rules.size());
        return rules;
    }
    
    /**
     * Evaluates transaction risk level.
     * 
     * Returns RiskLevel.HIGH if any rule flags as high-risk.
     * Returns RiskLevel.LOW if all rules flag as low-risk.
     * 
     * @param transaction The transaction to evaluate
     * @return Risk level (HIGH or LOW)
     */
    public RiskLevel evaluateRisk(Transaction transaction) {
        if (!properties.isEnabled()) {
            logger.debug("Risk evaluation disabled, returning LOW-RISK: transaction_id={}", transaction.getId());
            return RiskLevel.LOW;
        }
        
        if (transaction == null) {
            logger.warn("Cannot evaluate risk for null transaction");
            return RiskLevel.HIGH;  // Default to high-risk for safety
        }
        
        // Evaluate all rules
        for (RiskRule rule : riskRules) {
            try {
                if (rule.isHighRisk(transaction)) {
                    logger.info("Transaction flagged as HIGH-RISK: transaction_id={}, rule={}",
                        transaction.getId(), rule.getName());
                    return RiskLevel.HIGH;
                }
            } catch (Exception e) {
                logger.error("Error evaluating risk rule {}: transaction_id={}, error={}",
                    rule.getName(), transaction.getId(), e.getMessage(), e);
                // Default to high-risk on error for safety
                return RiskLevel.HIGH;
            }
        }
        
        logger.debug("Transaction evaluated as LOW-RISK: transaction_id={}", transaction.getId());
        return RiskLevel.LOW;
    }
    
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
        return evaluateRisk(transaction) == RiskLevel.HIGH;
    }
    
    /**
     * Evaluates if transaction is low-risk.
     * 
     * @param transaction The transaction to evaluate
     * @return true if low-risk, false if high-risk
     */
    public boolean isLowRisk(Transaction transaction) {
        return evaluateRisk(transaction) == RiskLevel.LOW;
    }
    
    /**
     * Adds custom risk rule.
     * 
     * Allows runtime addition of custom rules.
     * 
     * @param rule The risk rule to add
     */
    public void addRiskRule(RiskRule rule) {
        if (rule != null) {
            riskRules.add(rule);
            logger.info("Risk rule added: {}", rule.getName());
        }
    }
    
    /**
     * Removes risk rule by name.
     * 
     * @param ruleName The name of the rule to remove
     */
    public void removeRiskRule(String ruleName) {
        riskRules.removeIf(rule -> rule.getName().equals(ruleName));
        logger.info("Risk rule removed: {}", ruleName);
    }
    
    /**
     * Returns list of active risk rules.
     * 
     * @return List of risk rules
     */
    public List<RiskRule> getRiskRules() {
        return new ArrayList<>(riskRules);
    }
}
