package com.acabouomony.payment.domain.repository;

import com.acabouomony.payment.domain.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Merchant entities.
 * 
 * Provides persistence operations for merchant records.
 * 
 * IMPORTANT: Intentionally does NOT provide a findByApiKeyHash() method.
 * 
 * Why? Argon2 (and other salted hashing algorithms) generate different hashes
 * for the same input every time due to random salt. This makes direct database
 * queries by hash impossible.
 * 
 * Authentication Strategy:
 * 1. Use findAll() to get all merchants
 * 2. Iterate through merchants and use Argon2PasswordEncoder.matches() for each
 * 3. matches() extracts the salt from stored hash and re-hashes the input
 * 4. Return first merchant that matches
 * 
 * This is the correct and secure approach for salted hashing.
 * See MerchantAuthService.authenticate() for implementation.
 */
@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    
    /**
     * Finds a merchant by their public-facing business identifier (merchant_id).
     * This is used to look up merchants based on the ID stored in transactions.
     */
    Optional<Merchant> findByMerchantId(UUID merchantId);
}

