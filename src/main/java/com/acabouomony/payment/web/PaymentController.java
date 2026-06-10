package com.acabouomony.payment.web;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.entity.Merchant;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.PaymentValidationException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.DuplicatePaymentHandler;
import com.acabouomony.payment.domain.service.DuplicateRequestRecoveryService;
import com.acabouomony.payment.domain.service.IdempotencyService;
import com.acabouomony.payment.domain.service.MerchantAuthService;
import com.acabouomony.payment.domain.service.PaymentRequestValidator;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for payment processing.
 * 
 * Implements payment request handling with:
 * - Merchant authentication
 * - Request validation
 * - Response caching (fast-path)
 * - Idempotency enforcement (slow-path)
 * - Duplicate detection with payload hash validation
 * - Transaction persistence
 * 
 * Spec: spec-001-core-payment-processing.md
 * Task: task-009-db-idempotency-enforcement.md
 * Task: task-010-duplicate-request-recovery.md
 * 
 * Payment Flow:
 * 1. Check response cache (fast-path)
 * 2. Authenticate merchant (API key validation)
 * 3. Validate request payload (amount, currency, payment method)
 * 4. Check for duplicate (IdempotencyService.checkDuplicate)
 *    - If duplicate found and safe to cache: return cached response
 *    - If duplicate found but not safe: return appropriate status (202/409)
 * 5. If new request:
 *    - Prepare payload hash
 *    - Create new transaction
 *    - Persist with payload hash
 *    - Handle constraint violations (fallback recovery)
 * 6. Cache response (24-hour TTL)
 * 7. Continue with payment processing (Phase 3)
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
    private DuplicateRequestRecoveryService duplicateRequestRecoveryService;
    
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
     * @return ResponseEntity with payment response
     */
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @Valid @RequestBody PaymentRequest request) {
        logger.info("Payment request received: amount={}, currency={}, idempotency_key={}",
            request.getAmount(), request.getCurrency(), request.getIdempotencyKey());
        
        try {
            // Step 0: Get authenticated merchant from Security Context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Merchant merchant = (Merchant) authentication.getPrincipal();
            UUID merchantId = merchant.getId();
            logger.debug("Request from authenticated merchant: {}", merchantId);
            
            // Step 1: Check response cache (fast-path) - Service expects UUID
            Optional<PaymentResponseDTO> cachedResponse = duplicateRequestRecoveryService.recoverFromCache(
                merchantId,
                request.getIdempotencyKey()
            );
            
            if (cachedResponse.isPresent()) {
                logger.info("Cache hit: returning cached response for idempotency_key={}",
                    request.getIdempotencyKey());
                return ResponseEntity.ok(cachedResponse.get());
            }
            
            // Step 2: Validate request payload
            paymentRequestValidator.validate(request);
            logger.debug("Request validation passed");
            
            // Step 3: Check for duplicate request - Service expects UUID
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
                    ResponseEntity<PaymentResponseDTO> response = duplicatePaymentHandler.buildDuplicateResponse(tx);
                    
                    // Cache the response - Service expects UUID
                    if (response.getBody() != null) {
                        duplicateRequestRecoveryService.cacheResponse(
                            merchantId,
                            request.getIdempotencyKey(),
                            response.getBody()
                        );
                    }
                    
                    return response;
                } else {
                    // Not safe to cache (UNKNOWN or in-progress)
                    logger.debug("Returning appropriate status for in-progress/unknown transaction {}", tx.getId());
                    ResponseEntity<PaymentResponseDTO> response = duplicatePaymentHandler.buildDuplicateResponse(tx);
                    
                    // Cache the response - Service expects UUID
                    if (response.getBody() != null) {
                        duplicateRequestRecoveryService.cacheResponse(
                            merchantId,
                            request.getIdempotencyKey(),
                            response.getBody()
                        );
                    }
                    
                    return response;
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
                
                // Build response for new transaction
                ResponseEntity<PaymentResponseDTO> response = duplicatePaymentHandler.buildNewPaymentResponse(saved);
                
                // Cache the response - Service expects UUID
                if (response.getBody() != null) {
                    duplicateRequestRecoveryService.cacheResponse(
                        merchantId,
                        request.getIdempotencyKey(),
                        response.getBody()
                    );
                }
                
                return response;
                
            } catch (DataIntegrityViolationException e) {
                // Constraint violation: fallback to duplicate recovery - Service expects UUID
                logger.warn("DataIntegrityViolationException during transaction insert: {}", e.getMessage());
                
                Transaction recovered = duplicatePaymentHandler.handleDuplicatePayment(
                    merchantId,
                    request.getIdempotencyKey(),
                    request
                );
                
                logger.info("Duplicate payment recovered: id={}, status={}", recovered.getId(), recovered.getStatus());
                ResponseEntity<PaymentResponseDTO> response = duplicatePaymentHandler.buildDuplicateResponse(recovered);
                
                // Cache the response - Service expects UUID
                if (response.getBody() != null) {
                    duplicateRequestRecoveryService.cacheResponse(
                        merchantId,
                        request.getIdempotencyKey(),
                        response.getBody()
                    );
                }
                
                return response;
            }
            
        } catch (PaymentValidationException e) {
            logger.warn("Payment validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Unexpected error during payment processing", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves the status and details of a specific payment.
     *
     * @param transactionId The UUID of the transaction to retrieve.
     * @return ResponseEntity with payment details or 404 if not found.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentStatus(
            @PathVariable("transactionId") UUID transactionId) {
        logger.info("Request to get status for transaction_id={}", transactionId);

        try {
            // Authenticate the merchant first
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Merchant merchant = (Merchant) authentication.getPrincipal();
            UUID merchantId = merchant.getId();
            logger.debug("Merchant authenticated for status check: {}", merchantId);

        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);

        return transactionOpt
                    .filter(transaction -> transaction.getMerchantId().equals(merchantId)) // Authorization check
                .map(transaction -> {
                    // Reuse the existing handler to build a consistent response
                        logger.info("Transaction found for merchant: id={}, status={}", transaction.getId(), transaction.getStatus());
                    return duplicatePaymentHandler.buildDuplicateResponse(transaction);
                })
                .orElseGet(() -> {
                        logger.warn("Transaction not found for id={} or merchant not authorized", transactionId);
                    return ResponseEntity.notFound().build();
                });
        } catch (Exception e) {
            logger.error("Unexpected error during status check", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

