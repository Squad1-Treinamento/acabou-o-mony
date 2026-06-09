package com.acabouomony.payment.domain.service.audit;

import com.acabouomony.payment.domain.model.AuditEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for structured audit logging with masking and tamper detection.
 * 
 * Provides comprehensive audit logging for all sensitive or security-relevant actions.
 * Includes data masking to prevesums for tamper detection.
 * 
 * Spec: spent PII exposure and checkc-001-core-payment-processing.md - Audit Log Persistence
 * Task: task-019-structured-audit-logging.md
 * 
 * Features:
 * - Structured logging with event types
 * - Automatic data masking for PII
 * - SHA256 checksums for tamper detection
 * - Flexible key-value detail storage
 * - JSON serialization for log aggregation
 * - Immutable audit trail
 */
@Service
public class StructuredAuditLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(StructuredAuditLogger.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    
    private final DataMaskingService maskingService;
    private final ObjectMapper objectMapper;
    
    public StructuredAuditLogger(DataMaskingService maskingService, ObjectMapper objectMapper) {
        this.maskingService = maskingService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Logs a structured audit event.
     * 
     * Computes checksum, masks sensitive data, and logs as JSON.
     * 
     * @param event The audit event to log
     */
    public void logEvent(StructuredAuditEvent event) {
        try {
            // Set timestamp if not already set
            if (event.getTimestamp() == null) {
                event.setTimestamp(Instant.now());
            }
            
            // Set event ID if not already set
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID());
            }
            
            // Compute checksum
            String checksum = computeChecksum(event);
            event.setChecksum(checksum);
            
            // Serialize to JSON
            String jsonEvent = objectMapper.writeValueAsString(event);
            
            // Log as structured event
            auditLogger.info("AUDIT_EVENT: {}", jsonEvent);
            
            logger.debug("Audit event logged: event_id={}, type={}, transaction_id={}, checksum={}",
                event.getEventId(), event.getEventType(), event.getTransactionId(), checksum);
            
        } catch (Exception e) {
            logger.error("Error logging audit event: {}", e.getMessage(), e);
            // Log error but don't throw (audit logging should not break payment processing)
        }
    }
    
    /**
     * Logs payment state transition.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param oldStatus Old payment status
     * @param newStatus New payment status
     * @param actor Actor performing transition
     */
    public void logPaymentStateTransition(
            UUID transactionId,
            UUID merchantId,
            String oldStatus,
            String newStatus,
            String actor) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.PAYMENT_STATE_TRANSITION)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor(actor)
            .status("SUCCESS")
            .message("Payment state transitioned: " + oldStatus + " → " + newStatus)
            .timestamp(Instant.now())
            .build()
            .addDetail("old_status", oldStatus)
            .addDetail("new_status", newStatus);
        
        logEvent(event);
    }
    
    /**
     * Logs payment validation.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param validationResult Validation result (SUCCESS, FAILURE)
     * @param message Validation message
     */
    public void logPaymentValidation(
            UUID transactionId,
            UUID merchantId,
            String validationResult,
            String message) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.PAYMENT_VALIDATION)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status(validationResult)
            .message(message)
            .timestamp(Instant.now())
            .build();
        
        logEvent(event);
    }
    
    /**
     * Logs 3DS challenge initiation.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param challengeUrl Challenge URL
     */
    public void logChallengeInitiated(
            UUID transactionId,
            UUID merchantId,
            String challengeUrl) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.CHALLENGE_INITIATED)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status("SUCCESS")
            .message("3DS challenge initiated")
            .timestamp(Instant.now())
            .build()
            .addDetail("challenge_url", maskingService.maskSensitiveValue(challengeUrl));
        
        logEvent(event);
    }
    
    /**
     * Logs 3DS challenge completion.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param result Challenge result (SUCCESS, FAILURE)
     */
    public void logChallengeCompleted(
            UUID transactionId,
            UUID merchantId,
            String result) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.CHALLENGE_COMPLETED)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status(result)
            .message("3DS challenge completed: " + result)
            .timestamp(Instant.now())
            .build();
        
        logEvent(event);
    }
    
    /**
     * Logs reconciliation attempt.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param attemptNumber Attempt number
     */
    public void logReconciliationAttempt(
            UUID transactionId,
            UUID merchantId,
            int attemptNumber) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.RECONCILIATION_ATTEMPT)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status("PENDING")
            .message("Reconciliation attempt #" + attemptNumber)
            .timestamp(Instant.now())
            .build()
            .addDetail("attempt_number", attemptNumber);
        
        logEvent(event);
    }
    
    /**
     * Logs reconciliation completion.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param finalStatus Final payment status
     * @param attempts Number of attempts
     */
    public void logReconciliationCompleted(
            UUID transactionId,
            UUID merchantId,
            String finalStatus,
            int attempts) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.RECONCILIATION_COMPLETED)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status("SUCCESS")
            .message("Reconciliation completed: " + finalStatus)
            .timestamp(Instant.now())
            .build()
            .addDetail("final_status", finalStatus)
            .addDetail("attempts", attempts);
        
        logEvent(event);
    }
    
    /**
     * Logs webhook dispatch.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param webhookUrl Webhook URL
     * @param eventType Webhook event type
     */
    public void logWebhookDispatch(
            UUID transactionId,
            UUID merchantId,
            String webhookUrl,
            String eventType) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.WEBHOOK_DISPATCH)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status("PENDING")
            .message("Webhook dispatch initiated: " + eventType)
            .timestamp(Instant.now())
            .build()
            .addDetail("webhook_url", maskingService.maskSensitiveValue(webhookUrl))
            .addDetail("event_type", eventType);
        
        logEvent(event);
    }
    
    /**
     * Logs webhook delivery success.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param retryCount Number of retries
     */
    public void logWebhookDelivered(
            UUID transactionId,
            UUID merchantId,
            int retryCount) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.WEBHOOK_DELIVERED)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status("SUCCESS")
            .message("Webhook delivered successfully")
            .timestamp(Instant.now())
            .build()
            .addDetail("retry_count", retryCount);
        
        logEvent(event);
    }
    
    /**
     * Logs webhook delivery failure.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param retryCount Number of retries
     * @param errorMessage Error message
     */
    public void logWebhookFailed(
            UUID transactionId,
            UUID merchantId,
            int retryCount,
            String errorMessage) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.WEBHOOK_FAILED)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status("FAILURE")
            .message("Webhook delivery failed: " + errorMessage)
            .timestamp(Instant.now())
            .build()
            .addDetail("retry_count", retryCount)
            .addDetail("error_message", errorMessage);
        
        logEvent(event);
    }
    
    /**
     * Logs risk evaluation.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param riskLevel Risk level (LOW, HIGH)
     * @param reason Risk evaluation reason
     */
    public void logRiskEvaluation(
            UUID transactionId,
            UUID merchantId,
            String riskLevel,
            String reason) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.RISK_EVALUATION)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status("SUCCESS")
            .message("Risk evaluation: " + riskLevel)
            .timestamp(Instant.now())
            .build()
            .addDetail("risk_level", riskLevel)
            .addDetail("reason", reason);
        
        logEvent(event);
    }
    
    /**
     * Logs merchant authentication.
     * 
     * @param merchantId Merchant ID
     * @param result Authentication result (SUCCESS, FAILURE)
     * @param reason Reason for result
     */
    public void logMerchantAuthentication(
            UUID merchantId,
            String result,
            String reason) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.MERCHANT_AUTHENTICATION)
            .merchantId(merchantId)
            .actor("system")
            .status(result)
            .message("Merchant authentication: " + result)
            .timestamp(Instant.now())
            .build()
            .addDetail("reason", reason);
        
        logEvent(event);
    }
    
    /**
     * Logs idempotency check.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param result Check result (HIT, MISS)
     */
    public void logIdempotencyCheck(
            UUID transactionId,
            UUID merchantId,
            String result) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.IDEMPOTENCY_CHECK)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status("SUCCESS")
            .message("Idempotency check: " + result)
            .timestamp(Instant.now())
            .build()
            .addDetail("result", result);
        
        logEvent(event);
    }
    
    /**
     * Logs duplicate request detection.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param reason Reason for duplicate detection
     */
    public void logDuplicateRequest(
            UUID transactionId,
            UUID merchantId,
            String reason) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.DUPLICATE_REQUEST)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status("SUCCESS")
            .message("Duplicate request detected: " + reason)
            .timestamp(Instant.now())
            .build()
            .addDetail("reason", reason);
        
        logEvent(event);
    }
    
    /**
     * Logs optimistic lock conflict.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param attemptNumber Attempt number
     */
    public void logOptimisticLockConflict(
            UUID transactionId,
            UUID merchantId,
            int attemptNumber) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.OPTIMISTIC_LOCK_CONFLICT)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status("RETRY")
            .message("Optimistic lock conflict, retrying")
            .timestamp(Instant.now())
            .build()
            .addDetail("attempt_number", attemptNumber);
        
        logEvent(event);
    }
    
    /**
     * Logs security alert.
     * 
     * @param merchantId Merchant ID
     * @param alertType Type of security alert
     * @param message Alert message
     */
    public void logSecurityAlert(
            UUID merchantId,
            String alertType,
            String message) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.SECURITY_ALERT)
            .merchantId(merchantId)
            .actor("system")
            .status("ALERT")
            .message(message)
            .timestamp(Instant.now())
            .build()
            .addDetail("alert_type", alertType);
        
        logEvent(event);
    }
    
    /**
     * Logs operator action.
     * 
     * @param transactionId Transaction ID
     * @param merchantId Merchant ID
     * @param operatorId Operator ID
     * @param action Action performed
     * @param reason Reason for action
     */
    public void logOperatorAction(
            UUID transactionId,
            UUID merchantId,
            UUID operatorId,
            String action,
            String reason) {
        
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.OPERATOR_ACTION)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("operator:" + operatorId)
            .status("SUCCESS")
            .message("Operator action: " + action)
            .timestamp(Instant.now())
            .build()
            .addDetail("action", action)
            .addDetail("reason", reason)
            .addDetail("operator_id", operatorId.toString());
        
        logEvent(event);
    }
    
    /**
     * Computes SHA256 checksum for audit event.
     * 
     * Checksum = SHA256(eventId + eventType + transactionId + merchantId + actor + status + timestamp)
     * 
     * @param event The audit event
     * @return SHA256 hex string (64 characters)
     */
    private String computeChecksum(StructuredAuditEvent event) {
        try {
            String checksumInput = 
                (event.getEventId() != null ? event.getEventId().toString() : "") +
                (event.getEventType() != null ? event.getEventType().name() : "") +
                (event.getTransactionId() != null ? event.getTransactionId().toString() : "") +
                (event.getMerchantId() != null ? event.getMerchantId().toString() : "") +
                (event.getActor() != null ? event.getActor() : "") +
                (event.getStatus() != null ? event.getStatus() : "") +
                (event.getTimestamp() != null ? event.getTimestamp().toString() : "");
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(checksumInput.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
