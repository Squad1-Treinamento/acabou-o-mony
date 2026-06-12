package com.acabouomony.payment.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Wire format for Mercado Pago payment submission request.
 * 
 * Maps transaction data to Mercado Pago API format.
 * 
 * Spec: spec-001-core-payment-processing.md - Mercado Pago Integration
 * Task: task-011-mercado-pago-client.md
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MercadoPagoRequest {
    
    /**
     * Payment amount in cents.
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
    @JsonProperty("currency_id")
    private String currencyId;
    
    /**
     * Payment description.
     * 
     * Displayed to customer and in merchant dashboard
     */
    @JsonProperty("description")
    private String description;
    
    /**
     * Payer information.
     * 
     * Contains customer details
     */
    @JsonProperty("payer")
    private PayerInfo payer;
    
    /**
     * Payment method information.
     * 
     * Contains card token and payment method type
     */
    @JsonProperty("payment_method_id")
    private String paymentMethodId;
    
    /**
     * Card token from tokenization service.
     * 
     * Never contains raw PAN
     */
    @JsonProperty("token")
    private String token;
    
    /**
     * External reference for idempotency.
     * 
     * Mercado Pago uses this for duplicate detection
     * Set to transaction ID
     */
    @JsonProperty("external_reference")
    private String externalReference;
    
    /**
     * Notification URL for webhooks.
     * 
     * Mercado Pago sends payment status updates here
     */
    @JsonProperty("notification_url")
    private String notificationUrl;
    
    /**
     * Payer information nested object.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PayerInfo {
        
        /**
         * Customer email address.
         */
        @JsonProperty("email")
        private String email;
        
        /**
         * Customer first name.
         */
        @JsonProperty("first_name")
        private String firstName;
        
        /**
         * Customer last name.
         */
        @JsonProperty("last_name")
        private String lastName;
    }
}
