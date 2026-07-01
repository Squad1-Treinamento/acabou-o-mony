package com.acabouomony.payment.domain.listener;

import com.acabouomony.payment.domain.event.ThreeDsCompletedEvent;
import com.acabouomony.payment.domain.service.PaymentOrchestrationService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener for 3DS authentication completion.
 * 
 * Listens for ThreeDsCompletedEvent and resumes payment processing
 * after successful 3DS authentication.
 * 
 * Flow:
 * 1. 3DS callback received → completeThreeDsAuthentication() called
 * 2. Transaction transitions: CHALLENGE_PENDING → AUTHENTICATED (if approved)
 *                          or CHALLENGE_PENDING → DECLINED (if rejected)
 * 3. ThreeDsCompletedEvent published
 * 4. This listener catches event
 * 5. If approved: calls resumePaymentAfterAuth()
 * 6. Payment continues: AUTHENTICATED → PROCESSING → COMPLETED/DECLINED/FAILED
 * 
 * Spec: spec-005-3ds-core-payment-integration.md
 * Task: task-007-payment-finalizer.md
 * 
 * Async Processing:
 * - Runs on separate thread pool (non-blocking)
 * - Decouples 3DS callback response from payment processing
 * - Allows fast 200 OK response to 3DS engine
 * - Payment processing continues asynchronously
 */
@Component
public class ThreeDsEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ThreeDsEventListener.class);
    
    private final PaymentOrchestrationService orchestrationService;
    
    public ThreeDsEventListener(PaymentOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }
    
    /**
     * Handles 3DS authentication completion event.
     * 
     * Called after 3DS callback updates transaction status.
     * Resumes payment processing if authentication was approved.
     * 
     * If approved:
     * - Calls resumePaymentAfterAuth() to continue payment flow
     * - Transaction moves: AUTHENTICATED → PROCESSING → final state
     * 
     * If declined:
     * - Transaction already in DECLINED state (no further action)
     * - Webhook notification already dispatched
     * 
     * @param event The 3DS completion event containing transaction ID and approval status
     */
    @EventListener
    @Async
    public void handleThreeDsCompleted(ThreeDsCompletedEvent event) {
        logger.info("3DS completion event received: transaction_id={}, approved={}", 
            event.txId(), event.approved());
        
        if (event.approved()) {
            logger.info("3DS authentication approved, resuming payment: transaction_id={}", event.txId());
            
            try {
                // Resume payment processing after successful 3DS authentication
                orchestrationService.resumePaymentAfterAuth(event.txId());
                
                logger.info("Payment resumed successfully after 3DS: transaction_id={}", event.txId());
                
            } catch (Exception e) {
                logger.error("Error resuming payment after 3DS: transaction_id={}, error={}", 
                    event.txId(), e.getMessage(), e);
                // Exception will be handled by orchestration service
                // Transaction will transition to UNKNOWN or FAILED state
            }
        } else {
            logger.info("3DS authentication declined, no further action: transaction_id={}", event.txId());
            // Transaction already in DECLINED state
            // Webhook notification already dispatched by completeThreeDsAuthentication
        }
    }
}
