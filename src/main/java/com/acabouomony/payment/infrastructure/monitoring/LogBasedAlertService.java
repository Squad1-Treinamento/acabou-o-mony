package com.acabouomony.payment.infrastructure.monitoring;

import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Log-based implementation of AlertService.
 * 
 * Alerts are logged at ERROR level for operator visibility.
 * Production systems can extend this to integrate with:
 * - Email notifications
 * - Slack webhooks
 * - PagerDuty incidents
 * - Prometheus Alertmanager
 * 
 * Spec: spec-001-core-payment-processing.md - Monitoring & Alerting
 * Task: task-013-unknown-state-handling.md
 * 
 * Log Format:
 * - ERROR level for all alerts (ensures visibility in log aggregation)
 * - Structured format: ALERT: [type] [details]
 * - Includes: transaction_id, merchant_id, amount, reason
 * - Excludes: sensitive data (PAN, API keys)
 */
@Service
public class LogBasedAlertService implements AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogBasedAlertService.class);
    
    @Override
    public void alertUnknownStateTransition(Transaction transaction, String reason) {
        logger.error("ALERT: UNKNOWN_STATE_TRANSITION - " +
                "transaction_id={}, merchant_id={}, amount={}, currency={}, reason={}",
            transaction.getId(),
            transaction.getMerchantId(),
            transaction.getAmount(),
            transaction.getCurrency(),
            reason);
    }
    
    @Override
    public void alertOptimisticLockFailure(Transaction transaction, int attempts) {
        logger.error("ALERT: OPTIMISTIC_LOCK_FAILURE - " +
                "transaction_id={}, merchant_id={}, attempts={}, " +
                "message='Failed to update transaction after {} retry attempts. Manual intervention required.'",
            transaction.getId(),
            transaction.getMerchantId(),
            attempts,
            attempts);
    }
    
    @Override
    public void alertReconciliationFailure(Transaction transaction, int attempts) {
        logger.error("ALERT: RECONCILIATION_FAILURE - " +
                "transaction_id={}, merchant_id={}, amount={}, attempts={}, " +
                "message='Reconciliation exhausted after {} attempts. Transaction transitioned to FAILED.'",
            transaction.getId(),
            transaction.getMerchantId(),
            transaction.getAmount(),
            attempts,
            attempts);
    }
    
    @Override
    public void alertStaleUnknownTransaction(Transaction transaction, long ageMinutes) {
        logger.error("ALERT: STALE_UNKNOWN_TRANSACTION - " +
                "transaction_id={}, merchant_id={}, amount={}, age_minutes={}, " +
                "message='Transaction in UNKNOWN state for {} minutes. Expected resolution within 5 minutes.'",
            transaction.getId(),
            transaction.getMerchantId(),
            transaction.getAmount(),
            ageMinutes,
            ageMinutes);
    }
    
    @Override
    public void alertReconciliationQueueOverflow(UUID merchantId, int queueSize) {
        logger.error("ALERT: RECONCILIATION_QUEUE_OVERFLOW - " +
                "merchant_id={}, queue_size={}, max_queue_size=10, " +
                "message='Reconciliation queue overflow for merchant. Dropping new reconciliation requests.'",
            merchantId,
            queueSize);
    }
}
