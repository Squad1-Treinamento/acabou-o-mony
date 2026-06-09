package com.acabouomony.payment.domain.service.audit;

import com.acabouomony.payment.domain.model.AuditEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for StructuredAuditLogger.
 * 
 * Tests audit event logging, checksum generation, and data masking.
 * 
 * Spec: spec-001-core-payment-processing.md - Audit Log Persistence
 * Task: task-019-structured-audit-logging.md
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StructuredAuditLogger Tests")
class StructuredAuditLoggerTest {
    
    @Mock
    private DataMaskingService maskingService;
    
    private ObjectMapper objectMapper;
    private StructuredAuditLogger auditLogger;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        maskingService = new DataMaskingService();  // Use real implementation
        auditLogger = new StructuredAuditLogger(maskingService, objectMapper);
    }
    
    @Test
    @DisplayName("Should log payment state transition event")
    void testLogPaymentStateTransition() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logPaymentStateTransition(
                transactionId,
                merchantId,
                "VALIDATED",
                "PROCESSING",
                "system"
            );
        });
    }
    
    @Test
    @DisplayName("Should log payment validation event")
    void testLogPaymentValidation() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logPaymentValidation(
                transactionId,
                merchantId,
                "SUCCESS",
                "Payment validation passed"
            );
        });
    }
    
    @Test
    @DisplayName("Should log challenge initiated event")
    void testLogChallengeInitiated() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logChallengeInitiated(
                transactionId,
                merchantId,
                "https://example.com/challenge"
            );
        });
    }
    
    @Test
    @DisplayName("Should log challenge completed event")
    void testLogChallengeCompleted() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logChallengeCompleted(
                transactionId,
                merchantId,
                "SUCCESS"
            );
        });
    }
    
    @Test
    @DisplayName("Should log reconciliation attempt event")
    void testLogReconciliationAttempt() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logReconciliationAttempt(
                transactionId,
                merchantId,
                1
            );
        });
    }
    
    @Test
    @DisplayName("Should log reconciliation completed event")
    void testLogReconciliationCompleted() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logReconciliationCompleted(
                transactionId,
                merchantId,
                "COMPLETED",
                3
            );
        });
    }
    
    @Test
    @DisplayName("Should log webhook dispatch event")
    void testLogWebhookDispatch() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logWebhookDispatch(
                transactionId,
                merchantId,
                "https://merchant.example.com/webhook",
                "payment.completed"
            );
        });
    }
    
    @Test
    @DisplayName("Should log webhook delivered event")
    void testLogWebhookDelivered() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logWebhookDelivered(
                transactionId,
                merchantId,
                0
            );
        });
    }
    
    @Test
    @DisplayName("Should log webhook failed event")
    void testLogWebhookFailed() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logWebhookFailed(
                transactionId,
                merchantId,
                5,
                "Max retries exceeded"
            );
        });
    }
    
    @Test
    @DisplayName("Should log risk evaluation event")
    void testLogRiskEvaluation() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logRiskEvaluation(
                transactionId,
                merchantId,
                "HIGH",
                "Amount exceeds threshold"
            );
        });
    }
    
    @Test
    @DisplayName("Should log merchant authentication event")
    void testLogMerchantAuthentication() {
        // Arrange
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logMerchantAuthentication(
                merchantId,
                "SUCCESS",
                "API key valid"
            );
        });
    }
    
    @Test
    @DisplayName("Should log idempotency check event")
    void testLogIdempotencyCheck() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logIdempotencyCheck(
                transactionId,
                merchantId,
                "HIT"
            );
        });
    }
    
    @Test
    @DisplayName("Should log duplicate request event")
    void testLogDuplicateRequest() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logDuplicateRequest(
                transactionId,
                merchantId,
                "Same idempotency key"
            );
        });
    }
    
    @Test
    @DisplayName("Should log optimistic lock conflict event")
    void testLogOptimisticLockConflict() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logOptimisticLockConflict(
                transactionId,
                merchantId,
                2
            );
        });
    }
    
    @Test
    @DisplayName("Should log security alert event")
    void testLogSecurityAlert() {
        // Arrange
        UUID merchantId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logSecurityAlert(
                merchantId,
                "SUSPICIOUS_ACTIVITY",
                "Multiple failed authentication attempts"
            );
        });
    }
    
    @Test
    @DisplayName("Should log operator action event")
    void testLogOperatorAction() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logOperatorAction(
                transactionId,
                merchantId,
                operatorId,
                "MANUAL_RETRY",
                "Customer requested retry"
            );
        });
    }
    
    @Test
    @DisplayName("Should create audit event with checksum")
    void testAuditEventWithChecksum() {
        // Arrange
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.PAYMENT_STATE_TRANSITION)
            .transactionId(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .actor("system")
            .status("SUCCESS")
            .message("Test event")
            .timestamp(Instant.now())
            .build();
        
        // Act
        assertDoesNotThrow(() -> auditLogger.logEvent(event));
        
        // Assert
        assertNotNull(event.getChecksum());
        assertEquals(64, event.getChecksum().length());  // SHA256 hex is 64 chars
    }
    
    @Test
    @DisplayName("Should add details to audit event")
    void testAuditEventWithDetails() {
        // Arrange
        StructuredAuditEvent event = StructuredAuditEvent.builder()
            .eventType(AuditEventType.PAYMENT_STATE_TRANSITION)
            .transactionId(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .actor("system")
            .status("SUCCESS")
            .message("Test event")
            .timestamp(Instant.now())
            .build()
            .addDetail("old_status", "VALIDATED")
            .addDetail("new_status", "PROCESSING");
        
        // Act
        assertDoesNotThrow(() -> auditLogger.logEvent(event));
        
        // Assert
        assertEquals("VALIDATED", event.getDetails().get("old_status"));
        assertEquals("PROCESSING", event.getDetails().get("new_status"));
    }
    
    @Test
    @DisplayName("Should generate consistent checksums for same event")
    void testChecksumConsistency() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        
        StructuredAuditEvent event1 = StructuredAuditEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(AuditEventType.PAYMENT_STATE_TRANSITION)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status("SUCCESS")
            .timestamp(timestamp)
            .build();
        
        StructuredAuditEvent event2 = StructuredAuditEvent.builder()
            .eventId(event1.getEventId())
            .eventType(AuditEventType.PAYMENT_STATE_TRANSITION)
            .transactionId(transactionId)
            .merchantId(merchantId)
            .actor("system")
            .status("SUCCESS")
            .timestamp(timestamp)
            .build();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logEvent(event1);
            auditLogger.logEvent(event2);
        });
        
        // Assert
        assertEquals(event1.getChecksum(), event2.getChecksum());
    }
    
    @Test
    @DisplayName("Should generate different checksums for different events")
    void testChecksumDifference() {
        // Arrange
        UUID transactionId1 = UUID.randomUUID();
        UUID transactionId2 = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        
        StructuredAuditEvent event1 = StructuredAuditEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(AuditEventType.PAYMENT_STATE_TRANSITION)
            .transactionId(transactionId1)
            .merchantId(merchantId)
            .actor("system")
            .status("SUCCESS")
            .timestamp(timestamp)
            .build();
        
        StructuredAuditEvent event2 = StructuredAuditEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(AuditEventType.PAYMENT_STATE_TRANSITION)
            .transactionId(transactionId2)
            .merchantId(merchantId)
            .actor("system")
            .status("SUCCESS")
            .timestamp(timestamp)
            .build();
        
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.logEvent(event1);
            auditLogger.logEvent(event2);
        });
        
        // Assert
        assertNotEquals(event1.getChecksum(), event2.getChecksum());
    }
}
