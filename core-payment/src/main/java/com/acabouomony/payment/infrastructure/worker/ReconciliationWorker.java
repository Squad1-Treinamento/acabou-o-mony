package com.acabouomony.payment.infrastructure.worker;

import com.acabouomony.payment.config.UnknownStateProperties;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.ReconciliationConcurrencyManager;
import com.acabouomony.payment.domain.service.ReconciliationService;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Background worker for reconciling UNKNOWN state transactions.
 * 
 * Polls database for UNKNOWN transactions and attempts reconciliation.
 * Enforces per-merchant concurrency limits to prevent starvation.
 * 
 * Spec: spec-001-core-payment-processing.md - Reconciliation Rules
 * Task: task-014-reconciliation-worker.md
 * 
 * Polling Strategy:
 * - Runs every 100ms (high frequency for low latency)
 * - Queries UNKNOWN transactions older than 1 second (avoid immediate reconciliation)
 * - Batch size: 100 transactions per poll
 * - Ordered by created_at ASC (oldest first)
 * 
 * Concurrency Control:
 * - Maximum 2 concurrent reconciliation per merchant
 * - Queue depth: 10 per merchant
 * - Drops requests and alerts on queue overflow
 * 
 * Virtual Threads:
 * - Each reconciliation runs in separate virtual thread
 * - Non-blocking I/O for Mercado Pago queries
 * - Scales to thousands of concurrent reconciliations
 */
@Component
public class ReconciliationWorker {
    
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationWorker.class);
    
    private static final int BATCH_SIZE = 100;
    
    private final TransactionRepository transactionRepository;
    private final ReconciliationService reconciliationService;
    private final ReconciliationConcurrencyManager concurrencyManager;
    private final UnknownStateProperties properties;
    
    public ReconciliationWorker(
            TransactionRepository transactionRepository,
            ReconciliationService reconciliationService,
            ReconciliationConcurrencyManager concurrencyManager,
            UnknownStateProperties properties) {
        this.transactionRepository = transactionRepository;
        this.reconciliationService = reconciliationService;
        this.concurrencyManager = concurrencyManager;
        this.properties = properties;
    }
    
    /**
     * Polls and reconciles UNKNOWN transactions.
     * 
     * Runs every 100ms for low-latency reconciliation.
     */
    @Scheduled(fixedRate = 100)
    public void reconcileUnknownTransactions() {
        try {
            // Calculate threshold: don't reconcile transactions younger than delay
            long delayMs = properties.getReconciliationDelayMs();
            Instant threshold = Instant.now().minusMillis(delayMs);
            
            // Query UNKNOWN transactions
            List<Transaction> transactions = transactionRepository.findUnknownTransactionsForReconciliation(
                PaymentStatus.UNKNOWN,
                threshold,
                PageRequest.of(0, BATCH_SIZE)
            );
            
            if (transactions.isEmpty()) {
                return;  // No work to do
            }
            
            logger.debug("Found {} UNKNOWN transactions for reconciliation", transactions.size());
            
            // Process each transaction
            for (Transaction transaction : transactions) {
                processTransaction(transaction);
            }
            
        } catch (Exception e) {
            logger.error("Error in reconciliation worker: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Processes single transaction for reconciliation.
     * 
     * Enforces concurrency limits and queuing.
     * 
     * @param transaction The transaction to reconcile
     */
    private void processTransaction(Transaction transaction) {
        UUID merchantId = transaction.getMerchantId();
        
        // Try to acquire reconciliation slot
        if (concurrencyManager.tryAcquire(merchantId)) {
            // Slot acquired, reconcile immediately
            reconcileAsync(transaction);
        } else {
            // At capacity, try to queue
            boolean queued = concurrencyManager.queue(transaction);
            
            if (!queued) {
                logger.warn("Failed to queue transaction for reconciliation (queue overflow): " +
                        "transaction_id={}, merchant_id={}",
                    transaction.getId(), merchantId);
            }
        }
        
        // Check if any queued transactions can now be processed
        processQueuedTransactions(merchantId);
    }
    
    /**
     * Processes queued transactions for merchant.
     * 
     * @param merchantId The merchant ID
     */
    private void processQueuedTransactions(UUID merchantId) {
        while (concurrencyManager.tryAcquire(merchantId)) {
            Transaction queued = concurrencyManager.poll(merchantId);
            
            if (queued == null) {
                // Queue empty, release slot
                concurrencyManager.release(merchantId);
                break;
            }
            
            // Process queued transaction
            reconcileAsync(queued);
        }
    }
    
    /**
     * Reconciles transaction asynchronously.
     * 
     * Runs in virtual thread for non-blocking I/O.
     * 
     * @param transaction The transaction to reconcile
     */
    private void reconcileAsync(Transaction transaction) {
        UUID merchantId = transaction.getMerchantId();
        
        // Submit to virtual thread executor
        Thread.startVirtualThread(() -> {
            try {
                logger.debug("Starting reconciliation: transaction_id={}, merchant_id={}",
                    transaction.getId(), merchantId);
                
                boolean reconciled = reconciliationService.attemptReconciliation(transaction);
                
                if (reconciled) {
                    logger.info("Reconciliation completed: transaction_id={}, merchant_id={}",
                        transaction.getId(), merchantId);
                } else {
                    logger.warn("Reconciliation needs retry: transaction_id={}, merchant_id={}",
                        transaction.getId(), merchantId);
                }
                
            } catch (Exception e) {
                logger.error("Error reconciling transaction: transaction_id={}, merchant_id={}, error={}",
                    transaction.getId(), merchantId, e.getMessage(), e);
                
            } finally {
                // Always release slot
                concurrencyManager.release(merchantId);
                
                // Check if more queued transactions can be processed
                processQueuedTransactions(merchantId);
            }
        });
    }
}
