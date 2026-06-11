package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.monitoring.PaymentMetrics;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Handler for reconciliation state transitions.
 * 
 * Transitions UNKNOWN transactions to terminal states (COMPLETED/DECLINED/FAILED)
 * after querying Mercado Pago for final status.
 * 
 * Spec: spec-001-core-payment-processing.md - Reconciliation Rules
 * Task: task-015-reconciliation-logic.md
 * 
 * Transition Rules:
 * - UNKNOWN -> COMPLETED (payment approved at acquirer)
 * - UNKNOWN -> DECLINED (payment rejected at acquirer)
 * - UNKNOWN -> FAILED (reconciliation exhausted, assume worst case)
 * 
 * Atomic Persistence:
 * - Transaction state update
 * - Audit log entry (actor: "reconciliation")
 * - Outbox event for webhook notification
 * - All three persist in same database transaction
 * 
 * Optimistic Locking:
 * - Retry up to 3 times on version conflict
 * - Exponential backoff: 100ms, 200ms, 300ms
 * - Fail fast after 3 attempts
 */
@Service
public class ReconciliationStateTransitionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationStateTransitionHandler.class);
    
    private static final int MAX_RETRIES = 3;
    private static final long BACKOFF_BASE_MS = 100;
    private static final String ACTOR = "reconciliation";
    
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;
    private final OutboxEventService outboxEventService;
    private final StateTransitionValidator stateTransitionValidator;
    private final PaymentMetrics paymentMetrics;
    
    public ReconciliationStateTransitionHandler(
            TransactionRepository transactionRepository,
            AuditLogService auditLogService,
            OutboxEventService outboxEventService,
            StateTransitionValidator stateTransitionValidator,
            PaymentMetrics paymentMetrics) {
        this.transactionRepository = transactionRepository;
        this.auditLogService = auditLogService;
        this.outboxEventService = outboxEventService;
        this.stateTransitionValidator = stateTransitionValidator;
        this.paymentMetrics = paymentMetrics;
    }
    
    /**
     * Transitions UNKNOWN transaction to COMPLETED.
     * 
     * Called when Mercado Pago confirms payment was approved.
     * 
     * @param transaction The transaction in UNKNOWN state
     */
    public void transitionToCompleted(Transaction transaction) {
        logger.info("Reconciliation resolved to COMPLETED: transaction_id={}, merchant_id={}",
            transaction.getId(), transaction.getMerchantId());
        
        transitionState(transaction, PaymentStatus.COMPLETED);
        outboxEventService.createPaymentReconciledEvent(transaction);
    }
    
    /**
     * Transitions UNKNOWN transaction to DECLINED.
     * 
     * Called when Mercado Pago confirms payment was rejected.
     * 
     * @param transaction The transaction in UNKNOWN state
     */
    public void transitionToDeclined(Transaction transaction) {
        logger.info("Reconciliation resolved to DECLINED: transaction_id={}, merchant_id={}",
            transaction.getId(), transaction.getMerchantId());
        
        transitionState(transaction, PaymentStatus.DECLINED);
        outboxEventService.createPaymentReconciledEvent(transaction);
        }
    
    /**
 * Transitions UNKNOWN transaction to FAILED.
     * 
     * Called when reconciliation exhausted all retries without resolution.
     * Assumes worst case: payment did not process.
     * 
     * @param transaction The transaction in UNKNOWN state
     */
    public void transitionToFailed(Transaction transaction) {
        logger.warn("Reconciliation exhausted, transitioning to FAILED: transaction_id={}, merchant_id={}",
            transaction.getId(), transaction.getMerchantId());
        
        transitionState(transaction, PaymentStatus.FAILED);
        outboxEventService.createPaymentReconciledEvent(transaction);
    }
    
    /**
     * Core state transition logic with optimistic locking retry.
     * 
     * @param transaction The transaction to transition
     * @param newStatus The target status
     */
    private void transitionState(Transaction transaction, PaymentStatus newStatus) {
        PaymentStatus oldStatus = transaction.getStatus();
        
        // Validate transition
        stateTransitionValidator.validateTransition(oldStatus, newStatus);
        
        logger.debug("Reconciliation state transition: transaction_id={}, {} -> {}",
            transaction.getId(), oldStatus, newStatus);
        
        // Retry loop for optimistic locking
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                // Update transaction state
                transaction.setStatus(newStatus);
                transaction.setVersion(transaction.getVersion() + 1);
                transaction.setUpdatedAt(Instant.now());
                
                // Persist transaction (version conflict checked here)
                transactionRepository.save(transaction);
                
                // Create audit log entry (same transaction)
                auditLogService.logStateTransition(transaction, oldStatus, newStatus, ACTOR);
                
                logger.info("Reconciliation state transition successful: transaction_id={}, {} -> {}, version={}",
                    transaction.getId(), oldStatus, newStatus, transaction.getVersion());
                
                return;  // Success
                
            } catch (ObjectOptimisticLockingFailureException e) {
                // Version conflict: retry
                paymentMetrics.recordOptimisticLockConflict();
                
                if (attempt == MAX_RETRIES - 1) {
                    // Final attempt failed
                    logger.error("OptimisticLockException after {} attempts during reconciliation: transaction_id={}",
                        MAX_RETRIES, transaction.getId());
                    throw new RuntimeException(
                        "Failed to transition transaction during reconciliation after " + MAX_RETRIES + " attempts", e);
                }
                
                // Exponential backoff
                long backoffMs = BACKOFF_BASE_MS * (attempt + 1);
                logger.warn("Version conflict during reconciliation, retrying after {}ms: " +
                        "transaction_id={}, attempt={}/{}",
                    backoffMs, transaction.getId(), attempt + 1, MAX_RETRIES);
                
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted during reconciliation retry backoff: transaction_id={}",
                        transaction.getId());
                    throw new RuntimeException("Interrupted during reconciliation state transition retry", ie);
                }
                
                // Re-query transaction for fresh version
                Transaction fresh = transactionRepository.findById(transaction.getId())
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + transaction.getId()));
                
                // Check if already in target state (idempotent)
                if (fresh.getStatus() == newStatus) {
                    logger.info("Transaction already transitioned to {} by concurrent reconciliation: " +
                            "transaction_id={}, version={}",
                        newStatus, transaction.getId(), fresh.getVersion());
                    transaction.setVersion(fresh.getVersion());
                    transaction.setUpdatedAt(fresh.getUpdatedAt());
                    return;  // Success (idempotent)
                }
                
                // Check if no longer in UNKNOWN state (webhook resolved it first)
                if (fresh.getStatus() != PaymentStatus.UNKNOWN) {
                    logger.info("Transaction already resolved by webhook: transaction_id={}, status={}",
                   transaction.getId(), fresh.getStatus());
                    transaction.setStatus(fresh.getStatus());
                    transaction.setVersion(fresh.getVersion());
                    transaction.setUpdatedAt(fresh.getUpdatedAt());
                    return;  // Success (webhook won the race)
                }
                
                // Update transaction reference for next attempt
                transaction.setVersion(fresh.getVersion());
            }
        }
    }
}
