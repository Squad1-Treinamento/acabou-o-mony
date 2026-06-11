package com.acabouomony.payment.domain.model;

/**
 * Enumeration of audit event types.
 * 
 * Categorizes different types of audit events for structured logging.
 * 
 * Spec: spec-001-core-payment-processing.md - Audit Log Persistence
 * Task: task-019-structured-audit-logging.md
 */
public enum AuditEventType {
    /**
     * Payment state transition (CREATED → VALIDATED → PROCESSING → COMPLETED, etc.)
     */
    PAYMENT_STATE_TRANSITION,
    
    /**
     * Payment request validation (payload validation, card validation, etc.)
     */
    PAYMENT_VALIDATION,
    
    /**
     * 3DS challenge initiated
     */
    CHALLENGE_INITIATED,
    
    /**
     * 3DS challenge completed
     */
    CHALLENGE_COMPLETED,
    
    /**
     * 3DS challenge failed
     */
    CHALLENGE_FAILED,
    
    /**
     * Reconciliation attempt
     */
    RECONCILIATION_ATTEMPT,
    
    /**
     * Reconciliation completed
     */
    RECONCILIATION_COMPLETED,
    
    /**
     * Webhook dispatch
     */
    WEBHOOK_DISPATCH,
    
    /**
     * Webhook delivery success
     */
    WEBHOOK_DELIVERED,
    
    /**
     * Webhook delivery failure
     */
    WEBHOOK_FAILED,
    
    /**
     * Risk evaluation
     */
    RISK_EVALUATION,
    
    /**
     * Merchant authentication
     */
    MERCHANT_AUTHENTICATION,
    
    /**
     * Idempotency check
     */
    IDEMPOTENCY_CHECK,
    
    /**
     * Duplicate request detected
     */
    DUPLICATE_REQUEST,
    
    /**
     * Optimistic lock conflict
     */
    OPTIMISTIC_LOCK_CONFLICT,
    
    /**
     * Security alert (suspicious activity)
     */
    SECURITY_ALERT,
    
    /**
     * Manual operator action
     */
    OPERATOR_ACTION
}
