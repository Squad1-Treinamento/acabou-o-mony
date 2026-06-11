package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.PaymentValidationException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for idempotency coordination and duplicate detection.
 * 
 * Implements two-tier idempotency model:
 * 1. Fast-path: Redis cache (NOT authoritative)
 * 2. Slow-path: PostgreSQL UNIQUE constraint (authoritative)
 * 
 * Validates payload hash to detect duplicate requests with different payloads.
 * 
 * Spec: spec-001-core-payment-processing.md - Idempotency Rules
 * Task: task-007-payload-hashing.md
 * 
 * Idempotency enforcement:
 * - Same idempotency_key + same payload_hash = identical retry (return cached response)
 * - Same idempotency_key + different payload_hash = different request (error)
 * - Different idempotency_key = new transaction (allowed)
 */
@Service
public class IdempotencyService {
    
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    
    private final TransactionRepository transactionRepository;
    private final PayloadHashingService payloadHashingService;
    
    public IdempotencyService(
            TransactionRepository transactionRepository,
            PayloadHashingService payloadHashingService) {
        this.transactionRepository = transactionRepository;
        this.payloadHashingService = payloadHashingService;
    }
    
    /**
     * Checks for duplicate request using idempotency key and payload hash.
     * 
     * Returns one of:
     * 1. null: No existing transaction (new request, safe to proceed)
     * 2. Existing transaction: Found matching idempotency_key
     *    - If payload_hash matches: identical retry (return cached response)
     *    - If payload_hash differs: different request (throw error)
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key from request
     * @param request The payment request (for payload hash validation)
     * @return Existing transaction if found, null if new request
     * @throws PaymentValidationException if payload hash mismatch detected
     */
    public Optional<Transaction> checkDuplicate(
            UUID merchantId,
            UUID idempotencyKey,
            PaymentRequest request) {
        
        // Query database for existing transaction with same merchant + idempotency_key
        Optional<Transaction> existing = transactionRepository.findByMerchantIdAndIdempotencyKey(
            merchantId,
            idempotencyKey
        );
        
        if (existing.isEmpty()) {
            logger.debug("No existing transaction for merchant={}, idempotency_key={}", merchantId, idempotencyKey);
            return Optional.empty();
        }
        
        Transaction existingTx = existing.get();
        String currentPayloadHash = payloadHashingService.computePayloadHash(request);
        String storedPayloadHash = existingTx.getPayloadHash();
        
        // Validate payload hash matches
        if (!currentPayloadHash.equals(storedPayloadHash)) {
            logger.warn(
                "Payload hash mismatch for merchant={}, idempotency_key={}: current={}, stored={}",
                merchantId, idempotencyKey, currentPayloadHash, storedPayloadHash
            );
            throw new PaymentValidationException(
                "Idempotency key mismatch: payload differs. " +
                "Same idempotency_key cannot be used for different payments. " +
                "Submit with NEW idempotency_key for different payment."
            );
        }
        
        logger.debug(
            "Duplicate request detected for merchant={}, idempotency_key={}. " +
            "Payload hash matches (identical retry).",
            merchantId, idempotencyKey
        );
        
        return existing;
    }
    
    /**
     * Determines if a duplicate transaction is safe to return cached response.
     * 
     * Rules:
     * - If transaction is IN_PROGRESS (PROCESSING): Return 409 Conflict (retry later)
     * - If transaction is terminal (COMPLETED/DECLINED/FAILED): Return cached response
     * - If transaction is UNKNOWN: Return 202 Accepted (payment outcome uncertain)
     * 
     * @param transaction The existing transaction
     * @return true if safe to return cached response, false if conflict/uncertain
     */
    public boolean isSafeToReturnCachedResponse(Transaction transaction) {
        PaymentStatus status = transaction.getStatus();
        
        // Terminal states: safe to return cached response
        if (status == PaymentStatus.COMPLETED || 
            status == PaymentStatus.DECLINED || 
            status == PaymentStatus.FAILED) {
            logger.debug("Transaction {} in terminal state {}, safe to return cached response", 
                transaction.getId(), status);
            return true;
        }
        
        // UNKNOWN state: return 202 Accepted (outcome uncertain)
        if (status == PaymentStatus.UNKNOWN) {
            logger.debug("Transaction {} in UNKNOWN state, return 202 Accepted", transaction.getId());
            return false;  // Caller should return 202, not cached response
        }
        
        // IN_PROGRESS states: return 409 Conflict (retry later)
        logger.debug("Transaction {} in progress state {}, return 409 Conflict", transaction.getId(), status);
        return false;  // Caller should return 409, not cached response
    }
    
    /**
     * Prepares payload hash for new transaction persistence.
     * 
     * Called before creating new transaction to ensure payload hash is computed
     * and available for persistence in database.
     * 
     * @param request The payment request
     * @return Computed payload hash (SHA-256 hex string)
     */
    public String preparePayloadHash(PaymentRequest request) {
        String hash = payloadHashingService.computePayloadHash(request);
        logger.debug("Prepared payload hash for new transaction: {}", hash);
        return hash;
    }
}
