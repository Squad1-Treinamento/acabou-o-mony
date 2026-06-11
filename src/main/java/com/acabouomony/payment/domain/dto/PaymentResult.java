package com.acabouomony.payment.domain.dto;

import com.acabouomony.payment.domain.model.PaymentStatus;
import lombok.*;

import java.time.Instant;

/**
 * Result of payment submission to acquirer.
 * 
 * Contains acquirer reference, final status, and metadata.
 * 
 * Spec: spec-001-core-payment-processing.md - Mercado Pago Integration
 * Task: task-011-mercado-pago-client.md
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResult {
    
    /**
     * Acquirer's unique payment identifier.
     * 
     * For Mercado Pago: payment ID from response
     * Persisted in transaction.acquirer_reference
     * Used for reconciliation queries
     */
    private String acquirerReference;
    
    /**
     * Payment status from acquirer.
     * 
     * Values:
     * - COMPLETED: Payment approved
     * - DECLINED: Payment rejected
     * - FAILED: Internal error
     * - UNKNOWN: Timeout or network ambiguity
     */
    private PaymentStatus status;
    
    /**
     * Optional message from acquirer.
     * 
     * Examples:
     * - "Payment approved"
     * - "Insufficient funds"
     * - "Connection timeout"
     */
    private String message;
    
    /**
     * Timestamp when result received from acquirer.
     * 
     * ISO 8601 format
     */
    private Instant timestamp;
}
