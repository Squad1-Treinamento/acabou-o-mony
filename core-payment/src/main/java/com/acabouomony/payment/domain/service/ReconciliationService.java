package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.config.UnknownStateProperties;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for reconciling UNKNOWN state transactions.
 * 
 * Queries Mercado Pago for final payment status and transitions transaction
 * to terminal state (COMPLETED/DECLINED/FAILED).
 * 
 * Spec: spec-001-core-payment-processing.md - Reconciliation Rules
 * Task: task-014-reconciliation-worker.md, task-015-reconciliation-logic.md
 * 
 * Reconciliation Flow:
 * 1. Query Mercado Pago using acquirer_reference
 * 2. Map Mercado Pago status to PaymentStatus
 * 3. Transition transaction to final state
 * 4. Create audit log entry (actor: "reconciliation")
 * 5. Create outbox event for webhook notification
 * 
 * Retry Strategy:
 * - Maximum 3 retry attempts
 * - Exponential backoff: 1s, 2s
 * - After 3 failures: transition to FAILED (assume worst case)
 * 
 * Idempotency:
 * - Multiple reconciliation attempts for same transaction are safe
 * - Optimistic locking prevents concurrent updates
 * - Webhook may resolve transaction first (race condition handled)
 */
@Service
public class ReconciliationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);
    
    private final PaymentAcquirerClient acquirerClient;
    private final ReconciliationStateTransitionHandler stateTransitionHandler;
    private final UnknownStateProperties properties;
    private final AlertService alertService;
    
    public ReconciliationService(
            PaymentAcquirerClient acquirerClient,
            ReconciliationStateTransitionHandler stateTransitionHandler,
            UnknownStateProperties properties,
            AlertService alertService) {
        this.acquirerClient = acquirerClient;
        this.stateTransitionHandler = stateTransitionHandler;
        this.properties = properties;
        this.alertService = alertService;
    }
    
    /**
     * Attempts to reconcile UNKNOWN transaction.
     * 
     * Queries Mercado Pago for final status and transitions transaction.
     * Returns true if successfully reconciled, false if needs retry.
     * 
     * @param transaction The UNKNOWN transaction
     * @return true if reconciled, false if needs retry
     */
    public boolean attemptReconciliation(Transaction transaction) {
        logger.info("Attempting reconciliation: transaction_id={}, merchant_id={}, acquirer_ref={}",
            transaction.getId(), transaction.getMerchantId(), transaction.getAcquirerReference());
        
        // Validate transaction state
        if (transaction.getStatus() != PaymentStatus.UNKNOWN) {
            logger.warn("Transaction not in UNKNOWN state, skipping reconciliation: " +
                    "transaction_id={}, status={}",
                transaction.getId(), transaction.getStatus());
            return true;  // Already resolved
        }
        
        // Validate acquirer reference exists
        if (transaction.getAcquirerReference() == null || transaction.getAcquirerReference().isEmpty()) {
            logger.error("Transaction missing acquirer reference, cannot reconcile: transaction_id={}",
                transaction.getId());
            // Transition to FAILED (cannot reconcile without acquirer reference)
            stateTransitionHandler.transitionToFailed(transaction);
            return true;  // Resolved (as FAILED)
        }
        
        // Retry loop
        int maxRetries = properties.getMaxRetryAttempts();
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // Query Mercado Pago for status
                PaymentStatus status = acquirerClient.queryPaymentStatus(transaction.getAcquirerReference());
                
                logger.info("Reconciliation query result: transaction_id={}, status={}, attempt={}/{}",
                    transaction.getId(), status, attempt + 1, maxRetries);
                
                // Handle result
                switch (status) {
                    case COMPLETED:
                        stateTransitionHandler.transitionToCompleted(transaction);
                        return true;  // Success
                        
                    case DECLINED:
                        stateTransitionHandler.transitionToDeclined(transaction);
                        return true;  // Success
                        
                    case FAILED:
                        stateTransitionHandler.transitionToFailed(transaction);
                        return true;  // Success
                        
                    case UNKNOWN:
                        // Still unknown at acquirer, retry
                        if (attempt == maxRetries - 1) {
                            // Final attempt, assume worst case
                            logger.warn("Reconciliation exhausted, status still UNKNOWN: transaction_id={}",
                                transaction.getId());
                            stateTransitionHandler.transitionToFailed(transaction);
                            alertService.alertReconciliationFailure(transaction, maxRetries);
                            return true;  // Resolved (as FAILED)
                        }
                        
                        // Retry with backoff
                        long backoffMs = 1000L * (attempt + 1);  // 1s, 2s
                        logger.debug("Status still UNKNOWN, retrying after {}ms: transaction_id={}, attempt={}/{}",
                            backoffMs, transaction.getId(), attempt + 1, maxRetries);
                        
                        Thread.sleep(backoffMs);
                        break;
                        
                    default:
                        logger.warn("Unexpected status from reconciliation: transaction_id={}, status={}",
                            transaction.getId(), status);
                        stateTransitionHandler.transitionToFailed(transaction);
                        return true;  // Resolved (as FAILED)
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Reconciliation interrupted: transaction_id={}", transaction.getId());
                return false;  // Needs retry
                
            } catch (Exception e) {
                logger.error("Error during reconciliation: transaction_id={}, attempt={}/{}, error={}",
                    transaction.getId(), attempt + 1, maxRetries, e.getMessage(), e);
                
                if (attempt == maxRetries - 1) {
                    // Final attempt failed
                    logger.error("Reconciliation exhausted after {} attempts: transaction_id={}",
                        maxRetries, transaction.getId());
                    stateTransitionHandler.transitionToFailed(transaction);
                    alertService.alertReconciliationFailure(transaction, maxRetries);
                    return true;  // Resolved (as FAILED)
                }
                
                // Retry with backoff
                try {
                    long backoffMs = 1000L * (attempt + 1);
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;  // Needs retry
                }
            }
        }
        
        // Should not reach here
        logger.error("Reconciliation logic error: transaction_id={}", transaction.getId());
        return false;  // Needs retry
    }
}
