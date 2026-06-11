package com.acabouomony.payment.web;

import com.acabouomony.payment.domain.dto.PaymentRequest;
import com.acabouomony.payment.domain.entity.Merchant;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.PaymentValidationException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.*;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Import Authentication
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import java.security.Principal; // Import Principal
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    // MerchantAuthService is no longer needed for the main flow in this controller
    // but might be used by the security filter, so we leave it injected.
    private final MerchantAuthService merchantAuthService;
    private final PaymentRequestValidator paymentRequestValidator;
    private final IdempotencyService idempotencyService;
    private final DuplicatePaymentHandler duplicatePaymentHandler;
    private final DuplicateRequestRecoveryService duplicateRequestRecoveryService;
    private final TransactionRepository transactionRepository;
    private final PaymentOrchestrationService paymentOrchestrationService;
    
    @Autowired
    public PaymentController(
            MerchantAuthService merchantAuthService,
            PaymentRequestValidator paymentRequestValidator,
            IdempotencyService idempotencyService,
            DuplicatePaymentHandler duplicatePaymentHandler,
            DuplicateRequestRecoveryService duplicateRequestRecoveryService,
            TransactionRepository transactionRepository,
            PaymentOrchestrationService paymentOrchestrationService) {
        this.merchantAuthService = merchantAuthService;
        this.paymentRequestValidator = paymentRequestValidator;
        this.idempotencyService = idempotencyService;
        this.duplicatePaymentHandler = duplicatePaymentHandler;
        this.duplicateRequestRecoveryService = duplicateRequestRecoveryService;
        this.transactionRepository = transactionRepository;
        this.paymentOrchestrationService = paymentOrchestrationService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @Valid @RequestBody PaymentRequest request,
            Principal principal) { // 1. Inject the authenticated Principal
        
        logger.info("Payment request received: amount={}, currency={}, idempotency_key={}",
            request.getAmount(), request.getCurrency(), request.getIdempotencyKey());
        
        try {
            // 2. Get the merchant ID directly from the principal.
            // The principal's "name" is typically the username, which is our merchant_id.
            if (principal == null || principal.getName() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed or principal not found.");
            }
            UUID merchantId = UUID.fromString(principal.getName());
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
            
            // ... The rest of the method remains the same ...
            
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
                
                if (idempotencyService.isSafeToReturnCachedResponse(tx)) {
                    logger.debug("Returning cached response for transaction {}", tx.getId());
                    ResponseEntity<PaymentResponseDTO> response = duplicatePaymentHandler.buildDuplicateResponse(tx);
                    if (response.getBody() != null) {
                        duplicateRequestRecoveryService.cacheResponse(merchantId, request.getIdempotencyKey(), response.getBody());
                    }
                    return response;
                } else {
                    logger.debug("Returning appropriate status for in-progress/unknown transaction {}", tx.getId());
                    ResponseEntity<PaymentResponseDTO> response = duplicatePaymentHandler.buildDuplicateResponse(tx);
                    if (response.getBody() != null) {
                        duplicateRequestRecoveryService.cacheResponse(merchantId, request.getIdempotencyKey(), response.getBody());
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
                
                paymentOrchestrationService.processNewPaymentAsync(saved);

                ResponseEntity<PaymentResponseDTO> response = duplicatePaymentHandler.buildNewPaymentResponse(saved);
                
                if (response.getBody() != null) {
                    duplicateRequestRecoveryService.cacheResponse(merchantId, request.getIdempotencyKey(), response.getBody());
                }
                
                return response;
                
            } catch (DataIntegrityViolationException e) {
                logger.warn("DataIntegrityViolationException during transaction insert: {}", e.getMessage());
                
                Transaction recovered = duplicatePaymentHandler.handleDuplicatePayment(merchantId, request.getIdempotencyKey(), request);
                
                logger.info("Duplicate payment recovered: id={}, status={}", recovered.getId(), recovered.getStatus());
                ResponseEntity<PaymentResponseDTO> response = duplicatePaymentHandler.buildDuplicateResponse(recovered);
                
                if (response.getBody() != null) {
                    duplicateRequestRecoveryService.cacheResponse(merchantId, request.getIdempotencyKey(), response.getBody());
                }
                
                return response;
            }
            
        } catch (PaymentValidationException e) {
            logger.warn("Payment validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (ResponseStatusException e) {
            logger.warn("Authentication failed: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            logger.error("Unexpected error during payment processing", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}