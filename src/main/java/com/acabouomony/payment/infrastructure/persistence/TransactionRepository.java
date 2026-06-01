package com.acabouomony.payment.infrastructure.persistence;

import com.acabouomony.payment.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for Transaction entities.
 * 
 * Provides standard CRUD operations and persistence capabilities.
 * JPA/Hibernate automatically handles @Version annotation for optimistic locking.
 * 
 * No custom methods needed - using built-in save() for state transitions.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    // Standard JPA operations provided by framework:
    // - save(Transaction) - Used for both inserts and updates with version check
    // - findById(UUID) - Used to load transaction for state transitions
    // - All other CRUD operations as needed
}
