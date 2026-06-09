package com.acabouomony.payment.domain.service.audit;

import com.acabouomony.payment.domain.model.AuditEventType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Structured audit event for comprehensive logging.
 * 
 * Captures all relevant information about an audit event in a structured format.
 * Supports flexible key-value pairs for event-specific data.
 * 
 * Spec: spec-001-core-payment-processing.md - Audit Log Persistence
 * Task: task-019-structured-audit-logging.md
 * 
 * Fields:
 * - eventId: Unique event identifier
 * - eventType: Type of event (PAYMENT_STATE_TRANSITION, WEBHOOK_DISPATCH, etc.)
 * - transactionId: Associated transaction ID
 * - merchantId: Associated merchant ID
 * - actor: Who performed the action (system, webhook, reconciliation, operator)
 * - status: Event status (SUCCESS, FAILURE, PENDING)
 * - message: Human-readable message
 * - details: Event-specific key-value pairs
 * - checksum: SHA256 checksum for tamper detection
 * - timestamp: When the event occurred
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StructuredAuditEvent {
    
    /**
     * Unique event identifier (UUID).
     */
    private UUID eventId;
    
    /**
     * Type of audit event.
     */
    private AuditEventType eventType;
    
    /**
     * Associated transaction ID (nullable for non-transaction events).
     */
    private UUID transactionId;
    
    /**
     * Associated merchant ID.
     */
    private UUID merchantId;
    
    /**
     * Actor performing the action.
     * Examples: "system", "webhook", "reconciliation", "operator", "merchant_api"
     */
    private String actor;
    
    /**
     * Event status.
     * Examples: "SUCCESS", "FAILURE", "PENDING", "RETRY"
     */
    private String status;
    
    /**
     * Human-readable message describing the event.
     */
    private String message;
    
    /**
     * Event-specific details (key-value pairs).
     * Examples:
     * - "old_status": "VALIDATED"
     * - "new_status": "PROCESSING"
     * - "retry_count": "2"
     * - "error_code": "TIMEOUT"
     * - "duration_ms": "1234"
     */
    @Builder.Default
    private Map<String, Object> details = new HashMap<>();
    
    /**
     * SHA256 checksum for tamper detection.
     * Computed from: eventId + eventType + transactionId + merchantId + actor + status + timestamp
     */
    private String checksum;
    
    /**
     * When the event occurred.
     */
    private Instant timestamp;
    
    /**
     * Adds a detail to the event.
     * 
     * @param key The detail key
     * @param value The detail value
     * @return This event (for chaining)
     */
    public StructuredAuditEvent addDetail(String key, Object value) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.put(key, value);
        return this;
    }
    
    /**
     * Adds multiple details to the event.
     * 
     * @param detailsMap Map of details to add
     * @return This event (for chaining)
     */
    public StructuredAuditEvent addDetails(Map<String, Object> detailsMap) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.putAll(detailsMap);
        return this;
    }
}
