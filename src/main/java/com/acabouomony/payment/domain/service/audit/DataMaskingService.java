package com.acabouomony.payment.domain.service.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for masking personally identifiable information (PII) in audit logs.
 * 
 * Prevents sensitive data from appearing in logs while maintaining readability.
 * 
 * Spec: spec-001-core-payment-processing.md - Audit Log Persistence
 * Task: task-019-structured-audit-logging.md
 * 
 * Masking Rules:
 * - Card numbers (PAN): Show only last 4 digits
 * - Card tokens: Mask all but first 4 and last 4 characters
 * - API keys: Mask all but first 4 characters
 * - Customer names: Mask all but first and last character
 * - Email addresses: Mask domain part

 * - Phone numbers: Show only last 4 digits
 * - CPF/CNPJ: Show only last 4 digits
 */
@Service
public class DataMaskingService {
    


    private static final Logger logger = LoggerFactory.getLogger(DataMaskingService.class);
    
    /**
     * Masks card number (PAN).
     * 
     * Shows only last 4 digits.
     * Example: 4111111111111111 → ****1111
     * 
     * @param cardNumber The card number to mask
     * @return Masked card number
     */
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return "****";
        }
        
        if (cardNumber.length() <= 4) {
            return "****";
        }
        
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "****" + lastFour;
    }
    
    /**
     * Masks card token.
     * 
     * Shows first 4 and last 4 characters.
     * Example: card_token_abc123def456 → card****456
     * 
     * @param cardToken The card token to mask
     * @return Masked card token
     */
    public String maskCardToken(String cardToken) {
        if (cardToken == null || cardToken.length() <= 8) {
            return "****";
        }
        
        String first4 = cardToken.substring(0, 4);
        String last4 = cardToken.substring(cardToken.length() - 4);
        return first4 + "****" + last4;
    }
    
    /**
     * Masks API key.
     * 
     * Shows only first 4 characters.
     * Example: sk_live_abc123def456 → sk_l****
     * 
     * @param apiKey The API key to mask
     * @return Masked API key
     */
    public String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) {
            return "****";
        }
        
        String first4 = apiKey.substring(0, 4);
        return first4 + "****";
    }
    
    /**
     * Masks customer name.
     * 
     * Shows first and last character.
     * Example: John Doe → J****e
     * 
     * @param name The name to mask
     * @return Masked name
     */
    public String maskCustomerName(String name) {
        if (name == null || name.length() <= 2) {
            return "****";
        }
        
        String first = name.substring(0, 1);
        String last = name.substring(name.length() - 1);
        return first + "****" + last;
    }
    
    /**
     * Masks email address.
     * 
     * Shows first character and domain.
     * Example: john.doe@example.com → j****@example.com
     * 
     * @param email The email address to mask
     * @return Masked email address
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return "****";
        }
        
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() == 0) {
            return "****@" + domain;
        }
        
        String first = localPart.substring(0, 1);
        return first + "****@" + domain;
    }
    
    /**
     * Masks phone number.
     * 
     * Shows only last 4 digits.
     * Example: 5511999999999 → ****9999
     * 
     * @param phoneNumber The phone number to mask
     * @return Masked phone number
     */
    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 4) {
            return "****";
        }
        
        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        return "****" + lastFour;
    }
    
    /**
     * Masks CPF/CNPJ.
     * 
     * Shows only last 4 digits.
     * Example: 12345678901234 → ****1234
     * 
     * @param document The document number to mask
     * @return Masked document number
     */
    public String maskDocument(String document) {
        if (document == null || document.length() <= 4) {
            return "****";
        }
        
        String lastFour = document.substring(document.length() - 4);
        return "****" + lastFour;
    }
    
    /**
     * Masks generic sensitive string.
     * 
     * Shows only first 4 characters.
     * 
     * @param value The value to mask
     * @return Masked value
     */
    public String maskSensitiveValue(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        
        String first4 = value.substring(0, 4);
        return first4 + "****";
    }
}

