package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import com.acabouomony.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("RedisPaymentResponseCache")
class RedisPaymentResponseCacheTest {

    @Mock
    private RedisTemplate<String, PaymentResponseDTO> redisTemplate;

    @Mock
    private ValueOperations<String, PaymentResponseDTO> valueOperations;

    private RedisPaymentResponseCache cache;
    private UUID merchantId;
    private UUID idempotencyKey;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cache = new RedisPaymentResponseCache(redisTemplate);
        merchantId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Cache Storage and Retrieval")
    class CacheStorageAndRetrievalTests {

        @Test
        @DisplayName("Stores response with correct key and TTL")
        void testStoresWithCorrectKeyAndTTL() {
            PaymentResponseDTO response = createPaymentResponse();
            Duration ttl = Duration.ofHours(24);

            cache.cache(merchantId, idempotencyKey, response, ttl);

            String expectedKey = "core:idempotency:" + merchantId + ":" + idempotencyKey;
            verify(valueOperations).set(expectedKey, response, ttl);
        }

        @Test
        @DisplayName("Uses default TTL when null provided")
        void testUsesDefaultTTLWhenNull() {
            PaymentResponseDTO response = createPaymentResponse();

            cache.cache(merchantId, idempotencyKey, response, null);

            String expectedKey = "core:idempotency:" + merchantId + ":" + idempotencyKey;
            verify(valueOperations).set(eq(expectedKey), eq(response), any(Duration.class));
        }

        @Test
        @DisplayName("Retrieves value when key exists")
        void testRetrieveReturnsValueWhenKeyExists() {
            PaymentResponseDTO response = createPaymentResponse();
            String expectedKey = "core:idempotency:" + merchantId + ":" + idempotencyKey;
            when(valueOperations.get(expectedKey)).thenReturn(response);

            Optional<PaymentResponseDTO> result = cache.retrieve(merchantId, idempotencyKey);

            assertTrue(result.isPresent());
            assertEquals(response.getTransactionId(), result.get().getTransactionId());
        }

        @Test
        @DisplayName("Returns empty when key missing")
        void testRetrieveReturnsEmptyWhenKeyMissing() {
            String expectedKey = "core:idempotency:" + merchantId + ":" + idempotencyKey;
            when(valueOperations.get(expectedKey)).thenReturn(null);

            Optional<PaymentResponseDTO> result = cache.retrieve(merchantId, idempotencyKey);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Cache Invalidation")
    class CacheInvalidationTests {

        @Test
        @DisplayName("Deletes key on invalidate")
        void testInvalidateDeletesKey() {
            String expectedKey = "core:idempotency:" + merchantId + ":" + idempotencyKey;

            cache.invalidate(merchantId, idempotencyKey);

            verify(redisTemplate).delete(expectedKey);
        }
    }

    private PaymentResponseDTO createPaymentResponse() {
        return PaymentResponseDTO.builder()
            .transactionId(UUID.randomUUID())
            .status(PaymentStatus.COMPLETED)
            .amount(10000L)
            .currency("BRL")
            .maskedCard("411111XXXXXX1111")
            .message("Payment approved")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
