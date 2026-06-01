package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.AuditLog;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.OptimisticLockException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.AuditLogRepository;
import com.acabouomony.payment.infrastructure.persistence.OptimisticLockRetryHandler;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

/**
 * TransactionVersionService
 * 
 * Manages state transitions for transactions with optimistic locking and retry logic.
 * 
 * Responsibilities:
 * 1. Load transaction by ID
 * 2. Validate state transition using PaymentStateMachine
 * 3. Increment version number
 * 4. Persist transaction with version check
 * 5. Create audit log entry atomically
 * 6. Retry on optimistic lock conflicts (max 3 attempts)
 * 7. Log monitoring data
 * 
 * All database operations within a single @Transactional boundary
 * to ensure atomicity of transaction and audit log updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionVersionService {
    
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PaymentStateMachine paymentStateMachine;
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    /**
     * Updates transaction state with optimistic locking and retry logic.
     * 
     * Process:
     * 1. Load transaction from database
     * 2. Validate transition using state machine
     * 3. Increment version
     * 4. Persist transaction
     * 5. Create audit log atomically
     * 6. Retry on version conflicts (max 3 attempts with backoff)
     * 
     * @param transactionId The transaction ID to update
     * @param newStatus The desired new status
     * @param actor The actor performing the transition (e.g., "system", "reconciliation")
     * @throws OptimisticLockException if version conflicts after 3 retries
     */
    public void updateTransactionState(UUID transactionId, PaymentStatus newStatus, String actor) {
        OptimisticLockRetryHandler.executeWithRetry(
            () -> performStateTransition(transactionId, newStatus, actor),
            transactionId,
            MAX_RETRY_ATTEMPTS
        );
    }
    
    /**
     * Performs the actual state transition with database persistence.
     * 
     * This method is called within the retry loop and wrapped in a transaction.
     * Any OptimisticLockException thrown by Hibernate will be caught by the retry handler.
     * 
     * @param transactionId The transaction ID
     * @param newStatus The new status
     * @param actor The actor performing the transition
     * @return null (used for Supplier<Void> compatibility)
     */
    @Transactional
    public Void performStateTransition(UUID transactionId, PaymentStatus newStatus, String actor) {
        // Load current transaction
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        
        PaymentStatus currentStatus = PaymentStatus.valueOf(transaction.getStatus());
        
        // Validate transition
        paymentStateMachine.validateTransition(currentStatus, newStatus);
        
        // Increment version
        Integer currentVersion = transaction.getVersion();
        transaction.setVersion(currentVersion + 1);
        
        // Update status and timestamp
        transaction.setStatus(newStatus.toString());
        transaction.setUpdatedAt(Instant.now());
        
        // Persist transaction with version check (optimistic locking)
        // If version conflict, org.springframework.orm.ObjectOptimisticLockingFailureException is thrown
        transactionRepository.save(transaction);
        
        // Create audit log in same transaction (atomicity guaranteed)
        createAuditLog(transaction, currentStatus.toString(), newStatus.toString(), actor);
        
        log.info("Transaction {} transitioned from {} to {} (version {} -> {})", 
            transactionId, currentStatus, newStatus, currentVersion, currentVersion + 1);
        
        return null;
    }
    
    /**
     * Creates an audit log entry for the state transition.
     * 
     * Checksum is computed to detect tampering (integrity verification).
     * Audit log is persisted in the same transaction as the transaction update.
     * 
     * @param transaction The transaction being updated
     * @param oldStatus Previous status
     * @param newStatus New status
     * @param actor Who performed the transition
     */
    private void createAuditLog(Transaction transaction, String oldStatus, String newStatus, String actor) {
        String checksum = computeChecksum(transaction.getId().toString(), oldStatus, newStatus, actor);
        
        AuditLog auditLog = AuditLog.builder()
            .id(UUID.randomUUID())
            .transaction(transaction)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .actor(actor)
            .checksum(checksum)
            .createdAt(Instant.now())
            .build();
        
        auditLogRepository.save(auditLog);
    }
    
    /**
     * Computes SHA256 checksum for audit log integrity.
     * 
     * Prevents tampering by making it computationally difficult to forge
     * an audit entry with a matching checksum.
     * 
     * @param transactionId Transaction ID
     * @param oldStatus Old status
     * @param newStatus New status
     * @param actor Actor name
     * @return Hex-encoded SHA256 hash
     */
    private String computeChecksum(String transactionId, String oldStatus, String newStatus, String actor) {
        try {
            String input = transactionId + oldStatus + newStatus + actor;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Converts byte array to hex string.
     * 
     * @param bytes Byte array
     * @return Hex string representation
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
