package com.acabouomony.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for risk evaluation.
 * 
 * Allows configurable risk thresholds without code changes.
 * 
 * Spec: spec-001-core-payment-processing.md - Risk Evaluation Rules
 * Task: task-018-risk-evaluation.md
 * 
 * Properties:
 * - risk.evaluation.enabled: Enable/disable risk evaluation
 * - risk.evaluation.amount-threshold: Amount threshold in cents
 * - risk.evaluation.enable-new-card-check: Enable new card risk check
 * - risk.evaluation.enable-velocity-check: Enable velocity risk check
 * - risk.evaluation.max-transactions-per-minute: Max transactions per minute
 * - risk.evaluation.velocity-window-minutes: Velocity check time window
 */
@Component
@ConfigurationProperties(prefix = "risk.evaluation")
public class RiskEvaluationProperties {
    
    /**
     * Enable/disable risk evaluation.
     * Default: true
     */
    private boolean enabled = true;
    
    /**
     * Amount threshold in cents (e.g., 50000 = 500 BRL).
     * Transactions above this amount are flagged as high-risk.
     * Default: 50000 (500 BRL)
     */
    private long amountThreshold = 50000L;
    
    /**
     * Enable new card risk check.
     * Default: true
     */
    private boolean enableNewCardCheck = true;
    
    /**
     * Enable velocity risk check.
     * Default: true
     */
    private boolean enableVelocityCheck = true;
    
    /**
     * Maximum transactions per minute (for velocity check).
     * Default: 10 transactions per minute
     */
    private int maxTransactionsPerMinute = 10;
    
    /**
     * Velocity check time window in minutes.
     * Default: 5 minutes
     */
    private int velocityWindowMinutes = 5;
    
    // Getters and Setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public long getAmountThreshold() {
        return amountThreshold;
    }
    
    public void setAmountThreshold(long amountThreshold) {
        this.amountThreshold = amountThreshold;
    }
    
    public boolean isEnableNewCardCheck() {
        return enableNewCardCheck;
    }
    
    public void setEnableNewCardCheck(boolean enableNewCardCheck) {
        this.enableNewCardCheck = enableNewCardCheck;
    }
    
    public boolean isEnableVelocityCheck() {
        return enableVelocityCheck;
    }
    
    public void setEnableVelocityCheck(boolean enableVelocityCheck) {
        this.enableVelocityCheck = enableVelocityCheck;
    }
    
    public int getMaxTransactionsPerMinute() {
        return maxTransactionsPerMinute;
    }
    
    public void setMaxTransactionsPerMinute(int maxTransactionsPerMinute) {
        this.maxTransactionsPerMinute = maxTransactionsPerMinute;
    }
    
    public int getVelocityWindowMinutes() {
        return velocityWindowMinutes;
    }
    
    public void setVelocityWindowMinutes(int velocityWindowMinutes) {
        this.velocityWindowMinutes = velocityWindowMinutes;
    }
}
