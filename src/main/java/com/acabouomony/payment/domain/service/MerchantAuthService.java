package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.Merchant;
import com.acabouomony.payment.domain.repository.MerchantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

/**
 * Service for merchant authentication using hashed API keys.
 * 
 * Implements secure API key validation with:
 * - Argon2 hashing with secure parameters (memory=64MB, iterations=3, parallelism=1)
 * - Timing-safe comparison (prevents timing attacks)
 * - No plaintext key logging
 * - Constant-time hash comparison via PasswordEncoder.matches()
 * - Fetch-all-and-verify approach (necessary due to salted hashing)
 * 
 * Authentication Strategy:
 * 1. Fetch all merchants from database
 * 2. For each merchant, use timing-safe comparison to verify API key
 * 3. Return first merchant that matches, or empty Optional
 * 
 * Why not "hash and query"? Argon2 generates different hashes for the same input
 * (due to random salt), making direct database queries by hash impossible.
 * 
 * Spec: spec-001-core-payment-processing.md - Security Rules
 */
@Service
public class MerchantAuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(MerchantAuthService.class);
    
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public MerchantAuthService(MerchantRepository merchantRepository, PasswordEncoder passwordEncoder) {
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
    }
    /**
     * Authenticates a merchant using their API key.
     * 
     * Process:
     * 1. Query all merchants from database (simple approach)
     * 2. For each merchant, verify the provided API key against stored hash using Argon2
     * 3. Return merchant if verification succeeds, empty Optional otherwise
     * 
     * Note: This approach queries all merchants and verifies each one. While not optimal
     * for large merchant counts, it's the correct approach for Argon2 which generates
     * different hashes for the same input (due to random salt). The verification method
     * extracts the salt from the stored hash and re-hashes the input for comparison.
     * 
     * Never logs the plaintext API key (security requirement).
     * Logs only the authentication attempt and result.
     * 
     * @param apiKey The plaintext API key from the request
     * @return Optional containing the authenticated Merchant, or empty if not found
     */
    public Optional<Merchant> authenticate(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Optional.empty();
        }
        
        logger.debug("Initiating API key authentication.");
        // Query all merchants and verify each one
        // This is necessary because Argon2 generates different hashes for the same input
        // (due to random salt), so we cannot query by hash directly
        List<Merchant> allMerchants = merchantRepository.findAll();

        for (Merchant merchant : allMerchants) {
            if (passwordEncoder.matches(apiKey, merchant.getApiKeyHash())) {
                logger.info("Authentication successful for merchant_id={}", merchant.getMerchantId());
                return Optional.of(merchant);
            }
        }
        logger.warn("Authentication failed. No merchant found for the provided API key.");
                return Optional.empty();
    }
    
    /**
     * Hashes an API key using Argon2.
     * 
     * Uses Spring Security's PasswordEncoder with:
     * - Salt length: 16 bytes
     * - Hash length: 32 bytes
     * - Parallelism: 1
     * - Memory: 65536 KB (64 MB)
     * - Iterations: 3
     * 
     * This produces different hashes for the same input (due to random salt),
     * so comparison is done using the encoder's matches() method which
     * extracts the salt from the stored hash and re-hashes the input.
     * 
     * This method is public to allow API key generation during merchant registration.
     * 
     * @param plaintext The plaintext API key
     * @return The hashed API key (includes salt)
     */
    public String hashApiKey(String plaintext) {
        return passwordEncoder.encode(plaintext);
    }
    
    /**
     * Verifies that a plaintext API key matches a stored hash.
     * 
     * Uses PasswordEncoder's timing-safe comparison.
     * The encoder extracts the salt from the stored hash and re-hashes
     * the provided plaintext, then compares using constant-time logic.
     * 
     * This prevents timing attacks where an attacker could measure
     * comparison duration to infer correct characters.
     * 
     * @param plaintext The plaintext API key from the request
     * @param hash The stored hashed API key
     * @return true if the plaintext matches the hash, false otherwise
     */
    public boolean verifyApiKey(String plaintext, String hash) {
        return passwordEncoder.matches(plaintext, hash);
    }
}

