package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.AuditLog;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for creating and persisting audit log entries.
 * 
 * Every state transition creates an audit entry with checksum.
 * Checksum prevents tampering and enables verification.
 * 
 * Spec: spec-001-core-payment-processing.md - Audit Log Persistence
 * Task: task-012-payment-orchestration.md
 * 
 * Audit Log Entry Creation:
 * - transactionId: UUID of transaction
 * - oldStatus: Previous payment status
 * - newStatus: New payment status
 * - actor: "system", "webhook", "reconciliation", etc.
 * - checksum: SHA256(transactionId + oldStatus + newStatus + actor)
 * - createdAt: Timestamp of creation
 */
@Service
public class AuditLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    
    private final AuditLogRepository auditLogRepository;
    
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    /**
     * Logs a state transition with checksum.
     * 
     * Creates audit entry and persists to database.
     * Checksum = SHA256(transactionId + oldStatus + newStatus + actor)
     * 
     * @param transaction The transaction entity
     * @param oldStatus The previous status
     * @param newStatus The new status
     * @param actor The actor performing transition ("system", "webhook", "reconciliation")
     */
    public void logStateTransition(Transaction transaction, PaymentStatus oldStatus, PaymentStatus newStatus, String actor) {
        logger.debug("Logging state transition: transaction_id={}, {} -> {}, actor={}",
            transaction.getId(), oldStatus, newStatus, actor);
        
        try {
            // Compute checksum
            String checksumInput = transaction.getId().toString() + oldStatus.name() + newStatus.name() + actor;
            String checksum = computeSHA256(checksumInput);
            
            // Create audit entry
            AuditLog entry = AuditLog.builder()
                .id(UUID.randomUUID())
                .transaction(transaction)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .actor(actor)
                .checksum(checksum)
                .createdAt(Instant.now())
                .build();
            
            // Persist to database
            auditLogRepository.save(entry);
            
            logger.info("Audit log created: transaction_id={}, {} -> {}, checksum={}",
                transaction.getId(), oldStatus, newStatus, checksum);
            
        } catch (Exception e) {
            logger.error("Error creating audit log: transaction_id={}, error={}", transaction.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create audit log", e);
        }
    }
    
    /**
     * Computes SHA256 checksum of input string.
     * 
     * @param input The input string
     * @return SHA256 hex string (64 characters)
     */
    private String computeSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
