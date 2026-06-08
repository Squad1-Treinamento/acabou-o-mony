package com.acabouomony.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for UNKNOWN state handling and reconciliation.
 * 
 * Spec: spec-001-core-payment-processing.md - Reconciliation Rules
 * Task: task-013-unknown-state-handling.md
 * 
 * Properties:
 * - reconciliationDelayMs: Delay before starting reconciliation (default: 1000ms)
 * - reconciliationTimeoutMs: Maximum time to wait for reconciliation (default: 300000ms = 5 minutes)
 * - maxRetryAttempts: Maximum reconciliation retry attempts (default: 3)
 * - backoffBaseMs: Base backoff for exponential retry (default: 100ms)
 * - staleThresholdMinutes: Minutes before UNKNOWN considered stale (default: 5)
 * - maxConcurrentPerMerchant: Max concurrent reconciliation per merchant (default: 2)
 * - maxQueueDepthPerMerchant: Max reconciliation queue depth per merchant (default: 10)
 */
@Configuration
@ConfigurationProperties(prefix = "payment.unknown-state")
public class UnknownStateProperties {
    
    /**
     * Delay before starting reconciliation (milliseconds).
     * Default: 1000ms (1 second)
     */
    private long reconciliationDelayMs = 1000;
    
    /**
     * Maximum time to wait for reconciliation (milliseconds).
     * Default: 300000ms (5 minutes)
     */
    private long reconciliationTimeoutMs = 300000;
    
    /**
     * Maximum reconciliation retry attempts.
     * Default: 3
     */
    private int maxRetryAttempts = 3;
    
    /**
     * Base backoff for exponential retry (milliseconds).
     * Default: 100ms
     */
    private long backoffBaseMs = 100;
    
    /**
     * Minutes before UNKNOWN transaction considered stale.
     * Default: 5 minutes
     */
    private long staleThresholdMinutes = 5;
    
    /**
     * Maximum concurrent reconciliation queries per merchant.
     * Default: 2
     */
    private int maxConcurrentPerMerchant = 2;
    
    /**
     * Maximum reconciliation queue depth per merchant.
     * Default: 10
     */
    private int maxQueueDepthPerMerchant = 10;
    
    // Getters and Setters
    
    public long getReconciliationDelayMs() {
        return reconciliationDelayMs;
    }
    
    public void setReconciliationDelayMs(long reconciliationDelayMs) {
        this.reconciliationDelayMs = reconciliationDelayMs;
    }
    
    public long getReconciliationTimeoutMs() {
        return reconciliationTimeoutMs;
    }
    
    public void setReconciliationTimeoutMs(long reconciliationTimeoutMs) {
        this.reconciliationTimeoutMs = reconciliationTimeoutMs;
    }
    
    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }
    
    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }
    
    public long getBackoffBaseMs() {
        return backoffBaseMs;
    }
    
    public void setBackoffBaseMs(long backoffBaseMs) {
        this.backoffBaseMs = backoffBaseMs;
    }
    
    public long getStaleThresholdMinutes() {
        return staleThresholdMinutes;
    }
    
    public void setStaleThresholdMinutes(long staleThresholdMinutes) {
        this.staleThresholdMinutes = staleThresholdMinutes;
    }
    
    public int getMaxConcurrentPerMerchant() {
        return maxConcurrentPerMerchant;
    }
    
    public void setMaxConcurrentPerMerchant(int maxConcurrentPerMerchant) {
        this.maxConcurrentPerMerchant = maxConcurrentPerMerchant;
    }
    
    public int getMaxQueueDepthPerMerchant() {
        return maxQueueDepthPerMerchant;
    }
    
    public void setMaxQueueDepthPerMerchant(int maxQueueDepthPerMerchant) {
        this.maxQueueDepthPerMerchant = maxQueueDepthPerMerchant;
    }
}
