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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class TransactionVersionService {

    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PaymentStateMachine paymentStateMachine;
    private final OutboxEventService outboxEventService;
    private final TransactionVersionService self;

    public TransactionVersionService(
            TransactionRepository transactionRepository,
            AuditLogRepository auditLogRepository,
            PaymentStateMachine paymentStateMachine,
            OutboxEventService outboxEventService,
            @Lazy TransactionVersionService self) {
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.paymentStateMachine = paymentStateMachine;
        this.outboxEventService = outboxEventService;
        this.self = self;
    }

    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Updates transaction state with optimistic locking and retry logic.
     */
    public void updateTransactionState(UUID transactionId, PaymentStatus newStatus, String actor) {
        updateTransactionStateAndAcquirerRef(transactionId, newStatus, actor, null);
    }

    /**
     * Updates transaction state and acquirer reference, used after acquirer response.
     */
    public void updateTransactionStateAndAcquirerRef(UUID transactionId, PaymentStatus newStatus, String actor, String acquirerReference) {
        OptimisticLockRetryHandler.executeWithRetry(
                // CRITICAL FIX: Call the self-injected proxy to ensure a new transaction is started.
                () -> self.performStateTransition(transactionId, newStatus, actor, acquirerReference),
                transactionId,
                MAX_RETRY_ATTEMPTS
        );
    }

    /**
     * Performs the actual state transition within a new, independent transaction.
     * This prevents optimistic lock failures from marking the parent transaction for rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Void performStateTransition(UUID transactionId, PaymentStatus newStatus, String actor, String acquirerReference) {
        // Load current transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        PaymentStatus currentStatus = transaction.getStatus();
        Integer currentVersion = transaction.getVersion();

        // Validate transition
        paymentStateMachine.validateTransition(currentStatus, newStatus);

        // Update status and timestamp
        transaction.setStatus(newStatus);
        transaction.setUpdatedAt(Instant.now());

        // Optionally update the acquirer reference
        if (acquirerReference != null) {
            transaction.setAcquirerReference(acquirerReference);
        }

        // Persist transaction with version check
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Create audit log in same transaction
        createAuditLog(savedTransaction, currentStatus, newStatus, actor);

        // Create outbox event for terminal states
        createOutboxEventForStatus(savedTransaction, newStatus);

        log.info("Transaction {} transitioned from {} to {} (version {} -> {})",
                transactionId, currentStatus, newStatus, currentVersion, savedTransaction.getVersion());

        return null;
    }

    private void createOutboxEventForStatus(Transaction transaction, PaymentStatus status) {
        switch (status) {
            case COMPLETED:
                outboxEventService.createPaymentCompletedEvent(transaction);
                break;
            case DECLINED:
                outboxEventService.createPaymentDeclinedEvent(transaction);
                break;
            case FAILED:
                outboxEventService.createPaymentFailedEvent(transaction);
                break;
            default:
                // No event for intermediate states
                break;
        }
    }

    /**
     * Creates an audit log entry for the state transition.
     */
    private void createAuditLog(Transaction transaction, PaymentStatus oldStatus, PaymentStatus newStatus, String actor) {
        String checksum = computeChecksum(transaction.getId().toString(), oldStatus.toString(), newStatus.toString(), actor);

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