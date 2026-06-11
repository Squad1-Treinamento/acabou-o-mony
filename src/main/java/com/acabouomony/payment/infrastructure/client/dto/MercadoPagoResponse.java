package com.acabouomony.payment.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Wire format for Mercado Pago payment submission response.
 * 
 * Parsed from Mercado Pago API response.
 * 
 * Spec: spec-001-core-payment-processing.md - Mercado Pago Integration
 * Task: task-011-mercado-pago-client.md
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MercadoPagoResponse {
    
    /**
     * Mercado Pago payment ID.
     * 
     * Unique identifier for this payment at Mercado Pago
     * Persisted as transaction.acquirer_reference
     * Used for reconciliation queries
     */
    @JsonProperty("id")
    private String id;
    
    /**
     * Payment status.
     * 
     * Values:
     * - "approved": Payment successful
     * - "rejected": Payment declined
     * - "pending": Awaiting processing
     * - "cancelled": Payment cancelled
     * - "refunded": Payment refunded
     */
    @JsonProperty("status")
    private String status;
    
    /**
     * Detailed status information.
     * 
     * Examples:
     * - "accredited": Funds received
     * - "pending_contingency": Awaiting manual review
     * - "cc_rejected_insufficient_amount": Insufficient funds
     */
    @JsonProperty("status_detail")
    private String statusDetail;
    
    /**
     * Amount processed.
     * 
     * In cents
     */
    @JsonProperty("transaction_amount")
    private Long transactionAmount;
    
    /**
     * Currency code.
     * 
     * ISO 4217 code
     */
    @JsonProperty("currency_id")
    private String currencyId;
    
    /**
     * Creation timestamp.
     * 
     * ISO 8601 format
     */
    @JsonProperty("date_created")
    private String dateCreated;
    
    /**
     * Last modification timestamp.
     * 
     * ISO 8601 format
     */
    @JsonProperty("date_last_updated")
    private String dateLastUpdated;
    
    /**
     * External reference provided in request.
     * 
     * Echoed back from request
     */
    @JsonProperty("external_reference")
    private String externalReference;
    
    /**
     * Authorization code from acquirer.
     * 
     * Only present for approved payments
     */
    @JsonProperty("authorization_code")
    private String authorizationCode;
}
