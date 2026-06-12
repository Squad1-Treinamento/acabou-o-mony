package com.acabouomony.payment.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Payment request DTO with comprehensive validation.
 * 
 * Validates:
 * - amount (positive, non-zero)
 * - currency (ISO 4217, exactly 3 characters)
 * - idempotency_key (UUID format)
 * - payment_method (required, contains card_token_id)
 * - customer_id (optional but recommended)
 * 
 * Spec: spec-001-core-payment-processing.md - Request Validation Rules
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    
    /**
     * Transaction amount in cents (e.g., 1000 = $10.00).
     * 
     * Validation:
     * - Must be positive (> 0)
     * - Must not be null
     * - Prevents zero or negative amounts
     */
    @NotNull(message = "amount cannot be null")
    @Min(value = 1, message = "amount must be greater than 0 (in cents)")
    @JsonProperty("amount")
    private Long amount;
    
    /**
     * ISO 4217 currency code (e.g., "USD", "BRL").
     * 
     * Validation:
     * - Must be exactly 3 characters
     * - Must not be null
     * - Uppercase enforcement via custom validator
     */
    @NotNull(message = "currency cannot be null")
    @Size(min = 3, max = 3, message = "currency must be exactly 3 characters (ISO 4217)")
    @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be 3 uppercase letters (ISO 4217)")
    @JsonProperty("currency")
    private String currency;
    
    /**
     * Idempotency key for duplicate request detection.
     * 
     * Validation:
     * - Must be valid UUID format
     * - Must not be null
     * - Prevents duplicate charges
     * 
     * Spec: Idempotency-Key header or request body field
     */
    @NotNull(message = "idempotency_key cannot be null")
    @JsonProperty("idempotency_key")
    private UUID idempotencyKey;
    
    /**
     * Payment method details (card token, masked card, etc.).
     * 
     * Validation:
     * - Must not be null
     * - Must contain card_token_id
     * - Nested validation via @Valid
     */
    @NotNull(message = "payment_method cannot be null")
    @Valid
    @JsonProperty("payment_method")
    private PaymentMethod paymentMethod;
    
    /**
     * Customer identifier (optional but recommended).
     * 
     * Used for:
     * - Risk evaluation (velocity checks)
     * - Fraud detection
     * - Customer history
     */
    @JsonProperty("customer_id")
    private UUID customerId;
    
    /**
     * Customer email (optional).
     * 
     * Used for:
     * - Webhook notifications
     * - Customer communication
     */
    @Email(message = "customer_email must be valid email format")
    @JsonProperty("customer_email")
    private String customerEmail;
    
    /**
     * Nested payment method details.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentMethod {
        
        /**
         * Card token ID (from tokenization service).
         * 
         * Validation:
         * - Must not be null
         * - Must not be empty
         * - Must be non-blank
         * 
         * Never contains raw PAN (card number).
         */
        @NotNull(message = "card_token_id cannot be null")
        @NotBlank(message = "card_token_id cannot be blank")
        @Size(min = 1, max = 100, message = "card_token_id must be between 1 and 100 characters")
        @JsonProperty("card_token_id")
        private String cardTokenId;
        
        /**
         * Masked card number (e.g., "411111XXXXXX1111").
         * 
         * Validation:
         * - Optional (can be null)
         * - If provided, must match format: 6 digits + X's + 4 digits
         * 
         * Used for:
         * - Customer reference
         * - UI display
         * - Never contains raw PAN
         */
        @Pattern(
            regexp = "^[0-9]{6}X{6,8}[0-9]{4}$",
            message = "masked_card must match format: 6 digits + X's + 4 digits (e.g., 411111XXXXXX1111)"
        )
        @JsonProperty("masked_card")
        private String maskedCard;
    }
}
