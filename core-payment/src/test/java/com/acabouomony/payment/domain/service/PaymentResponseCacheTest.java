package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import com.acabouomony.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PaymentResponseCache.
 * 
 * Tests verify:
 * - Cache stores and retrieves responses
 * - TTL expiration works correctly
 * - Cache invalidation works
 * - Concurrent access is thread-safe
 * - Cache key format is correct
 * 
 * Spec: spec-001-core-payment-processing.md - Idempotency Implementation Model
 * Task: task-010-duplicate-request-recovery.md
 */
@DisplayName("PaymentResponseCache")
class PaymentResponseCacheTest {
    
    private InMemoryPaymentResponseCache cache;
    private UUID merchantId;
    private UUID idempotencyKey;
    
    @BeforeEach
    void setUp() {
        cache = new InMemoryPaymentResponseCache();
        merchantId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();
    }
    
    @Nested
    @DisplayName("Cache Storage and Retrieval")
    class CacheStorageAndRetrievalTests {
        
        @Test
        @DisplayName("Stores and retrieves response")
        void testStoresAndRetrievesResponse() {
            // Arrange
            PaymentResponseDTO response = createPaymentResponse();
            Duration ttl = Duration.ofHours(24);
            
            // Act
            cache.cache(merchantId, idempotencyKey, response, ttl);
            Optional<PaymentResponseDTO> retrieved = cache.retrieve(merchantId, idempotencyKey);
            
            // Assert
            assertTrue(retrieved.isPresent());
            assertEquals(response.getTransactionId(), retrieved.get().getTransactionId());
            assertEquals(response.getStatus(), retrieved.get().getStatus());
        }
        
        @Test
        @DisplayName("Returns empty for missing key")
        void testReturnsEmptyForMissingKey() {
            // Act
            Optional<PaymentResponseDTO> retrieved = cache.retrieve(merchantId, idempotencyKey);
            
            // Assert
            assertTrue(retrieved.isEmpty());
        }
        
        @Test
        @DisplayName("Returns empty for different merchant")
        void testReturnsEmptyForDifferentMerchant() {
            // Arrange
            PaymentResponseDTO response = createPaymentResponse();
            cache.cache(merchantId, idempotencyKey, response, Duration.ofHours(24));
            
            UUID differentMerchant = UUID.randomUUID();
            
            // Act
            Optional<PaymentResponseDTO> retrieved = cache.retrieve(differentMerchant, idempotencyKey);
            
            // Assert
            assertTrue(retrieved.isEmpty());
        }
        
        @Test
        @DisplayName("Returns empty for different idempotency key")
        void testReturnsEmptyForDifferentIdempotencyKey() {
            // Arrange
            PaymentResponseDTO response = createPaymentResponse();
            cache.cache(merchantId, idempotencyKey, response, Duration.ofHours(24));
            
            UUID differentKey = UUID.randomUUID();
            
            // Act
            Optional<PaymentResponseDTO> retrieved = cache.retrieve(merchantId, differentKey);
            
            // Assert
            assertTrue(retrieved.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Cache Invalidation")
    class CacheInvalidationTests {
        
        @Test
        @DisplayName("Invalidates cached entry")
        void testInvalidatesCachedEntry() {
            // Arrange
            PaymentResponseDTO response = createPaymentResponse();
            cache.cache(merchantId, idempotencyKey, response, Duration.ofHours(24));
            
            // Verify cached
            assertTrue(cache.retrieve(merchantId, idempotencyKey).isPresent());
            
            // Act
            cache.invalidate(merchantId, idempotencyKey);
            
            // Assert
            assertTrue(cache.retrieve(merchantId, idempotencyKey).isEmpty());
        }
        
        @Test
        @DisplayName("Clears all cached entries")
        void testClearsAllCachedEntries() {
            // Arrange
            PaymentResponseDTO response1 = createPaymentResponse();
            PaymentResponseDTO response2 = createPaymentResponse();
            
            UUID key1 = UUID.randomUUID();
            UUID key2 = UUID.randomUUID();
            
            cache.cache(merchantId, key1, response1, Duration.ofHours(24));
            cache.cache(merchantId, key2, response2, Duration.ofHours(24));
            
            // Verify cached
            assertEquals(2, cache.size());
            
            // Act
            cache.clear();
            
            // Assert
            assertEquals(0, cache.size());
            assertTrue(cache.retrieve(merchantId, key1).isEmpty());
            assertTrue(cache.retrieve(merchantId, key2).isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Cache TTL Expiration")
    class CacheTTLExpirationTests {
        
        @Test
        @DisplayName("Returns empty for expired entry")
        void testReturnsEmptyForExpiredEntry() throws InterruptedException {
            // Arrange
            PaymentResponseDTO response = createPaymentResponse();
            Duration shortTtl = Duration.ofMillis(100);  // 100ms TTL
            
            cache.cache(merchantId, idempotencyKey, response, shortTtl);
            
            // Verify cached initially
            assertTrue(cache.retrieve(merchantId, idempotencyKey).isPresent());
            
            // Act: Wait for TTL to expire
            Thread.sleep(150);
            
            // Assert
            assertTrue(cache.retrieve(merchantId, idempotencyKey).isEmpty());
        }
        
        @Test
        @DisplayName("Respects different TTL values")
        void testRespectsDifferentTTLValues() throws InterruptedException {
            // Arrange
            PaymentResponseDTO response1 = createPaymentResponse();
            PaymentResponseDTO response2 = createPaymentResponse();
            
            UUID key1 = UUID.randomUUID();
            UUID key2 = UUID.randomUUID();
            
            Duration shortTtl = Duration.ofMillis(100);
            Duration longTtl = Duration.ofMillis(500);
            
            cache.cache(merchantId, key1, response1, shortTtl);
            cache.cache(merchantId, key2, response2, longTtl);
            
            // Act: Wait for short TTL to expire
            Thread.sleep(150);
            
            // Assert
            assertTrue(cache.retrieve(merchantId, key1).isEmpty(), "Short TTL should expire");
            assertTrue(cache.retrieve(merchantId, key2).isPresent(), "Long TTL should still be valid");
        }
    }
    
    @Nested
    @DisplayName("Cache Size and Cleanup")
    class CacheSizeAndCleanupTests {
        
        @Test
        @DisplayName("Tracks cache size correctly")
        void testTracksCacheSizeCorrectly() {
            // Arrange
            PaymentResponseDTO response1 = createPaymentResponse();
            PaymentResponseDTO response2 = createPaymentResponse();
            
            UUID key1 = UUID.randomUUID();
            UUID key2 = UUID.randomUUID();
            
            // Act
            cache.cache(merchantId, key1, response1, Duration.ofHours(24));
            assertEquals(1, cache.size());
            
            cache.cache(merchantId, key2, response2, Duration.ofHours(24));
            assertEquals(2, cache.size());
            
            cache.invalidate(merchantId, key1);
            assertEquals(1, cache.size());
        }
        
        @Test
        @DisplayName("Cleans up expired entries on retrieval")
        void testCleansUpExpiredEntriesOnRetrieval() throws InterruptedException {
            // Arrange
            PaymentResponseDTO response = createPaymentResponse();
            Duration shortTtl = Duration.ofMillis(100);
            
            cache.cache(merchantId, idempotencyKey, response, shortTtl);
            assertEquals(1, cache.size());
            
            // Act: Wait for TTL to expire and retrieve
            Thread.sleep(150);
            cache.retrieve(merchantId, idempotencyKey);
            
            // Assert: Expired entry should be cleaned up
            assertEquals(0, cache.size());
        }
    }
    
    @Nested
    @DisplayName("Concurrent Access")
    class ConcurrentAccessTests {
        
        @Test
        @DisplayName("Handles concurrent cache operations")
        void testHandlesConcurrentCacheOperations() throws InterruptedException {
            // Arrange
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            
            // Act: Multiple threads caching responses
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    UUID key = UUID.randomUUID();
                    PaymentResponseDTO response = createPaymentResponse();
                    cache.cache(merchantId, key, response, Duration.ofHours(24));
                });
                threads[i].start();
            }
            
            // Wait for all threads
            for (Thread thread : threads) {
                thread.join();
            }
            
            // Assert: All entries should be cached
            assertEquals(threadCount, cache.size());
        }
    }
    
    // Helper method
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
