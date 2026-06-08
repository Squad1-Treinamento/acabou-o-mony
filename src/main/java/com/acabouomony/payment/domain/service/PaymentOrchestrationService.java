package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentResult;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.PaymentAcquirerException;
import com.acabouomony.payment.domain.exception.PaymentTimeoutException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Payment processing orchestration service.
 * 
 * Orchestrates complete payment flow:
 * 1. Validate transaction state
 * 2. Transition CREATED -> VALIDATED
 * 3. Evaluate risk (low-risk vs high-risk)
 * 4. Transition to PROCESSING or CHALLENGE_PENDING
 * 5. Call MercadoPagoClient.submitPayment()
 * 6. Handle result (COMPLETED/DECLINED/FAILED/UNKNOWN)
 * 7. Persist transaction with optimistic locking
 * 8. Create audit log entry
 * 9. Create outbox event for webhook
 * 
 * Spec: spec-001-core-payment-processing.md - Payment Lifecycle Rules
 * Task: task-012-payment-orchestration.md, task-013-unknown-state-handling.md
 * 
 * State Transitions:
 * - CREATED -> VALIDATED (validation)
 * - VALIDATED -> CHALLENGE_PENDING (high-risk, 3DS required)
 * - VALIDATED -> PROCESSING (low-risk, skip 3DS)
 * - PROCESSING -> COMPLETED/DECLINED/FAILED/UNKNOWN (payment result)
 * 
 * Timeout Handling:
 * - PaymentTimeoutException caught and delegated to UnknownStateTransitionHandler
 * - PaymentAcquirerException caught and delegated to UnknownStateTransitionHandler
 * - Both trigger PROCESSING -> UNKNOWN transition
 * - Reconciliation scheduled by Phase 4a
 * 
 * Optimistic Locking:
 * - Retry up to 3 times on version conflict
 * - Exponential backoff: 100ms, 200ms, 300ms
 * - Fail fast after 3 attempts (operator intervention required)
 */
@Service
public class PaymentOrchestrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentOrchestrationService.class);
    
    private static final int MAX_RETRIES = 3;
    private static final long BACKOFF_BASE_MS = 100;
    
    private final TransactionRepository transactionRepository;
    private final PaymentAcquirerClient paymentAcquirerClient;
    private final RiskEvaluationService riskEvaluationService;
    private final StateTransitionValidator stateTransitionValidator;
    private final AuditLogService auditLogService;
    private final OutboxEventService outboxEventService;
    private final UnknownStateTransitionHandler unknownStateTransitionHandler;
    
    public PaymentOrchestrationService(
            TransactionRepository transactionRepository,
            PaymentAcquirerClient paymentAcquirerClient,
            RiskEvaluationService riskEvaluationService,
            StateTransitionValidator stateTransitionValidator,
            AuditLogService auditLogService,
            OutboxEventService outboxEventService,
            UnknownStateTransitionHandler unknownStateTransitionHandler) {
        this.transactionRepository = transactionRepository;
        this.paymentAcquirerClient = paymentAcquirerClient;
        this.riskEvaluationService = riskEvaluationService;
        this.stateTransitionValidator = stateTransitionValidator;
        this.auditLogService = auditLogService;
        this.outboxEventService = outboxEventService;
        this.unknownStateTransitionHandler = unknownStateTransitionHandler;
    }
    
    /**
     * Processes a payment through complete orchestration flow.
     * 
     * Handles all scenarios:
     * - Low-risk: CREATED -> VALIDATED -> PROCESSING -> COMPLETED/DECLINED/FAILED/UNKNOWN
     * - High-risk: CREATED -> VALIDATED -> CHALLENGE_PENDING (wait for 3DS)
     * 
     * @param transaction The transaction to process
     */
    public void processPayment(Transaction transaction) {
        logger.info("Processing payment: transaction_id={}, amount={}, currency={}",
            transaction.getId(), transaction.getAmount(), transaction.getCurrency());
        
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        // Validate initial state
        if (transaction.getStatus() != PaymentStatus.CREATED) {
            throw new IllegalStateException(
                String.format("Transaction must be in CREATED state, but is in %s", transaction.getStatus())
            );
        }
        
        try {
            // Step 1: Transition CREATED -> VALIDATED
            transitionState(transaction, PaymentStatus.VALIDATED, "system");
            
            // Step 2: Evaluate risk
            boolean isHighRisk = riskEvaluationService.isHighRisk(transaction);
            
            if (isHighRisk) {
                // High-risk: transition to CHALLENGE_PENDING (3DS required)
                logger.info("High-risk transaction detected: transaction_id={}, transitioning to CHALLENGE_PENDING",
                    transaction.getId());
                transitionState(transaction, PaymentStatus.CHALLENGE_PENDING, "system");
                // Return here; payment will continue after 3DS challenge completes
                return;
            }
            
            // Low-risk: proceed directly to PROCESSING
            logger.debug("Low-risk transaction: transaction_id={}, proceeding to PROCESSING", transaction.getId());
            transitionState(transaction, PaymentStatus.PROCESSING, "system");
            
            // Step 3: Submit payment to acquirer
            PaymentResult result = paymentAcquirerClient.submitPayment(transaction);
            
            logger.info("Payment result received: transaction_id={}, status={}, acquirer_ref={}",
                transaction.getId(), result.getStatus(), result.getAcquirerReference());
            
            // Step 4: Handle payment result
            handlePaymentResult(transaction, result);
            
        } catch (PaymentTimeoutException e) {
            // Timeout during Mercado Pago call
            logger.warn("Payment timeout, transitioning to UNKNOWN: transaction_id={}, error={}",
                transaction.getId(), e.getMessage());
            
            // Transition to UNKNOWN state
            unknownStateTransitionHandler.transitionToUnknownDueToTimeout(transaction, e.getMessage());
            
        } catch (PaymentAcquirerException e) {
            // Acquirer error during payment submission
            logger.warn("Acquirer error, transitioning to UNKNOWN: transaction_id={}, error={}",
                transaction.getId(), e.getMessage());
            
            // Transition to UNKNOWN state
            unknownStateTransitionHandler.transitionToUnknownDueToAcquirerError(transaction, e.getMessage());
            
        } catch (Exception e) {
            logger.error("Error processing payment: transaction_id={}, error={}", transaction.getId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Handles payment result from acquirer.
     * 
     * Maps PaymentResult to transaction state and persists.
     * 
     * @param transaction The transaction
     * @param result The payment result from acquirer
     */
    private void handlePaymentResult(Transaction transaction, PaymentResult result) {
        logger.debug("Handling payment result: transaction_id={}, status={}", transaction.getId(), result.getStatus());
        
        // Persist acquirer reference
        if (result.getAcquirerReference() != null) {
            transaction.setAcquirerReference(result.getAcquirerReference());
        }
        
        // Transition to final state based on result
        PaymentStatus resultStatus = result.getStatus();
        
        switch (resultStatus) {
            case COMPLETED:
                transitionState(transaction, PaymentStatus.COMPLETED, "system");
                outboxEventService.createPaymentCompletedEvent(transaction);
                break;
                
            case DECLINED:
                transitionState(transaction, PaymentStatus.DECLINED, "system");
                outboxEventService.createPaymentDeclinedEvent(transaction);
                break;
                
            case FAILED:
                transitionState(transaction, PaymentStatus.FAILED, "system");
                outboxEventService.createPaymentFailedEvent(transaction);
                break;
                
            case UNKNOWN:
                // This should not happen in normal flow (handled by exception catch)
                // But if it does, transition to UNKNOWN
                unknownStateTransitionHandler.transitionToUnknownDueToAcquirerError(transaction, 
                    "Acquirer returned UNKNOWN status");
                break;
                
            default:
                logger.error("Unexpected payment status: transaction_id={}, status={}", transaction.getId(), resultStatus);
                transitionState(transaction, PaymentStatus.FAILED, "system");
                outboxEventService.createPaymentFailedEvent(transaction);
        }
    }
    
    /**
     * Transitions transaction to new state with optimistic locking retry.
     * 
     * Implements retry loop with exponential backoff:
     * - Attempt 1: immediate
     * - Attempt 2: 100ms backoff
     * - Attempt 3: 200ms backoff
     * - Fail fast after 3 attempts
     * 
     * @param transaction The transaction
     * @param newStatus The target status
     * @param actor The actor performing transition
     */
    private void transitionState(Transaction transaction, PaymentStatus newStatus, String actor) {
        PaymentStatus oldStatus = transaction.getStatus();
        
        // Validate transition
        stateTransitionValidator.validateTransition(oldStatus, newStatus);
        
        logger.debug("Transitioning state: transaction_id={}, {} -> {}, actor={}",
            transaction.getId(), oldStatus, newStatus, actor);
        
        // Retry loop for optimistic locking
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                // Update transaction state
                transaction.setStatus(newStatus);
                transaction.setVersion(transaction.getVersion() + 1);
                transaction.setUpdatedAt(Instant.now());
                
                // Persist transaction (version conflict checked here)
                transactionRepository.save(transaction);
                
                // Create audit log entry
                auditLogService.logStateTransition(transaction, oldStatus, newStatus, actor);
                
                logger.info("State transition successful: transaction_id={}, {} -> {}, version={}",
                    transaction.getId(), oldStatus, newStatus, transaction.getVersion());
                
                return;  // Success
                
            } catch (ObjectOptimisticLockingFailureException e) {
                // Version conflict: retry
                if (attempt == MAX_RETRIES - 1) {
                    // Final attempt failed
                    logger.error("OptimisticLockException after {} attempts: transaction_id={}",
                        MAX_RETRIES, transaction.getId());
                    throw new RuntimeException("Failed to transition state after " + MAX_RETRIES + " attempts", e);
                }
                
                // Exponential backoff
                long backoffMs = BACKOFF_BASE_MS * (attempt + 1);
                logger.warn("Version conflict, retrying after {}ms: transaction_id={}, attempt={}/{}",
                    backoffMs, transaction.getId(), attempt + 1, MAX_RETRIES);
                
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted during retry backoff: transaction_id={}", transaction.getId());
                    throw new RuntimeException("Interrupted during state transition retry", ie);
                }
                
                // Re-query transaction for fresh version
                Transaction fresh = transactionRepository.findById(transaction.getId())
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + transaction.getId()));
                
                // Check if already in target state (idempotent)
                if (fresh.getStatus() == newStatus) {
                    logger.info("State already transitioned by concurrent operation: transaction_id={}, status={}",
                        transaction.getId(), newStatus);
                    transaction.setVersion(fresh.getVersion());
                    transaction.setUpdatedAt(fresh.getUpdatedAt());
                    return;  // Success (idempotent)
                }
                
                // Update transaction reference for next attempt
                transaction.setVersion(fresh.getVersion());
            }
        }
    }
}
