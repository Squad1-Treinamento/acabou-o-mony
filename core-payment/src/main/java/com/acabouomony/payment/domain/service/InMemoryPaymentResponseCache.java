package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of payment response cache.
 * 
 * Uses ConcurrentHashMap with TTL tracking for thread-safe caching.
 * Suitable for single-instance deployments or fallback when Redis unavailable.
 * 
 * Spec: spec-001-core-payment-processing.md - Idempotency Implementation Model
 * Task: task-010-duplicate-request-recovery.md
 * 
 * Features:
 * - Thread-safe concurrent access
 * - TTL-based expiration
 * - Automatic cleanup of expired entries
 * - Merchant-scoped cache keys
 * - No external dependencies
 */
@Component
public class InMemoryPaymentResponseCache implements PaymentResponseCache {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryPaymentResponseCache.class);
    
    /**
     * Cache entry with TTL tracking.
     */
    private static class CacheEntry {
        private final PaymentResponseDTO response;
        private final Instant expiresAt;
        
        CacheEntry(PaymentResponseDTO response, Instant expiresAt) {
            this.response = response;
            this.expiresAt = expiresAt;
        }
        
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
    
    /**
     * Cache storage: merchant_id:idempotency_key -> CacheEntry
     */
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    /**
     * Caches a payment response with TTL.
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     * @param response The payment response to cache
     * @param ttl Time to live for cache entry
     */
    @Override
    public void cache(UUID merchantId, UUID idempotencyKey, PaymentResponseDTO response, Duration ttl) {
        String key = buildCacheKey(merchantId, idempotencyKey);
        Instant expiresAt = Instant.now().plus(ttl);
        
        cache.put(key, new CacheEntry(response, expiresAt));
        
        logger.debug("Cached payment response: merchant={}, idempotency_key={}, expires_at={}",
            merchantId, idempotencyKey, expiresAt);
    }
    
    /**
     * Retrieves a cached payment response.
     * 
     * Returns empty if:
     * - Cache miss (key not found)
     * - Cache hit but entry expired
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     * @return Optional containing cached response if found and not expired
     */
    @Override
    public Optional<PaymentResponseDTO> retrieve(UUID merchantId, UUID idempotencyKey) {
        String key = buildCacheKey(merchantId, idempotencyKey);
        
        CacheEntry entry = cache.get(key);
        
        if (entry == null) {
            logger.debug("Cache miss: merchant={}, idempotency_key={}", merchantId, idempotencyKey);
            return Optional.empty();
        }
        
        if (entry.isExpired()) {
            logger.debug("Cache expired: merchant={}, idempotency_key={}", merchantId, idempotencyKey);
            cache.remove(key);  // Clean up expired entry
            return Optional.empty();
        }
        
        logger.debug("Cache hit: merchant={}, idempotency_key={}", merchantId, idempotencyKey);
        return Optional.of(entry.response);
    }
    
    /**
     * Invalidates a cached response.
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     */
    @Override
    public void invalidate(UUID merchantId, UUID idempotencyKey) {
        String key = buildCacheKey(merchantId, idempotencyKey);
        cache.remove(key);
        
        logger.debug("Invalidated cache: merchant={}, idempotency_key={}", merchantId, idempotencyKey);
    }
    
    /**
     * Clears all cached responses.
     * 
     * Used for testing and maintenance.
     */
    @Override
    public void clear() {
        cache.clear();
        logger.debug("Cleared all cached responses");
    }
    
    /**
     * Builds cache key from merchant ID and idempotency key.
     * 
     * Format: "payment_response:{merchant_id}:{idempotency_key}"
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     * @return Cache key string
     */
    private String buildCacheKey(UUID merchantId, UUID idempotencyKey) {
        return "payment_response:" + merchantId + ":" + idempotencyKey;
    }
    
    /**
     * Returns current cache size (for testing/monitoring).
     * 
     * @return Number of entries in cache
     */
    public int size() {
        return cache.size();
    }
}
