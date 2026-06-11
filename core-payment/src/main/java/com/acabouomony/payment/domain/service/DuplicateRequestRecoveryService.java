package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.PaymentValidationException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.AuditLogRepository;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for duplicate request recovery and response caching.
 * 
 * Orchestrates:
 * - Response caching for duplicate requests
 * - Cache retrieval for fast-path responses
 * - Handling of mismatched payloads
 * - UNKNOWN state recovery
 * - Reconciliation scheduling
 * 
 * Spec: spec-001-core-payment-processing.md - Duplicate Request Rules
 * Task: task-010-duplicate-request-recovery.md
 * 
 * Duplicate Request Handling:
 * 1. Check response cache (fast-path)
 * 2. If cache hit: return cached response
 * 3. If cache miss: query database (slow-path)
 * 4. If duplicate found: validate payload hash
 * 5. If hash matches: cache and return response
 * 6. If hash differs: set UNKNOWN state and return error
 */
@Service
public class DuplicateRequestRecoveryService {
    
    private static final Logger logger = LoggerFactory.getLogger(DuplicateRequestRecoveryService.class);
    
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    
    private final PaymentResponseCache responseCache;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final UnknownStateRecoveryHandler unknownStateRecoveryHandler;
    
    public DuplicateRequestRecoveryService(
            PaymentResponseCache responseCache,
            TransactionRepository transactionRepository,
            AuditLogRepository auditLogRepository,
            UnknownStateRecoveryHandler unknownStateRecoveryHandler) {
        this.responseCache = responseCache;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.unknownStateRecoveryHandler = unknownStateRecoveryHandler;
    }
    
    /**
     * Recovers response from cache.
     * 
     * Fast-path: Check cache for existing response.
     * Returns cached response if found and not expired.
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     * @return Optional containing cached response if found
     */
    public Optional<PaymentResponseDTO> recoverFromCache(UUID merchantId, UUID idempotencyKey) {
        logger.debug("Attempting to recover response from cache: merchant={}, idempotency_key={}",
            merchantId, idempotencyKey);
        
        Optional<PaymentResponseDTO> cached = responseCache.retrieve(merchantId, idempotencyKey);
        
        if (cached.isPresent()) {
            logger.info("Cache hit: returning cached response for merchant={}, idempotency_key={}",
                merchantId, idempotencyKey);
        } else {
            logger.debug("Cache miss: merchant={}, idempotency_key={}", merchantId, idempotencyKey);
        }
        
        return cached;
    }
    
    /**
     * Caches a payment response.
     * 
     * Called after successful payment processing.
     * Caches response with 24-hour TTL.
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     * @param response The payment response to cache
     */
    public void cacheResponse(UUID merchantId, UUID idempotencyKey, PaymentResponseDTO response) {
        logger.debug("Caching payment response: merchant={}, idempotency_key={}, status={}",
            merchantId, idempotencyKey, response.getStatus());
        
        responseCache.cache(merchantId, idempotencyKey, response, CACHE_TTL);
    }
    
    /**
     * Handles mismatched payload for duplicate request.
     * 
     * When duplicate request has different payload:
     * 1. Set transaction status to UNKNOWN
     * 2. Persist audit entry
     * 3. Schedule reconciliation
     * 4. Return 400 Bad Request
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     * @param request The payment request with mismatched payload
     * @param existingTransaction The existing transaction
     * @return ResponseEntity with 400 Bad Request
     */
    public ResponseEntity<PaymentResponseDTO> handleMismatchedPayload(
            UUID merchantId,
            UUID idempotencyKey,
            PaymentRequest request,
            Transaction existingTransaction) {
        
        logger.warn("Payload mismatch detected: merchant={}, idempotency_key={}, transaction_id={}",
            merchantId, idempotencyKey, existingTransaction.getId());
        
        // Transition to UNKNOWN state
        PaymentStatus oldStatus = existingTransaction.getStatus();
        existingTransaction.setStatus(PaymentStatus.UNKNOWN);
        existingTransaction.setVersion(existingTransaction.getVersion() + 1);
        existingTransaction.setUpdatedAt(Instant.now());
        
        try {
            // Persist transaction update
            transactionRepository.save(existingTransaction);
            
            // Create audit entry
            // TODO: Implement audit log creation (requires AuditLog entity)
            // auditLogRepository.save(new AuditLog(
            //     existingTransaction.getId(),
            //     oldStatus,
            //     PaymentStatus.UNKNOWN,
            //     "duplicate_request_mismatch"
            // ));
            
            logger.info("Transaction transitioned to UNKNOWN due to payload mismatch: id={}",
                existingTransaction.getId());
            
        } catch (Exception e) {
            logger.error("Error handling mismatched payload: {}", e.getMessage(), e);
            // Continue with error response even if audit fails
        }
        
        // Schedule reconciliation
        unknownStateRecoveryHandler.scheduleReconciliation(existingTransaction);
        
        // Return 400 Bad Request
        PaymentResponseDTO errorResponse = PaymentResponseDTO.builder()
            .transactionId(existingTransaction.getId())
            .status(PaymentStatus.UNKNOWN)
            .amount(existingTransaction.getAmount())
            .currency(existingTransaction.getCurrency())
            .message("Idempotency key mismatch: payload differs. " +
                "Same idempotency_key cannot be used for different payments. " +
                "Submit with NEW idempotency_key for different payment. " +
                "Transaction transitioned to UNKNOWN state for reconciliation.")
            .createdAt(existingTransaction.getCreatedAt())
            .updatedAt(existingTransaction.getUpdatedAt())
            .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handles recovery for UNKNOWN state transaction.
     * 
     * When duplicate request matches existing UNKNOWN transaction:
     * 1. Check if recovery is possible
     * 2. Schedule reconciliation if needed
     * 3. Return 202 Accepted (outcome uncertain)
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     * @param existingTransaction The existing UNKNOWN transaction
     * @return ResponseEntity with 202 Accepted
     */
    public ResponseEntity<PaymentResponseDTO> handleUnknownStateRecovery(
            UUID merchantId,
            UUID idempotencyKey,
            Transaction existingTransaction) {
        
        logger.debug("Handling UNKNOWN state recovery: merchant={}, idempotency_key={}, transaction_id={}",
            merchantId, idempotencyKey, existingTransaction.getId());
        
        // Check if recovery is possible
        if (!unknownStateRecoveryHandler.canRecoverFromUnknown(existingTransaction)) {
            logger.warn("Cannot recover from UNKNOWN state: transaction_id={}", existingTransaction.getId());
            // Return conflict if not in UNKNOWN state
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        // Schedule reconciliation
        unknownStateRecoveryHandler.scheduleReconciliation(existingTransaction);
        
        // Build and return 202 Accepted response
        return unknownStateRecoveryHandler.buildUnknownRecoveryResponse(existingTransaction);
    }
    
    /**
     * Invalidates cached response.
     * 
     * Called when error occurs during processing.
     * Ensures stale response not returned on retry.
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     */
    public void invalidateCache(UUID merchantId, UUID idempotencyKey) {
        logger.debug("Invalidating cache: merchant={}, idempotency_key={}", merchantId, idempotencyKey);
        responseCache.invalidate(merchantId, idempotencyKey);
    }
}
