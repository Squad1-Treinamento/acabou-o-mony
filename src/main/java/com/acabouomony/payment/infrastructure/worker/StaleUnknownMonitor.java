package com.acabouomony.payment.infrastructure.worker;

import com.acabouomony.payment.config.UnknownStateProperties;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.AlertService;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Background monitor for detecting stale UNKNOWN transactions.
 * 
 * Scans for UNKNOWN transactions exceeding 5-minute reconciliation window.
 * Alerts operators for manual intervention.
 * 
 * Spec: spec-001-core-payment-processing.md - Reconciliation Behavior & Termination
 * Task: task-013-unknown-state-handling.md
 * 
 * Detection Rules:
 * - UNKNOWN transaction > 5 minutes old = stale
 * - Log ERROR with transaction details
 * - Alert operator for manual intervention
 * - After 24 hours: automatic transition to FAILED (future enhancement)
 * 
 * Polling Strategy:
 * - Runs every 60 seconds (low frequency, not time-critical)
 * - Queries all UNKNOWN transactions older than threshold
 * - No batch limit (expect small number of stale transactions)
 */
@Component
public class StaleUnknownMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(StaleUnknownMonitor.class);
    
    private final TransactionRepository transactionRepository;
    private final UnknownStateProperties properties;
    private final AlertService alertService;
    
    public StaleUnknownMonitor(
            TransactionRepository transactionRepository,
            UnknownStateProperties properties,
            AlertService alertService) {
        this.transactionRepository = transactionRepository;
        this.properties = properties;
        this.alertService = alertService;
    }
    
    /**
     * Scans for stale UNKNOWN transactions.
     * 
     * Runs every 60 seconds.
     */
    @Scheduled(fixedRate = 60000)
    public void detectStaleTransactions() {
        try {
            // Calculate stale threshold
            long thresholdMinutes = properties.getStaleThresholdMinutes();
            Instant staleThreshold = Instant.now().minus(Duration.ofMinutes(thresholdMinutes));
            
            // Query stale UNKNOWN transactions
            List<Transaction> staleTransactions = transactionRepository.findStaleUnknownTransactions(
                PaymentStatus.UNKNOWN,
                staleThreshold
            );
            
            if (staleTransactions.isEmpty()) {
                return;  // No stale transactions
            }
            
            logger.warn("Found {} stale UNKNOWN transactions exceeding {}-minute threshold",
                staleTransactions.size(), thresholdMinutes);
            
            // Alert for each stale transaction
            for (Transaction transaction : staleTransactions) {
                handleStaleTransaction(transaction, thresholdMinutes);
            }
            
        } catch (Exception e) {
            logger.error("Error in stale UNKNOWN monitor: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handles stale UNKNOWN transaction.
     * 
     * Logs error and alerts operator.
     * 
     * @param transaction The stale transaction
     * @param thresholdMinutes The stale threshold in minutes
     */
    private void handleStaleTransaction(Transaction transaction, long thresholdMinutes) {
        // Calculate age
        long ageMinutes = Duration.between(transaction.getCreatedAt(), Instant.now()).toMinutes();
        
        logger.error("STALE UNKNOWN TRANSACTION DETECTED: " +
                "transaction_id={}, merchant_id={}, amount={}, currency={}, " +
                "age_minutes={}, threshold_minutes={}, " +
                "acquirer_ref={}, " +
                "message='Transaction in UNKNOWN state for {} minutes. Expected resolution within {} minutes. Manual intervention required.'",
            transaction.getId(),
            transaction.getMerchantId(),
            transaction.getAmount(),
            transaction.getCurrency(),
            ageMinutes,
            thresholdMinutes,
            transaction.getAcquirerReference(),
            ageMinutes,
            thresholdMinutes);
        
        // Alert operator
        alertService.alertStaleUnknownTransaction(transaction, ageMinutes);
        
        // TODO: Future enhancement - automatic transition to FAILED after 24 hours
        // if (ageMinutes > 1440) {  // 24 hours
        //     stateTransitionHandler.transitionToFailed(transaction);
        // }
    }
}
