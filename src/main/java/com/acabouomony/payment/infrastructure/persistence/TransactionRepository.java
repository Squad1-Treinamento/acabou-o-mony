package com.acabouomony.payment.infrastructure.persistence;

import com.acabouomony.payment.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
