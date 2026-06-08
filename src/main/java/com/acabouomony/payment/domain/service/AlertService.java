package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.Transaction;

/**
 * Service for alerting operators about critical payment events.
 * 
 * Provides notification mechanism for events requiring operator attention:
 * - UNKNOWN state transitions
 * - Optimistic lock failures
 * - Reconciliation failures
 * - Stale UNKNOWN transactions
 * 
 * Spec: spec-001-core-payment-processing.md - Monitoring & Alerting
 * Task: task-013-unknown-state-handling.md
 * 
 * Implementation Strategy:
 * - Interface allows multiple implementations (log-based, email, Slack, PagerDuty)
 * - Default implementation: log-based (simple, no external dependencies)
 * - Production: can be extended to integrate with monitoring systems
 */
public interface AlertService {
    
    /**
     * Alerts operator about transaction transitioning to UNKNOWN state.
     * 
     * @param transaction The transaction in UNKNOWN state
     * @param reason The reason for UNKNOWN transition
     */
    void alertUnknownStateTransition(Transaction transaction, String reason);
    
    /**
     * Alerts operator about optimistic lock failure after max retries.
     * 
     * @param transaction The transaction that failed to update
     * @param attempts Number of retry attempts made
     */
    void alertOptimisticLockFailure(Transaction transaction, int attempts);
    
    /**
     * Alerts operator about reconciliation failure after max retries.
     * 
     * @param transaction The transaction that failed reconciliation
     * @param attempts Number of reconciliation attempts made
     */
    void alertReconciliationFailure(Transaction transaction, int attempts);
    
    /**
     * Alerts operator about stale UNKNOWN transaction.
     * 
     * @param transaction The stale transaction
     * @param ageMinutes Age of transaction in minutes
     */
    void alertStaleUnknownTransaction(Transaction transaction, long ageMinutes);
    
    /**
     * Alerts operator about reconciliation queue overflow.
     * 
     * @param merchantId The merchant with queue overflow
     * @param queueSize Current queue size
     */
    void alertReconciliationQueueOverflow(java.util.UUID merchantId, int queueSize);
}
