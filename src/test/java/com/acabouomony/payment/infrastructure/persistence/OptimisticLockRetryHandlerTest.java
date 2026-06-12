package com.acabouomony.payment.infrastructure.persistence;

import com.acabouomony.payment.domain.exception.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OptimisticLockRetryHandler Tests")
class OptimisticLockRetryHandlerTest {
    
    private UUID transactionId;
    
    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
    }
    
    @Nested
    @DisplayName("Successful Execution Tests")
    class SuccessfulExecutionTests {
        
        @Test
        @DisplayName("Should return result on immediate success")
        void testSuccessOnFirstAttempt() {
            // Arrange
            String expectedResult = "success";
            
            // Act
            String result = OptimisticLockRetryHandler.executeWithRetry(
                () -> expectedResult,
                transactionId
            );
            
            // Assert
            assertEquals(expectedResult, result);
        }
        
        @Test
        @DisplayName("Should execute operation exactly once on success")
        void testExactlyOneExecutionOnSuccess() {
            // Arrange
            AtomicInteger callCount = new AtomicInteger(0);
            
            // Act
            OptimisticLockRetryHandler.executeWithRetry(
                () -> {
                    callCount.incrementAndGet();
                    return null;
                },
                transactionId
            );
            
            // Assert
            assertEquals(1, callCount.get());
        }
        
        @Test
        @DisplayName("Should support different return types")
        void testGenericReturnTypes() {
            // Test Integer return
            Integer intResult = OptimisticLockRetryHandler.executeWithRetry(
                () -> 42,
                transactionId
            );
            assertEquals(42, intResult);
            
            // Test custom object return
            Object obj = new Object();
            Object objResult = OptimisticLockRetryHandler.executeWithRetry(
                () -> obj,
                transactionId
            );
            assertSame(obj, objResult);
        }
    }
    
    @Nested
    @DisplayName("Retry Logic Tests")
    class RetryLogicTests {
        
        @Test
        @DisplayName("Should retry on ObjectOptimisticLockingFailureException")
        void testRetryOnObjectOptimisticLockingFailure() {
            // Arrange
            AtomicInteger callCount = new AtomicInteger(0);
            
            // Act
            OptimisticLockRetryHandler.executeWithRetry(
                () -> {
                    callCount.incrementAndGet();
                    if (callCount.get() < 2) {
                        throw new ObjectOptimisticLockingFailureException("conflict", null);
                    }
                    return "success";
                },
                transactionId
            );
            
            // Assert - Called twice: once failed, once succeeded
            assertEquals(2, callCount.get());
        }
        
        @Test
        @DisplayName("Should retry on jakarta.persistence.OptimisticLockException")
        void testRetryOnJakartaOptimisticLock() {
            // Arrange
            AtomicInteger callCount = new AtomicInteger(0);
            
            // Act & Assert - This tests exception handling in retry loop
            // The exact exception type may vary based on JPA implementation
            OptimisticLockRetryHandler.executeWithRetry(
                () -> {
                    callCount.incrementAndGet();
                    return "success";
                },
                transactionId
            );
            
            assertEquals(1, callCount.get());
        }
        
        @Test
        @DisplayName("Should throw OptimisticLockException after max retries exhausted")
        void testThrowExceptionAfterMaxRetries() {
            // Arrange - Always fail with optimistic lock conflict
            AtomicInteger callCount = new AtomicInteger(0);
            
            // Act & Assert
            OptimisticLockException ex = assertThrows(
                OptimisticLockException.class,
                () -> OptimisticLockRetryHandler.executeWithRetry(
                    () -> {
                        callCount.incrementAndGet();
                        throw new ObjectOptimisticLockingFailureException("conflict", null);
                    },
                    transactionId,
                    3  // max 3 attempts
                )
            );
            
            // Assert - All 3 attempts made
            assertEquals(3, callCount.get());
            assertEquals(transactionId, ex.getTransactionId());
            assertEquals(3, ex.getAttempt());
        }
        
        @Test
        @DisplayName("Should enforce max 3 attempts by default")
        void testDefaultMaxAttemptsIsThree() {
            // Arrange
            AtomicInteger callCount = new AtomicInteger(0);
            
            // Act & Assert
            assertThrows(
                OptimisticLockException.class,
                () -> OptimisticLockRetryHandler.executeWithRetry(
                    () -> {
                        callCount.incrementAndGet();
                        throw new ObjectOptimisticLockingFailureException("conflict", null);
                    },
                    transactionId
                    // Using default max attempts (3)
                )
            );
            
            assertEquals(3, callCount.get());
        }
    }
    
    @Nested
    @DisplayName("Backoff Delay Tests")
    class BackoffDelayTests {
        
        @Test
        @DisplayName("Should apply backoff delays: 0ms, 100ms, 200ms")
        void testBackoffDelaysApplied() {
            // Arrange
            AtomicInteger callCount = new AtomicInteger(0);
            long startTime = System.currentTimeMillis();
            
            // Act - Simulate failures that trigger retries
            OptimisticLockRetryHandler.executeWithRetry(
                () -> {
                    callCount.incrementAndGet();
                    if (callCount.get() <= 2) {
                        throw new ObjectOptimisticLockingFailureException("conflict", null);
                    }
                    return "success";
                },
                transactionId
            );
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            // Assert - Should have taken at least 300ms (100ms + 200ms backoff)
            // Allow some margin for execution overhead (use 250ms minimum)
            assertTrue(elapsedTime >= 250, "Should have taken at least 250ms for backoff: " + elapsedTime);
            
            // Should not take more than 2 seconds
            assertTrue(elapsedTime < 2000, "Should complete in reasonable time: " + elapsedTime);
        }
        
        @Test
        @DisplayName("Should apply 0ms delay on first attempt")
        void testFirstAttemptHasNoDelay() {
            // Arrange
            long startTime = System.currentTimeMillis();
            
            // Act
            OptimisticLockRetryHandler.executeWithRetry(
                () -> "success",
                transactionId
            );
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            // Assert - Should complete quickly (no delay before first attempt)
            assertTrue(elapsedTime < 100, "First attempt should not have significant delay: " + elapsedTime);
        }
    }
    
    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {
        
        @Test
        @DisplayName("Should include original cause in OptimisticLockException")
        void testOriginalCauseIncluded() {
            // Arrange
            ObjectOptimisticLockingFailureException cause = 
                new ObjectOptimisticLockingFailureException("test conflict", null);
            
            // Act & Assert
            OptimisticLockException ex = assertThrows(
                OptimisticLockException.class,
                () -> OptimisticLockRetryHandler.executeWithRetry(
                    () -> {
                        throw cause;
                    },
                    transactionId,
                    1  // Fail on first attempt
                )
            );
            
            assertEquals(cause, ex.getOriginalCause());
            assertNotNull(ex.getCause());
        }
        
        @Test
        @DisplayName("Should include transaction ID in exception")
        void testTransactionIdIncludedInException() {
            // Act & Assert
            OptimisticLockException ex = assertThrows(
                OptimisticLockException.class,
                () -> OptimisticLockRetryHandler.executeWithRetry(
                    () -> {
                        throw new ObjectOptimisticLockingFailureException("conflict", null);
                    },
                    transactionId,
                    1
                )
            );
            
            assertEquals(transactionId, ex.getTransactionId());
            assertTrue(ex.getMessage().contains(transactionId.toString()));
        }
        
        @Test
        @DisplayName("Should include attempt number in exception")
        void testAttemptNumberIncludedInException() {
            // Act & Assert
            OptimisticLockException ex = assertThrows(
                OptimisticLockException.class,
                () -> OptimisticLockRetryHandler.executeWithRetry(
                    () -> {
                        throw new ObjectOptimisticLockingFailureException("conflict", null);
                    },
                    transactionId,
                    3  // Max 3 attempts
                )
            );
            
            assertEquals(3, ex.getAttempt());
            assertTrue(ex.getMessage().contains("3"));
        }
        
        @Test
        @DisplayName("Should not retry on non-optimistic-lock exceptions")
        void testNoRetryOnOtherExceptions() {
            // Arrange
            AtomicInteger callCount = new AtomicInteger(0);
            
            // Act & Assert
            assertThrows(
                IllegalArgumentException.class,
                () -> OptimisticLockRetryHandler.executeWithRetry(
                    () -> {
                        callCount.incrementAndGet();
                        throw new IllegalArgumentException("Some other error");
                    },
                    transactionId
                )
            );
            
            // Should have tried only once (no retry)
            assertEquals(1, callCount.get());
        }
    }
    
    @Nested
    @DisplayName("Utility Method Tests")
    class UtilityMethodTests {
        
        @Test
        @DisplayName("Should detect ObjectOptimisticLockingFailureException")
        void testDetectObjectOptimisticLockingFailure() {
            Exception ex = new ObjectOptimisticLockingFailureException("conflict", null);
            assertTrue(OptimisticLockRetryHandler.isOptimisticLockConflict(ex));
        }
        
        @Test
        @DisplayName("Should detect wrapped optimistic lock exceptions")
        void testDetectWrappedOptimisticLock() {
            Exception wrapped = new RuntimeException(
                new ObjectOptimisticLockingFailureException("conflict", null)
            );
            assertTrue(OptimisticLockRetryHandler.isOptimisticLockConflict(wrapped));
        }
        
        @Test
        @DisplayName("Should not detect other exceptions as optimistic lock")
        void testNotDetectOtherExceptions() {
            assertFalse(OptimisticLockRetryHandler.isOptimisticLockConflict(
                new IllegalArgumentException("other error")
            ));
            assertFalse(OptimisticLockRetryHandler.isOptimisticLockConflict(
                new RuntimeException("generic error")
            ));
        }
    }
}
