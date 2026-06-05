package com.acabouomony.payment.web.dto;

import com.acabouomony.payment.domain.model.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for payment requests.
 * 
 * Returned to client after payment processing.
 * Contains transaction details and current status.
 * 
 * Spec: spec-001-core-payment-processing.md - Duplicate Request Rules
 * Task: task-009-db-idempotency-enforcement.md
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {
    
    /**
     * Unique transaction identifier.
     * 
     * Used for:
     * - Polling transaction status
     * - Webhook correlation
     * - Merchant reconciliation
     */
    @JsonProperty("transaction_id")
    private UUID transactionId;
    
    /**
     * Current payment status.
     * 
     * Values:
     * - CREATED: Request initialized
     * - VALIDATED: Payload validated
     * - CHALLENGE_PENDING: Awaiting 3DS
     * - AUTHENTICATED: 3DS complete
     * - PROCESSING: Payment in progress
     * - UNKNOWN: Outcome uncertain (timeout)
     * - COMPLETED: Payment approved
     * - DECLINED: Payment rejected
     * - FAILED: Internal error
     */
    @JsonProperty("status")
    private PaymentStatus status;
    
    /**
     * Transaction amount in cents.
     * 
     * Example: 10000 = $100.00
     */
    @JsonProperty("amount")
    private Long amount;
    
    /**
     * ISO 4217 currency code.
     * 
     * Example: "BRL", "USD"
     */
    @JsonProperty("currency")
    private String currency;
    
    /**
     * Masked card number for customer reference.
     * 
     * Format: 411111XXXXXX1111
     * Never contains raw PAN
     * Optional (may be null for some payment methods)
     */
    @JsonProperty("masked_card")
    private String maskedCard;
    
    /**
     * Human-readable message for client.
     * 
     * Examples:
     * - "Payment processing"
     * - "Payment approved"
     * - "Payment declined"
     * - "Payment outcome uncertain, please check status"
     * 
     * Optional (may be null)
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * Timestamp when transaction was created.
     * 
     * ISO 8601 format
     */
    @JsonProperty("created_at")
    private Instant createdAt;
    
    /**
     * Timestamp when transaction was last updated.
     * 
     * ISO 8601 format
     */
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    /**
     * Idempotency key for duplicate detection.
     * 
     * Returned to client for reference.
     * Can be used to poll transaction status.
     * Optional (may be null)
     */
    @JsonProperty("idempotency_key")
    private UUID idempotencyKey;
}
