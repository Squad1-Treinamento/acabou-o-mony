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
 * Handler for transitioning transactions to UNKNOWN state.
 * 
 * Implements safe state transition logic for timeout and error scenarios.
 * Ensures atomic persistence of transaction, audit log, and outbox event.
 * 
 * Spec: spec-001-core-payment-processing.md - Timeout & Uncertain Payment Rules
 * Task: task-013-unknown-state-handling.md
 * 
 * Transition Rules:
 * - Only valid from PROCESSING state
 * - Increments transaction version (optimistic locking)
 * - Creates audit log entry with checksum
 * - Creates outbox event for webhook notification
 * - All three persist atomically (same DB transaction)
 * 
 * Retry Strategy:
 * - Retry up to 3 times on OptimisticLockException
 * - Exponential backoff: 100ms, 200ms, 300ms
 * - Fail fast after 3 attempts (operator intervention required)
 * 
 * Logging:
 * - WARN level for timeout transitions
 * - Include: transaction_id, merchant_id, amount, reason
 * - Exclude: sensitive data (PAN, API keys)
 * 
 * Metrics:
 * - payment.unknown.transitions.total (counter)
 * - payment.timeout.errors.total (counter)
 * - payment.acquirer.errors.total (counter)
 * - payment.optimistic.lock.conflicts.total (counter)
 */
@Service
public class UnknownStateTransitionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(UnknownStateTransitionHandler.class);
    
    private static final int MAX_RETRIES = 3;
    private static final long BACKOFF_BASE_MS = 100;
    
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;
    private final OutboxEventService outboxEventService;
    private final StateTransitionValidator stateTransitionValidator;
    private final PaymentMetrics paymentMetrics;
    private final AlertService alertService;
    
    public UnknownStateTransitionHandler(
            TransactionRepository transactionRepository,
            AuditLogService auditLogService,
            OutboxEventService outboxEventService,
            StateTransitionValidator stateTransitionValidator,
            PaymentMetrics paymentMetrics,
            AlertService alertService) {
        this.transactionRepository = transactionRepository;
        this.auditLogService = auditLogService;
        this.outboxEventService = outboxEventService;
        this.stateTransitionValidator = stateTransitionValidator;
        this.paymentMetrics = paymentMetrics;
        this.alertService = alertService;
    }
    
    /**
     * Transitions transaction to UNKNOWN state due to timeout.
     * 
     * Called when Mercado Pago times out during payment submission.
     * Safely transitions PROCESSING -> UNKNOWN with atomic persistence.
     * 
     * @param transaction The transaction in PROCESSING state
     * @param reason The reason for timeout (e.g., "Mercado Pago read timeout")
     */
    public void transitionToUnknownDueToTimeout(Transaction transaction, String reason) {
        logger.warn("Transaction timed out during Mercado Pago call, transitioning to UNKNOWN: " +
                "transaction_id={}, merchant_id={}, amount={}, reason={}",
            transaction.getId(), transaction.getMerchantId(), transaction.getAmount(), reason);
        
        paymentMetrics.recordTimeoutError();
        alertService.alertUnknownStateTransition(transaction, reason);
        transitionToUnknown(transaction, reason);
    }
    
    /**
     * Transitions transaction to UNKNOWN state due to acquirer error.
     * 
     * Called when Mercado Pago returns an error (5xx, network error, etc.).
     * Safely transitions PROCESSING -> UNKNOWN with atomic persistence.
     * 
     * @param transaction The transaction in PROCESSING state
     * @param reason The reason for error (e.g., "Mercado Pago connection failed")
     */
    public void transitionToUnknownDueToAcquirerError(Transaction transaction, String reason) {
        logger.warn("Transaction encountered acquirer error, transitioning to UNKNOWN: " +
                "transaction_id={}, merchant_id={}, amount={}, reason={}",
            transaction.getId(), transaction.getMerchantId(), transaction.getAmount(), reason);
        
        paymentMetrics.recordAcquirerError();
        alertService.alertUnknownStateTransition(transaction, reason);
        transitionToUnknown(transaction, reason);
    }
    
    /**
     * Transitions transaction to UNKNOWN state.
     * 
     * Core implementation with optimistic locking retry logic.
     * 
     * @param transaction The transaction to transition
     * @param reason The reason for transition
     */
    private void transitionToUnknown(Transaction transaction, String reason) {
        PaymentStatus oldStatus = transaction.getStatus();
        PaymentStatus newStatus = PaymentStatus.UNKNOWN;
        
        // Validate transition
        stateTransitionValidator.validateTransition(oldStatus, newStatus);
        
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
                auditLogService.logStateTransition(transaction, oldStatus, newStatus, "system");
                
                // Create outbox event for webhook (same transaction)
                outboxEventService.createPaymentUnknownEvent(transaction);
                
                // Record metric
                paymentMetrics.recordUnknownStateTransition();
                
                logger.info("Transaction successfully transitioned to UNKNOWN: " +
                        "transaction_id={}, version={}, reason={}",
                    transaction.getId(), transaction.getVersion(), reason);
                
                return;  // Success
                
            } catch (ObjectOptimisticLockingFailureException e) {
                // Version conflict: retry
                paymentMetrics.recordOptimisticLockConflict();
                
                if (attempt == MAX_RETRIES - 1) {
                    // Final attempt failed
                    logger.error("OptimisticLockException after {} attempts, failing fast: transaction_id={}",
                        MAX_RETRIES, transaction.getId());
                    alertService.alertOptimisticLockFailure(transaction, MAX_RETRIES);
                    throw new RuntimeException(
                        "Failed to transition transaction to UNKNOWN after " + MAX_RETRIES + " attempts", e);
                }
                
                // Exponential backoff
                long backoffMs = BACKOFF_BASE_MS * (attempt + 1);
                logger.warn("Version conflict during UNKNOWN transition, retrying after {}ms: " +
                        "transaction_id={}, attempt={}/{}",
                    backoffMs, transaction.getId(), attempt + 1, MAX_RETRIES);
                
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted during retry backoff: transaction_id={}", transaction.getId());
                    throw new RuntimeException("Interrupted during UNKNOWN transition retry", ie);
                }
                
                // Re-query transaction for fresh version
                Transaction fresh = transactionRepository.findById(transaction.getId())
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + transaction.getId()));
                
                // Check if already in UNKNOWN state (idempotent)
                if (fresh.getStatus() == PaymentStatus.UNKNOWN) {
                    logger.info("Transaction already transitioned to UNKNOWN by concurrent operation: " +
                            "transaction_id={}, version={}",
                        transaction.getId(), fresh.getVersion());
                    transaction.setVersion(fresh.getVersion());
                    transaction.setUpdatedAt(fresh.getUpdatedAt());
                    return;  // Success (idempotent)
                }
                
                // Update transaction reference for next attempt
                transaction.setVersion(fresh.getVersion());
            }
        }
    }
}
