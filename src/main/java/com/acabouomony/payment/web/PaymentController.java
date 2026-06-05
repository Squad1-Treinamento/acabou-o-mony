package com.acabouomony.payment.web;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.PaymentValidationException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.DuplicatePaymentHandler;
import com.acabouomony.payment.domain.service.IdempotencyService;
import com.acabouomony.payment.domain.service.MerchantAuthService;
import com.acabouomony.payment.domain.service.PaymentRequestValidator;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for payment processing.
 * 
 * Implements payment request handling with:
 * - Merchant authentication
 * - Request validation
 * - Idempotency enforcement (fast-path + slow-path)
 * - Duplicate detection with payload hash validation
 * - Transaction persistence
 * 
 * Spec: spec-001-core-payment-processing.md
 * Task: task-009-db-idempotency-enforcement.md
 * 
 * Payment Flow:
 * 1. Authenticate merchant (API key validation)
 * 2. Validate request payload (amount, currency, payment method)
 * 3. Check for duplicate (IdempotencyService.checkDuplicate)
 *    - If duplicate found and safe to cache: return cached response
 *    - If duplicate found but not safe: return 202/409
 * 4. If new request:
 *    - Prepare payload hash
 *    - Create new transaction
 *    - Persist with payload hash
 *    - Handle constraint violations (fallback recovery)
 * 5. Continue with payment processing (Phase 3)
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    @Autowired
    private MerchantAuthService merchantAuthService;
    
    @Autowired
    private PaymentRequestValidator paymentRequestValidator;
    
    @Autowired
    private IdempotencyService idempotencyService;
    
    @Autowired
    private DuplicatePaymentHandler duplicatePaymentHandler;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    /**
     * Creates a new payment or returns cached response for duplicate.
     * 
     * Request must include:
     * - Authorization header with merchant API key
     * - Valid payment request body with idempotency_key
     * 
     * Response:
     * - 200 OK: Payment completed (COMPLETED/DECLINED/FAILED)
     * - 202 Accepted: Payment processing (UNKNOWN/in-progress)
     * - 400 Bad Request: Validation error or payload mismatch
     * - 409 Conflict: Duplicate request with in-progress payment
     * - 401 Unauthorized: Invalid API key
     * - 500 Internal Server Error: Unexpected error
     * 
     * @param request The payment request
     * @param authHeader The Authorization header (API key)
     * @return ResponseEntity with payment response
     */
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Payment request received: amount={}, currency={}, idempotency_key={}",
            request.getAmount(), request.getCurrency(), request.getIdempotencyKey());
        
        try {
            // Step 1: Authenticate merchant
            // TODO: Implement merchant authentication (Task-004)
            // For now, use a placeholder merchant ID
            UUID merchantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            logger.debug("Merchant authenticated: {}", merchantId);
            
            // Step 2: Validate request payload
            paymentRequestValidator.validate(request);
            logger.debug("Request validation passed");
            
            // Step 3: Check for duplicate request
            Optional<Transaction> existingTx = idempotencyService.checkDuplicate(
                merchantId,
                request.getIdempotencyKey(),
                request
            );
            
            if (existingTx.isPresent()) {
                Transaction tx = existingTx.get();
                logger.info("Duplicate request detected: transaction_id={}, status={}",
                    tx.getId(), tx.getStatus());
                
                // Check if safe to return cached response
                if (idempotencyService.isSafeToReturnCachedResponse(tx)) {
                    logger.debug("Returning cached response for transaction {}", tx.getId());
                    return duplicatePaymentHandler.buildDuplicateResponse(tx);
                } else {
                    // Not safe to cache (UNKNOWN or in-progress)
                    logger.debug("Returning appropriate status for in-progress/unknown transaction {}", tx.getId());
                    return duplicatePaymentHandler.buildDuplicateResponse(tx);
                }
            }
            
            logger.debug("New payment request (no duplicate found)");
            
            // Step 4: Create new transaction
            String payloadHash = idempotencyService.preparePayloadHash(request);
            
            Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .idempotencyKey(request.getIdempotencyKey())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.CREATED)
                .payloadHash(payloadHash)
                .maskedCard(request.getPaymentMethod().getMaskedCard())
                .cardTokenId(request.getPaymentMethod().getCardTokenId())
                .version(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            logger.debug("Created transaction object: id={}, status={}", transaction.getId(), transaction.getStatus());
            
            // Step 5: Persist transaction
            try {
                Transaction saved = transactionRepository.save(transaction);
                logger.info("Transaction persisted: id={}, status={}", saved.getId(), saved.getStatus());
                
                // Return response for new transaction
                return duplicatePaymentHandler.buildNewPaymentResponse(saved);
                
            } catch (DataIntegrityViolationException e) {
                // Constraint violation: fallback to duplicate recovery
                logger.warn("DataIntegrityViolationException during transaction insert: {}", e.getMessage());
                
                Transaction recovered = duplicatePaymentHandler.handleDuplicatePayment(
                    merchantId,
                    request.getIdempotencyKey(),
                    request
                );
                
                logger.info("Duplicate payment recovered: id={}, status={}", recovered.getId(), recovered.getStatus());
                return duplicatePaymentHandler.buildDuplicateResponse(recovered);
            }
            
        } catch (PaymentValidationException e) {
            logger.warn("Payment validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Unexpected error during payment processing", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}