package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.config.UnknownStateProperties;
import com.acabouomony.payment.domain.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages concurrency control for reconciliation processing.
 * 
 * Enforces per-merchant limits to prevent single merchant from starving others:
 * - Maximum 2 concurrent reconciliation queries per merchant
 * - Maximum queue depth of 10 per merchant
 * - Drops requests and alerts when queue overflows
 * 
 * Spec: spec-001-core-payment-processing.md - Reconciliation & Concurrency Rules
 * Task: task-014-reconciliation-worker.md
 * 
 * Thread Safety:
 * - Uses ConcurrentHashMap for merchant tracking
 * - Uses AtomicInteger for concurrent count
 * - Uses ConcurrentLinkedQueue for queuing
 */
@Service
public class ReconciliationConcurrencyManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationConcurrencyManager.class);
    
    private final UnknownStateProperties properties;
    private final AlertService alertService;
    
    // Track concurrent reconciliation count per merchant
    private final Map<UUID, AtomicInteger> concurrentCountMap = new ConcurrentHashMap<>();
    
    // Queue pending reconciliation requests per merchant
    private final Map<UUID, Queue<Transaction>> reconciliationQueues = new ConcurrentHashMap<>();
    
    public ReconciliationConcurrencyManager(
            UnknownStateProperties properties,
            AlertService alertService) {
        this.properties = properties;
        this.alertService = alertService;
    }
    
    /**
     * Attempts to acquire reconciliation slot for merchant.
     * 
     * Returns true if reconciliation can proceed immediately.
     * Returns false if at capacity (caller should queue or drop).
     * 
     * @param merchantId The merchant ID
     * @return true if slot acquired, false if at capacity
     */
    public boolean tryAcquire(UUID merchantId) {
        AtomicInteger count = concurrentCountMap.computeIfAbsent(merchantId, k -> new AtomicInteger(0));
        
        int current = count.get();
        int maxConcurrent = properties.getMaxConcurrentPerMerchant();
        
        if (current >= maxConcurrent) {
            logger.debug("Reconciliation at capacity for merchant: merchant_id={}, current={}, max={}",
                merchantId, current, maxConcurrent);
            return false;
        }
        
        // Atomically increment if still below limit
        int newCount = count.incrementAndGet();
        
        if (newCount > maxConcurrent) {
            // Race condition: another thread incremented first
            count.decrementAndGet();
            return false;
        }
        
        logger.debug("Reconciliation slot acquired: merchant_id={}, concurrent_count={}",
            merchantId, newCount);
        return true;
    }
    
    /**
     * Releases reconciliation slot for merchant.
     * 
     * @param merchantId The merchant ID
     */
    public void release(UUID merchantId) {
        AtomicInteger count = concurrentCountMap.get(merchantId);
        
        if (count == null) {
            logger.warn("Attempted to release reconciliation slot for unknown merchant: merchant_id={}",
                merchantId);
            return;
        }
        
        int newCount = count.decrementAndGet();
        
        logger.debug("Reconciliation slot released: merchant_id={}, concurrent_count={}",
            merchantId, newCount);
        
        // Clean up if no longer in use
        if (newCount == 0) {
            concurrentCountMap.remove(merchantId);
        }
    }
    
    /**
     * Queues transaction for later reconciliation.
     * 
     * Returns true if successfully queued.
     * Returns false if queue overflow (drops request and alerts).
     * 
     * @param transaction The transaction to queue
     * @return true if queued, false if dropped
     */
    public boolean queue(Transaction transaction) {
        UUID merchantId = transaction.getMerchantId();
        Queue<Transaction> queue = reconciliationQueues.computeIfAbsent(merchantId, k -> new ConcurrentLinkedQueue<>());
        
        int maxQueueDepth = properties.getMaxQueueDepthPerMerchant();
        
        if (queue.size() >= maxQueueDepth) {
            logger.error("Reconciliation queue overflow, dropping request: " +
                    "merchant_id={}, queue_size={}, max_queue_depth={}, transaction_id={}",
                merchantId, queue.size(), maxQueueDepth, transaction.getId());
            
            alertService.alertReconciliationQueueOverflow(merchantId, queue.size());
            return false;
        }
        
        queue.offer(transaction);
        
        logger.debug("Transaction queued for reconciliation: merchant_id={}, transaction_id={}, queue_size={}",
            merchantId, transaction.getId(), queue.size());
        
        return true;
    }
    
    /**
     * Polls next transaction from merchant's reconciliation queue.
     * 
     * Returns null if queue is empty.
     * 
     * @param merchantId The merchant ID
     * @return Next transaction or null
     */
    public Transaction poll(UUID merchantId) {
        Queue<Transaction> queue = reconciliationQueues.get(merchantId);
        
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        
        Transaction transaction = queue.poll();
        
        if (transaction != null) {
            logger.debug("Transaction polled from reconciliation queue: merchant_id={}, transaction_id={}, remaining={}",
                merchantId, transaction.getId(), queue.size());
        }
        
        // Clean up empty queue
        if (queue.isEmpty()) {
            reconciliationQueues.remove(merchantId);
        }
        
        return transaction;
    }
    
    /**
     * Gets current concurrent reconciliation count for merchant.
     * 
     * @param merchantId The merchant ID
     * @return Current concurrent count
     */
    public int getConcurrentCount(UUID merchantId) {
        AtomicInteger count = concurrentCountMap.get(merchantId);
        return count == null ? 0 : count.get();
    }
    
    /**
     * Gets current queue size for merchant.
     * 
     * @param merchantId The merchant ID
     * @return Current queue size
     */
    public int getQueueSize(UUID merchantId) {
        Queue<Transaction> queue = reconciliationQueues.get(merchantId);
        return queue == null ? 0 : queue.size();
    }
}
