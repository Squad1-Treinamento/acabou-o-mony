package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.web.dto.PaymentResponseDTO;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for payment response caching.
 * 
 * Provides abstraction for response caching mechanism.
 * Implementations can use in-memory cache, Redis, or other backends.
 * 
 * Spec: spec-001-core-payment-processing.md - Idempotency Implementation Model
 * Task: task-010-duplicate-request-recovery.md
 * 
 * Cache Key Format: "payment_response:{merchant_id}:{idempotency_key}"
 * TTL: 24 hours (per spec)
 */
public interface PaymentResponseCache {
    
    /**
     * Caches a payment response.
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     * @param response The payment response to cache
     * @param ttl Time to live for cache entry
     */
    void cache(UUID merchantId, UUID idempotencyKey, PaymentResponseDTO response, Duration ttl);
    
    /**
     * Retrieves a cached payment response.
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     * @return Optional containing cached response if found and not expired
     */
    Optional<PaymentResponseDTO> retrieve(UUID merchantId, UUID idempotencyKey);
    
    /**
     * Invalidates a cached response.
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     */
    void invalidate(UUID merchantId, UUID idempotencyKey);
    
    /**
     * Clears all cached responses.
     * 
     * Used for testing and maintenance.
     */
    void clear();
}
