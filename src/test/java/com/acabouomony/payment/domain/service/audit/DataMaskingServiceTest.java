package com.acabouomony.payment.domain.service.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataMaskingService.
 * 
 * Tests PII masking for various data types.
 * 
 * Spec: spec-001-core-payment-processing.md - Audit Log Persistence
 * Task: task-019-structured-audit-logging.md
 */
@DisplayName("DataMaskingService Tests")
class DataMaskingServiceTest {
    
    private DataMaskingService maskingService;
    
    @BeforeEach
    void setUp() {
        maskingService = new DataMaskingService();
    }
    
    // Card Number Masking Tests
    
    @Test
    @DisplayName("Should mask card number showing only last 4 digits")
    void testMaskCardNumber() {
        // Act
        String masked = maskingService.maskCardNumber("4111111111111111");
        
        // Assert
        assertEquals("****1111", masked);
    }
    
    @Test
    @DisplayName("Should mask short card number")
    void testMaskShortCardNumber() {
        // Act
        String masked = maskingService.maskCardNumber("1234");
        
        // Assert
        assertEquals("****", masked);
    }
    
    @Test
    @DisplayName("Should handle null card number")
    void testMaskNullCardNumber() {
        // Act
        String masked = maskingService.maskCardNumber(null);
        
        // Assert
        assertEquals("****", masked);
    }
    
    // Card Token Masking Tests
    
    @Test
    @DisplayName("Should mask card token showing first 4 and last 4 characters")
    void testMaskCardToken() {
        // Act
        String masked = maskingService.maskCardToken("card_token_abc123def456");
        
        // Assert
        assertEquals("card****456", masked);
    }
    
    @Test
    @DisplayName("Should mask short card token")
    void testMaskShortCardToken() {
        // Act
        String masked = maskingService.maskCardToken("short");
        
        // Assert
        assertEquals("****", masked);
    }
    
    // API Key Masking Tests
    
    @Test
    @DisplayName("Should mask API key showing only first 4 characters")
    void testMaskApiKey() {
        // Act
        String masked = maskingService.maskApiKey("sk_live_abc123def456");
        
        // Assert
        assertEquals("sk_l****", masked);
    }
    
    @Test
    @DisplayName("Should mask short API key")
    void testMaskShortApiKey() {
        // Act
        String masked = maskingService.maskApiKey("key");
        
        // Assert
        assertEquals("****", masked);
    }
    
    // Customer Name Masking Tests
    
    @Test
    @DisplayName("Should mask customer name showing first and last character")
    void testMaskCustomerName() {
        // Act
        String masked = maskingService.maskCustomerName("John Doe");
        
        // Assert
        assertEquals("J****e", masked);
    }
    
    @Test
    @DisplayName("Should mask short customer name")
    void testMaskShortCustomerName() {
        // Act
        String masked = maskingService.maskCustomerName("Jo");
        
        // Assert
        assertEquals("****", masked);
    }
    
    // Email Masking Tests
    
    @Test
    @DisplayName("Should mask email showing first character and domain")
    void testMaskEmail() {
        // Act
        String masked = maskingService.maskEmail("john.doe@example.com");
        
        // Assert
        assertEquals("j****@example.com", masked);
    }
    
    @Test
    @DisplayName("Should mask email with single character local part")
    void testMaskEmailSingleChar() {
        // Act
        String masked = maskingService.maskEmail("j@example.com");
        
        // Assert
        assertEquals("j****@example.com", masked);
    }
    
    @Test
    @DisplayName("Should handle invalid email")
    void testMaskInvalidEmail() {
        // Act
        String masked = maskingService.maskEmail("invalid");
        
        // Assert
        assertEquals("****", masked);
    }
    
    @Test
    @DisplayName("Should handle null email")
    void testMaskNullEmail() {
        // Act
        String masked = maskingService.maskEmail(null);
        
        // Assert
        assertEquals("****", masked);
    }
    
    // Phone Number Masking Tests
    
    @Test
    @DisplayName("Should mask phone number showing only last 4 digits")
    void testMaskPhoneNumber() {
        // Act
        String masked = maskingService.maskPhoneNumber("5511999999999");
        
        // Assert
        assertEquals("****9999", masked);
    }
    
    @Test
    @DisplayName("Should mask short phone number")
    void testMaskShortPhoneNumber() {
        // Act
        String masked = maskingService.maskPhoneNumber("1234");
        
        // Assert
        assertEquals("****", masked);
    }
    
    // Document Masking Tests
    
    @Test
    @DisplayName("Should mask document showing only last 4 digits")
    void testMaskDocument() {
        // Act
        String masked = maskingService.maskDocument("12345678901234");
        
        // Assert
        assertEquals("****1234", masked);
    }
    
    @Test
    @DisplayName("Should mask short document")
    void testMaskShortDocument() {
        // Act
        String masked = maskingService.maskDocument("123");
        
        // Assert
        assertEquals("****", masked);
    }
    
    // Generic Sensitive Value Masking Tests
    
    @Test
    @DisplayName("Should mask generic sensitive value showing first 4 characters")
    void testMaskSensitiveValue() {
        // Act
        String masked = maskingService.maskSensitiveValue("sensitive_data_value");
        
        // Assert
        assertEquals("sens****", masked);
    }
    
    @Test
    @DisplayName("Should mask short sensitive value")
    void testMaskShortSensitiveValue() {
        // Act
        String masked = maskingService.maskSensitiveValue("abc");
        
        // Assert
        assertEquals("****", masked);
    }
    
    @Test
    @DisplayName("Should handle null sensitive value")
    void testMaskNullSensitiveValue() {
        // Act
        String masked = maskingService.maskSensitiveValue(null);
        
        // Assert
        assertEquals("****", masked);
    }
}
