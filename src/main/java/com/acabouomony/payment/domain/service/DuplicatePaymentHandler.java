package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.DuplicatePaymentException;
import com.acabouomony.payment.domain.exception.PaymentValidationException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling duplicate payment requests.
 * 
 * Implements fallback recovery when database UNIQUE constraint is violated.
 * Recovers existing transaction and returns appropriate response.
 * 
 * Spec: spec-001-core-payment-processing.md - Duplicate Request Rules
 * Task: task-009-db-idempotency-enforcement.md
 * 
 * Duplicate Detection Flow:
 * 1. New payment request arrives
 * 2. IdempotencyService.checkDuplicate() queries database
 * 3. If no existing transaction: proceed with new transaction
 * 4. If existing transaction found: validate payload hash and return cached response
 * 5. If constraint violation during insert (race condition): fallback recovery
 *    - Query database for existing transaction
 *    - Validate payload hash
 *    - Return cached response or error
 */
@Service
public class DuplicatePaymentHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(DuplicatePaymentHandler.class);
    
    private final TransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;
    
    public DuplicatePaymentHandler(
            TransactionRepository transactionRepository,
            IdempotencyService idempotencyService) {
        this.transactionRepository = transactionRepository;
        this.idempotencyService = idempotencyService;
    }
    
    /**
     * Handles duplicate payment when UNIQUE constraint violated.
     * 
     * Called when database constraint violation occurs during transaction insert.
     * Recovers existing transaction and validates payload hash.
     * 
     * Flow:
     * 1. Query database for existing transaction by merchant + idempotency_key
     * 2. If found: validate payload hash matches
     * 3. If hash matches: return existing transaction (duplicate detected)
     * 4. If hash differs: throw PaymentValidationException (different request)
     * 5. If not found: re-throw original constraint violation (unexpected)
     * 
     * @param merchantId The merchant ID
     * @param idempotencyKey The idempotency key
     * @param request The payment request (for payload hash validation)
     * @return Existing transaction if duplicate detected
     * @throws DuplicatePaymentException if constraint violation but no transaction found
     * @throws PaymentValidationException if payload hash mismatch
     */
    public Transaction handleDuplicatePayment(
            UUID merchantId,
            UUID idempotencyKey,
            PaymentRequest request) {
        
        logger.debug("Handling duplicate payment: merchant={}, idempotency_key={}", merchantId, idempotencyKey);
        
        // Query database for existing transaction
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKeyAndMerchantId(
            idempotencyKey,
            merchantId
        );
        
        if (existing.isEmpty()) {
            logger.error(
                "Constraint violation but no transaction found: merchant={}, idempotency_key={}. " +
                "This indicates a race condition or database inconsistency.",
                merchantId, idempotencyKey
            );
            throw new DuplicatePaymentException(
                "Database constraint violation detected but transaction not found. " +
                "This may indicate a race condition. Please retry with a new idempotency key.",
                null, merchantId, idempotencyKey
            );
        }
        
        Transaction existingTx = existing.get();
        
        // Validate payload hash matches (using IdempotencyService)
        try {
            idempotencyService.checkDuplicate(merchantId, idempotencyKey, request);
        } catch (PaymentValidationException e) {
            // Payload hash mismatch - different request with same idempotency key
            logger.warn(
                "Payload hash mismatch for duplicate payment: merchant={}, idempotency_key={}, transaction_id={}",
                merchantId, idempotencyKey, existingTx.getId()
            );
            throw e;  // Re-throw validation exception
        }
        
        logger.info(
            "Duplicate payment recovered: merchant={}, idempotency_key={}, transaction_id={}, status={}",
            merchantId, idempotencyKey, existingTx.getId(), existingTx.getStatus()
        );
        
        return existingTx;
    }
    
    /**
     * Builds HTTP response for duplicate payment.
     * 
     * Determines appropriate HTTP status based on transaction status:
     * - Terminal states (COMPLETED/DECLINED/FAILED): 200 OK with cached response
     * - UNKNOWN state: 202 Accepted (outcome uncertain, poll for status)
     * - In-progress states: 409 Conflict (retry later)
     * 
     * @param transaction The existing transaction
     * @return ResponseEntity with appropriate status and response body
     */
    public ResponseEntity<PaymentResponseDTO> buildDuplicateResponse(Transaction transaction) {
        PaymentStatus status = transaction.getStatus();
        
        PaymentResponseDTO response = PaymentResponseDTO.builder()
            .transactionId(transaction.getId())
            .status(status)
            .amount(transaction.getAmount().longValue())
            .currency(transaction.getCurrency())
            .maskedCard(transaction.getMaskedCard())
            .createdAt(transaction.getCreatedAt())
            .updatedAt(transaction.getUpdatedAt())
            .idempotencyKey(UUID.fromString(transaction.getIdempotencyKey()))
            .build();
        
        // Terminal states: safe to return cached response
        if (status == PaymentStatus.COMPLETED) {
            response.setMessage("Payment approved");
            logger.debug("Returning cached COMPLETED response for transaction {}", transaction.getId());
            return ResponseEntity.ok(response);
        }
        
        if (status == PaymentStatus.DECLINED) {
            response.setMessage("Payment declined");
            logger.debug("Returning cached DECLINED response for transaction {}", transaction.getId());
            return ResponseEntity.ok(response);
        }
        
        if (status == PaymentStatus.FAILED) {
            response.setMessage("Payment failed");
            logger.debug("Returning cached FAILED response for transaction {}", transaction.getId());
            return ResponseEntity.ok(response);
        }
        
        // UNKNOWN state: return 202 Accepted (outcome uncertain)
        if (status == PaymentStatus.UNKNOWN) {
            response.setMessage("Payment outcome uncertain. Please check status later or poll /api/v1/payments/{transaction_id}");
            logger.debug("Returning 202 Accepted for UNKNOWN transaction {}", transaction.getId());
            return ResponseEntity.accepted().body(response);
        }
        
        // In-progress states: return 409 Conflict (retry later)
        logger.debug("Returning 409 Conflict for in-progress transaction {} with status {}", transaction.getId(), status);
        response.setMessage("Payment processing in progress. Please retry later.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    /**
     * Builds HTTP response for new payment (not duplicate).
     * 
     * Used when payment request is new (not a duplicate).
     * Returns 200 OK or 202 Accepted depending on transaction status.
     * 
     * @param transaction The newly created transaction
     * @return ResponseEntity with appropriate status and response body
     */
    public ResponseEntity<PaymentResponseDTO> buildNewPaymentResponse(Transaction transaction) {
        PaymentStatus status = transaction.getStatus();
        
        PaymentResponseDTO response = PaymentResponseDTO.builder()
            .transactionId(transaction.getId())
            .status(status)
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .maskedCard(transaction.getMaskedCard())
            .idempotencyKey(transaction.getIdempotencyKey())
            .createdAt(transaction.getCreatedAt())
            .updatedAt(transaction.getUpdatedAt())
            .build();
        
        // Terminal states: payment completed synchronously
        if (status == PaymentStatus.COMPLETED) {
            response.setMessage("Payment approved");
            logger.debug("Returning 200 OK for COMPLETED transaction {}", transaction.getId());
            return ResponseEntity.ok(response);
        }
        
        if (status == PaymentStatus.DECLINED) {
            response.setMessage("Payment declined");
            logger.debug("Returning 200 OK for DECLINED transaction {}", transaction.getId());
            return ResponseEntity.ok(response);
        }
        
        if (status == PaymentStatus.FAILED) {
            response.setMessage("Payment failed");
            logger.debug("Returning 200 OK for FAILED transaction {}", transaction.getId());
            return ResponseEntity.ok(response);
        }
        
        // UNKNOWN state: return 202 Accepted (outcome uncertain)
        if (status == PaymentStatus.UNKNOWN) {
            response.setMessage("Payment processing uncertain. Please check status later.");
            logger.debug("Returning 202 Accepted for UNKNOWN transaction {}", transaction.getId());
            return ResponseEntity.accepted().body(response);
        }
        
        // In-progress states: return 202 Accepted (processing continues)
        logger.debug("Returning 202 Accepted for in-progress transaction {} with status {}", transaction.getId(), status);
        response.setMessage("Payment processing. Check status later.");
        return ResponseEntity.accepted().body(response);
    }
}

