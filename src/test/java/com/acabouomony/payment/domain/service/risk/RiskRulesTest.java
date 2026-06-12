package com.acabouomony.payment.domain.service.risk;

import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for risk evaluation rules.
 * 
 * Tests individual risk rules in isolation.
 * 
 * Spec: spec-001-core-payment-processing.md - Risk Evaluation Rules
 * Task: task-018-risk-evaluation.md
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Risk Rules Tests")
class RiskRulesTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    // Amount Risk Rule Tests
    
    @Test
    @DisplayName("AmountRiskRule: Should flag high amount as high-risk")
    void testAmountRiskRuleHighAmount() {
        // Arrange
        AmountRiskRule rule = new AmountRiskRule(50000L);
        Transaction transaction = createTransaction(100000L);
        
        // Act
        boolean isHighRisk = rule.isHighRisk(transaction);
        
        // Assert
        assertTrue(isHighRisk);
        assertEquals("AmountRiskRule", rule.getName());
    }
    
    @Test
    @DisplayName("AmountRiskRule: Should flag low amount as low-risk")
    void testAmountRiskRuleLowAmount() {
        // Arrange
        AmountRiskRule rule = new AmountRiskRule(50000L);
        Transaction transaction = createTransaction(10000L);
        
        // Act
        boolean isHighRisk = rule.isHighRisk(transaction);
        
        // Assert
        assertFalse(isHighRisk);
    }
    
    @Test
    @DisplayName("AmountRiskRule: Should flag amount equal to threshold as low-risk")
    void testAmountRiskRuleEqualToThreshold() {
        // Arrange
        AmountRiskRule rule = new AmountRiskRule(50000L);
        Transaction transaction = createTransaction(50000L);
        
        // Act
        boolean isHighRisk = rule.isHighRisk(transaction);
        
        // Assert
        assertFalse(isHighRisk);
    }
    
    @Test
    @DisplayName("AmountRiskRule: Should handle null transaction")
    void testAmountRiskRuleNullTransaction() {
        // Arrange
        AmountRiskRule rule = new AmountRiskRule(50000L);
        
        // Act
        boolean isHighRisk = rule.isHighRisk(null);
        
        // Assert
        assertTrue(isHighRisk);  // Default to high-risk for safety
    }
    
    // New Card Risk Rule Tests
    
    @Test
    @DisplayName("NewCardRiskRule: Should flag new card as high-risk")
    void testNewCardRiskRuleNewCard() {
        // Arrange
        NewCardRiskRule rule = new NewCardRiskRule(transactionRepository);
        Transaction transaction = createTransaction(10000L);
        transaction.setCardTokenId("card_token_new");
        
        when(transactionRepository.countByCardTokenIdAndMerchantIdAndStatusCompleted(
            "card_token_new", transaction.getMerchantId())).thenReturn(0L);
        
        // Act
        boolean isHighRisk = rule.isHighRisk(transaction);
        
        // Assert
        assertTrue(isHighRisk);
        assertEquals("NewCardRiskRule", rule.getName());
    }
    
    @Test
    @DisplayName("NewCardRiskRule: Should flag used card as low-risk")
    void testNewCardRiskRuleUsedCard() {
        // Arrange
        NewCardRiskRule rule = new NewCardRiskRule(transactionRepository);
        Transaction transaction = createTransaction(10000L);
        transaction.setCardTokenId("card_token_used");
        
        when(transactionRepository.countByCardTokenIdAndMerchantIdAndStatusCompleted(
            "card_token_used", transaction.getMerchantId())).thenReturn(5L);
        
        // Act
        boolean isHighRisk = rule.isHighRisk(transaction);
        
        // Assert
        assertFalse(isHighRisk);
    }
    
    @Test
    @DisplayName("NewCardRiskRule: Should flag missing card token as high-risk")
    void testNewCardRiskRuleMissingCardToken() {
        // Arrange
        NewCardRiskRule rule = new NewCardRiskRule(transactionRepository);
        Transaction transaction = createTransaction(10000L);
        transaction.setCardTokenId(null);
        
        // Act
        boolean isHighRisk = rule.isHighRisk(transaction);
        
        // Assert
        assertTrue(isHighRisk);
    }
    
    @Test
    @DisplayName("NewCardRiskRule: Should handle database errors gracefully")
    void testNewCardRiskRuleDatabaseError() {
        // Arrange
        NewCardRiskRule rule = new NewCardRiskRule(transactionRepository);
        Transaction transaction = createTransaction(10000L);
        transaction.setCardTokenId("card_token");
        
        when(transactionRepository.countByCardTokenIdAndMerchantIdAndStatusCompleted(
            "card_token", transaction.getMerchantId()))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act
        boolean isHighRisk = rule.isHighRisk(transaction);
        
        // Assert
        assertTrue(isHighRisk);  // Default to high-risk on error
    }
    
    // Velocity Risk Rule Tests
    
    @Test
    @DisplayName("VelocityRiskRule: Should flag high velocity as high-risk")
    void testVelocityRiskRuleHighVelocity() {
        // Arrange
        VelocityRiskRule rule = new VelocityRiskRule(transactionRepository, 10, 5);
        Transaction transaction = createTransaction(10000L);
        
        // 60 transactions in 5 minutes = 12 per minute (exceeds threshold of 10)
        when(transactionRepository.countByMerchantIdAndCreatedAtAfter(
            transaction.getMerchantId(), any())).thenReturn(60L);
        
        // Act
        boolean isHighRisk = rule.isHighRisk(transaction);
        
        // Assert
        assertTrue(isHighRisk);
        assertEquals("VelocityRiskRule", rule.getName());
    }
    
    @Test
    @DisplayName("VelocityRiskRule: Should flag normal velocity as low-risk")
    void testVelocityRiskRuleNormalVelocity() {
        // Arrange
        VelocityRiskRule rule = new VelocityRiskRule(transactionRepository, 10, 5);
        Transaction transaction = createTransaction(10000L);
        
        // 30 transactions in 5 minutes = 6 per minute (below threshold of 10)
        when(transactionRepository.countByMerchantIdAndCreatedAtAfter(
            transaction.getMerchantId(), any())).thenReturn(30L);
        
        // Act
        boolean isHighRisk = rule.isHighRisk(transaction);
        
        // Assert
        assertFalse(isHighRisk);
    }
    
    @Test
    @DisplayName("VelocityRiskRule: Should flag velocity equal to threshold as low-risk")
    void testVelocityRiskRuleEqualToThreshold() {
        // Arrange
        VelocityRiskRule rule = new VelocityRiskRule(transactionRepository, 10, 5);
        Transaction transaction = createTransaction(10000L);
        
        // 50 transactions in 5 minutes = 10 per minute (equal to threshold)
        when(transactionRepository.countByMerchantIdAndCreatedAtAfter(
            transaction.getMerchantId(), any())).thenReturn(50L);
        
        // Act
        boolean isHighRisk = rule.isHighRisk(transaction);
        
        // Assert
        assertFalse(isHighRisk);
    }
    
    @Test
    @DisplayName("VelocityRiskRule: Should handle database errors gracefully")
    void testVelocityRiskRuleDatabaseError() {
        // Arrange
        VelocityRiskRule rule = new VelocityRiskRule(transactionRepository, 10, 5);
        Transaction transaction = createTransaction(10000L);
        
        when(transactionRepository.countByMerchantIdAndCreatedAtAfter(
            transaction.getMerchantId(), any()))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act
        boolean isHighRisk = rule.isHighRisk(transaction);
        
        // Assert
        assertTrue(isHighRisk);  // Default to high-risk on error
    }
    
    // Helper methods
    
    private Transaction createTransaction(long amount) {
        return Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(amount)
            .currency("BRL")
            .status(PaymentStatus.VALIDATED)
            .payloadHash("hash")
            .maskedCard("411111XXXXXX1111")
            .cardTokenId("card_token")
            .version(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
