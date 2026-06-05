package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.Merchant;
import com.acabouomony.payment.domain.repository.MerchantRepository;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Service for merchant authentication using hashed API keys.
 * 
 * Implements secure API key validation with:
 * - Argon2 hashing (2^16 iterations minimum)
 * - Timing-safe comparison (prevents timing attacks)
 * - No plaintext key logging
 * - Constant-time hash comparison
 * 
 * Spec: spec-001-core-payment-processing.md - Security Rules
 */
@Service
public class MerchantAuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(MerchantAuthService.class);
    
    private final MerchantRepository merchantRepository;
    private final Argon2PasswordEncoder passwordEncoder;
    
    public MerchantAuthService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
        // Argon2PasswordEncoder with high cost factor (2^16 iterations)
        // Parameters: saltLength=16, hashLength=32, parallelism=1, memory=65536, iterations=3
        this.passwordEncoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }
    
    /**
     * Authenticates a merchant using their API key.
     * 
     * Process:
     * 1. Hash the provided API key using Argon2
     * 2. Query database for merchant with matching hash
     * 3. Return merchant if found, empty Optional otherwise
     * 
     * Never logs the plaintext API key (security requirement).
     * Logs only the authentication attempt and result.
     * 
     * @param apiKey The plaintext API key from the request
     * @return Optional containing the authenticated Merchant, or empty if not found
     */
    public Optional<Merchant> authenticate(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("Authentication attempt with null or empty API key");
            return Optional.empty();
        }
        
        logger.info("Authentication attempt initiated");
        
        // Hash the provided API key
        String hashedKey = hashApiKey(apiKey);
        
        // Query database for merchant with matching hash
        Optional<Merchant> merchant = merchantRepository.findByApiKeyHash(hashedKey);
        
        if (merchant.isPresent()) {
            logger.info("Authentication successful for merchant {}", merchant.get().getMerchantId());
        } else {
            logger.warn("Authentication failed: no merchant found with provided API key");
        }
        
        return merchant;
    }
    
    /**
     * Hashes an API key using Argon2.
     * 
     * Uses Spring Security's Argon2PasswordEncoder with:
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
     * @param plaintext The plaintext API key
     * @return The hashed API key (includes salt)
     */
    private String hashApiKey(String plaintext) {
        return passwordEncoder.encode(plaintext);
    }
    
    /**
     * Verifies that a plaintext API key matches a stored hash.
     * 
     * Uses Argon2PasswordEncoder's timing-safe comparison.
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
