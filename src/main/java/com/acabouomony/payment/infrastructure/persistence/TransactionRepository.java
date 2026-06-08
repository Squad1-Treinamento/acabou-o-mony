package com.acabouomony.payment.infrastructure.persistence;

import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Transaction entities.
 * 
 * Provides standard CRUD operations and persistence capabilities.
 * JPA/Hibernate automatically handles @Version annotation for optimistic locking.
 * 
 * Custom query methods:
 * - findByMerchantIdAndIdempotencyKey: Used for idempotency duplicate detection
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    /**
     * Finds existing transaction by merchant ID and idempotency key.
     * 
     * Used for idempotency coordination:
     * - Detects duplicate requests (same merchant + same idempotency_key)
     * - Validates payload hash to ensure identical retry
     * 
     * Spec: spec-001-core-payment-processing.md - Idempotency Rules
     * Task: task-007-payload-hashing.md
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     * @return Optional containing transaction if found, empty otherwise
     */
    Optional<Transaction> findByMerchantIdAndIdempotencyKey(UUID merchantId, UUID idempotencyKey);
    
    /**
     * Finds UNKNOWN transactions for reconciliation.
     * 
     * Used by reconciliation worker to query transactions needing status resolution.
     * Filters by:
     * - Status: UNKNOWN
     * - Created before threshold (e.g., 1 minute ago to avoid immediate reconciliation)
     * - Ordered by created_at ASC (oldest first)
     * 
     * Spec: spec-001-core-payment-processing.md - Reconciliation Rules
     * Task: task-014-reconciliation-worker.md
     * 
     * @param status The pNKNOWN)
     * @param createdBefore Timestamp thresholdayment status (U
     * @param pageable Pagination parameters
     * @return List of transactions needing reconciliation
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = :status " +
           "AND t.createdAt < :createdBefore " +
           "ORDER BY t.createdAt ASC")
    List<Transaction> findUnknownTransactionsForReconciliation(
        @Param("status") PaymentStatus status,
        @Param("createdBefore") Instant createdBefore,
        Pageable pageable
    );
    
    /**
     * Finds stale UNKNOWN transactions.
     * 
     * Used by stale transaction monitor to detect -001-core-payment-processing.md - Reconciliation Behavior & Termination
     * Tasexpected reconciliation UNKNOWN transactions
     * that have exceeded the window (5 minutes).
     * 
     * Spec: spece-handling.md
     * 
     * @param status The payment status (UNKNOWN)
     * k: task-013-unknown-stat@param staleThreshold Timestamp threshold (e.g., 5 minutes ago)
     * @return List of stale UNKNOWN transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = :status " +
           "AND t.createdAt < :staleThreshold")
    List<Transaction> findStaleUnknownTransactions(
        @Param("status") PaymentStatus status,
        @Param("staleThreshold") Instant staleThreshold
    );
}
