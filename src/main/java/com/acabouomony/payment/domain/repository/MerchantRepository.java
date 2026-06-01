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
 * Intentionally does NOT provide a method to find by plaintext API key
 * as this would be a security vulnerability.
 * 
 * API keys are hashed with Argon2, and authentication is performed
 * by hashing the provided key and comparing hashes.
 */
@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    
    /**
     * Finds a merchant by their merchant ID.
     * 
     * @param merchantId The unique merchant identifier
     * @return Optional containing the merchant if found
     */
    Optional<Merchant> findByMerchantId(UUID merchantId);
    
    /**
     * Finds a merchant by their API key hash.
     * 
     * This is used internally during authentication after hashing
     * the provided API key. We compare hashes, never plaintext keys.
     * 
     * @param apiKeyHash The hashed API key
     * @return Optional containing the merchant if found
     */
    Optional<Merchant> findByApiKeyHash(String apiKeyHash);
}
