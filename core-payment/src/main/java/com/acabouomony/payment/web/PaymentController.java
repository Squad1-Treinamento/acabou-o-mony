package com.acabouomony.payment.web;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.PaymentValidationException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.*;
import com.acabouomony.payment.domain.service.PaymentOrchestrationService;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Autowired
    private PaymentOrchestrationService orchestrationService;

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
            // Step 0: Extract authenticated merchant ID from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || authentication.getPrincipal() == null) {
                logger.error("No authentication found in SecurityContext");
                return ResponseEntity.status(401).build();
            }
            
            String merchantIdStr = authentication.getPrincipal().toString();
            UUID merchantId = UUID.fromString(merchantIdStr);
            logger.debug("Merchant authenticated: {}", merchantId);
            
            // Step 1: Check response cache (fast-path)
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
                    ResponseEntity<PaymentResponseDTO> response = duplicatePaymentHandler.buildDuplicateResponse(tx);
                    
                    // Cache the response
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
                    
                    // Cache the response
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

                // Step 5a: Run payment orchestration (mutates saved in place)
                orchestrationService.processPayment(saved);
                logger.info("Orchestration complete: id={}, status={}", saved.getId(), saved.getStatus());

                                // Build response for new transaction
                ResponseEntity<PaymentResponseDTO> response;
                if (saved.getStatus() == PaymentStatus.CHALLENGE_PENDING) {
                    PaymentResponseDTO dto = PaymentResponseDTO.builder()
                            .transactionId(saved.getId())
                            .merchantId(saved.getMerchantId())
                            .status(saved.getStatus())
                            .amount(saved.getAmount())
                            .currency(saved.getCurrency())
                            .createdAt(saved.getCreatedAt())
                            .updatedAt(saved.getUpdatedAt())
                            .idempotencyKey(saved.getIdempotencyKey())
                            .challengeId(saved.getChallengeId())
                            .acsUrl(saved.getChallengeAcsUrl())
                            .build();
                    response = ResponseEntity.accepted().body(dto);
                } else {
                    response = duplicatePaymentHandler.buildNewPaymentResponse(saved);
                }
                
                // Cache the response
                if (response.getBody() != null) {
                    duplicateRequestRecoveryService.cacheResponse(
                        merchantId,
                        request.getIdempotencyKey(),
                        response.getBody()
                    );
                }
                
                return response;
                
            } catch (DataIntegrityViolationException e) {
                // Constraint violation: fallback to duplicate recovery
                logger.warn("DataIntegrityViolationException during transaction insert: {}", e.getMessage());
                
                Transaction recovered = duplicatePaymentHandler.handleDuplicatePayment(
                    merchantId,
                    request.getIdempotencyKey(),
                    request
                );
                
                logger.info("Duplicate payment recovered: id={}, status={}", recovered.getId(), recovered.getStatus());
                ResponseEntity<PaymentResponseDTO> response = duplicatePaymentHandler.buildDuplicateResponse(recovered);
                
                // Cache the response
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
     * Retrieves all transactions for the authenticated merchant.
     * 
     * Authentication: Required (API key via Authorization header)
     * 
     * Response:
     * - 200 OK: List of transactions (may be empty)
     * - 401 Unauthorized: If authentication fails
     * - 500 Internal Server Error: If unexpected error occurs
     * 
     * Security:
     * - Merchant isolation enforced (queries only authenticated merchant's transactions)
     * - Merchant ID extracted from SecurityContext (set by ApiKeyAuthenticationFilter)
     * 
     * @return ResponseEntity with list of payment responses
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> getTransactions() {
        try {
            // Extract authenticated merchant ID from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || authentication.getPrincipal() == null) {
                logger.error("No authentication found in SecurityContext");
                return ResponseEntity.status(401).build();
            }
            
            String merchantIdStr = authentication.getPrincipal().toString();
            UUID merchantId = UUID.fromString(merchantIdStr);
            
            logger.info("Fetching transactions for merchant: {}", merchantId);
            
            // Query transactions for authenticated merchant
            List<Transaction> transactions = transactionRepository.findByMerchantId(merchantId);
            
            logger.debug("Found {} transactions for merchant {}", transactions.size(), merchantId);
            
            // Map to response DTOs
            List<PaymentResponseDTO> response = transactions.stream()
                .map(this::mapTransactionToResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching transactions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Maps Transaction entity to PaymentResponseDTO.
     * 
     * @param transaction The transaction entity
     * @return PaymentResponseDTO with mapped fields
     */
        private PaymentResponseDTO mapTransactionToResponse(Transaction transaction) {
        return PaymentResponseDTO.builder()
            .transactionId(transaction.getId())
            .merchantId(transaction.getMerchantId())
            .status(transaction.getStatus())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .maskedCard(transaction.getMaskedCard())
            .createdAt(transaction.getCreatedAt())
            .updatedAt(transaction.getUpdatedAt())
            .idempotencyKey(transaction.getIdempotencyKey())
            .challengeId(transaction.getChallengeId())
            .acsUrl(transaction.getChallengeAcsUrl())
            .build();
    }
}