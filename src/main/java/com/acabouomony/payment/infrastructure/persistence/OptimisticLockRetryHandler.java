package com.acabouomony.payment.infrastructure.persistence;

import com.acabouomony.payment.domain.exception.OptimisticLockException;
import jakarta.persistence.OptimisticLockException as JpaOptimisticLock;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Utility class for handling optimistic lock retry logic.
 * 
 * Provides retry capabilities with exponential backoff when optimistic
 * lock conflicts occur. Max 3 attempts with delays: immediate, 100ms, 200ms.
 */
public class OptimisticLockRetryHandler {
    
    private static final int MAX_ATTEMPTS = 3;
    private static final int[] BACKOFF_MS = {0, 100, 200};
    
    private OptimisticLockRetryHandler() {
        // Utility class, no instantiation
    }
    
    /**
     * Executes an operation with automatic retry on optimistic lock conflicts.
     * 
     * Retries up to 3 times with exponential backoff:
     * - Attempt 1: Immediate
     * - Attempt 2: After 100ms delay
     * - Attempt 3: After 200ms delay
     * 
     * @param <T> The return type of the operation
     * @param operation The supplier providing the operation to execute
     * @param transactionId Transaction ID for error reporting
     * @return The result of the operation on success
     * @throws OptimisticLockException if all retries are exhausted
     */
    public static <T> T executeWithRetry(Supplier<T> operation, UUID transactionId) {
        return executeWithRetry(operation, transactionId, MAX_ATTEMPTS);
    }
    
    /**
     * Executes an operation with automatic retry on optimistic lock conflicts.
     * 
     * @param <T> The return type of the operation
     * @param operation The supplier providing the operation to execute
     * @param transactionId Transaction ID for error reporting
     * @param maxAttempts Maximum number of retry attempts (typically 3)
     * @return The result of the operation on success
     * @throws OptimisticLockException if all retries are exhausted
     */
    public static <T> T executeWithRetry(Supplier<T> operation, UUID transactionId, int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Apply backoff delay before execution (except first attempt)
                if (attempt > 1) {
                    int delayMs = BACKOFF_MS[attempt - 1];
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new OptimisticLockException(transactionId, attempt, e);
                    }
                }
                
                return operation.get();
                
            } catch (ObjectOptimisticLockingFailureException | JpaOptimisticLock e) {
                // Optimistic lock conflict detected
                if (attempt >= maxAttempts) {
                    // All retries exhausted
                    throw new OptimisticLockException(transactionId, attempt, e);
                }
                // Will retry on next iteration
            }
        }
        
        // Should not reach here, but throw just in case
        throw new OptimisticLockException(transactionId, maxAttempts, null);
    }
    
    /**
     * Checks if an exception is an optimistic lock conflict.
     * 
     * @param e The exception to check
     * @return true if the exception is an optimistic lock conflict
     */
    public static boolean isOptimisticLockConflict(Exception e) {
        return e instanceof ObjectOptimisticLockingFailureException ||
               e instanceof JpaOptimisticLock ||
               (e.getCause() instanceof ObjectOptimisticLockingFailureException) ||
               (e.getCause() instanceof JpaOptimisticLock);
    }
}
