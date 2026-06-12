package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "cache.backend", havingValue = "redis")
public class RedisPaymentResponseCache implements PaymentResponseCache {

    private static final Logger log = LoggerFactory.getLogger(RedisPaymentResponseCache.class);
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, PaymentResponseDTO> redisTemplate;

    public RedisPaymentResponseCache(RedisTemplate<String, PaymentResponseDTO> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void cache(UUID merchantId, UUID idempotencyKey, PaymentResponseDTO response, Duration ttl) {
        String key = buildKey(merchantId, idempotencyKey);
        redisTemplate.opsForValue().set(key, response, ttl != null ? ttl : TTL);
        log.debug("Cached payment response in Redis: key={}", key);
    }

    @Override
    public Optional<PaymentResponseDTO> retrieve(UUID merchantId, UUID idempotencyKey) {
        String key = buildKey(merchantId, idempotencyKey);
        PaymentResponseDTO response = redisTemplate.opsForValue().get(key);
        if (response != null) {
            log.debug("Redis cache hit: key={}", key);
            return Optional.of(response);
        }
        log.debug("Redis cache miss: key={}", key);
        return Optional.empty();
    }

    @Override
    public void invalidate(UUID merchantId, UUID idempotencyKey) {
        String key = buildKey(merchantId, idempotencyKey);
        redisTemplate.delete(key);
        log.debug("Invalidated Redis cache: key={}", key);
    }

    @Override
    public void clear() {
        log.warn("Redis clear() not implemented — use redis-cli FLUSHDB");
    }

    private String buildKey(UUID merchantId, UUID idempotencyKey) {
        return "core:idempotency:" + merchantId + ":" + idempotencyKey;
    }
}
