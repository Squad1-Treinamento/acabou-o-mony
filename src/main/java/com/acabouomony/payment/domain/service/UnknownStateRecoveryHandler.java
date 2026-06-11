package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for handling recovery of UNKNOWN state transactions.
 * 
 * Handles:
 * - Detection of UNKNOWN state transactions
 * - Building appropriate responses for UNKNOWN states
 * - Scheduling reconciliation
 * - Determining if recovery is possible
 * 
 * Spec: spec-001-core-payment-processing.md - Unknown State Handling
 * Task: task-010-duplicate-request-recovery.md
 * 
 * UNKNOWN State Rules:
 * - Occurs when Mercado Pago timeout or network ambiguity
 * - Requires reconciliation to resolve to terminal state
 * - Client receives 202 Accepted (outcome uncertain)
 * - Idempotency protection remains active
 */
@Service
public class UnknownStateRecoveryHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(UnknownStateRecoveryHandler.class);
    
    /**
     * Determines if a transaction can be recovered from UNKNOWN state.
     * 
     * A transaction can be recovered if:
     * - Status is UNKNOWN
     * - Reconciliation has not been attempted yet (or can be retried)
     * 
     * @param transaction The transaction to check
     * @return true if recovery is possible, false otherwise
     */
    public boolean canRecoverFromUnknown(Transaction transaction) {
        if (transaction == null) {
            return false;
        }
        
        PaymentStatus status = transaction.getStatus();
        
        // Only UNKNOWN transactions need recovery
        if (status != PaymentStatus.UNKNOWN) {
            logger.debug("Transaction {} not in UNKNOWN state (status={}), no recovery needed",
                transaction.getId(), status);
            return false;
        }
        
        logger.debug("Transaction {} can be recovered from UNKNOWN state", transaction.getId());
        return true;
    }
    
    /**
     * Builds HTTP response for UNKNOWN state transaction.
     * 
     * Returns 202 Accepted with message indicating outcome is uncertain.
     * Client should poll for status or wait for webhook notification.
     * 
     * @param transaction The UNKNOWN state transaction
     * @return ResponseEntity with 202 Accepted status
     */
    public ResponseEntity<PaymentResponseDTO> buildUnknownRecoveryResponse(Transaction transaction) {
        PaymentResponseDTO response = PaymentResponseDTO.builder()
            .transactionId(transaction.getId())
            .status(PaymentStatus.UNKNOWN)
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .maskedCard(transaction.getMaskedCard())
            .message("Payment outcome uncertain due to timeout. " +
                "Reconciliation in progress. " +
                "Check status later or wait for webhook notification.")
            .createdAt(transaction.getCreatedAt())
            .updatedAt(transaction.getUpdatedAt())
            .idempotencyKey(transaction.getIdempotencyKey())
            .build();
        
        logger.debug("Building 202 Accepted response for UNKNOWN transaction {}", transaction.getId());
        
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * Schedules reconciliation for UNKNOWN state transaction.
     * 
     * In Phase 4, this will trigger reconciliation worker.
     * For now, this is a placeholder for future implementation.
     * 
     * @param transaction The UNKNOWN state transaction
     */
    public void scheduleReconciliation(Transaction transaction) {
        logger.info("Scheduling reconciliation for UNKNOWN transaction: id={}, merchant_id={}",
            transaction.getId(), transaction.getMerchantId());
        
        // TODO: Implement reconciliation scheduling (Phase 4a)
        // This will:
        // 1. Create reconciliation task
        // 2. Add to reconciliation queue
        // 3. Trigger reconciliation worker
    }
}
