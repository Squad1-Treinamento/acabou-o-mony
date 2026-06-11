package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.config.RiskEvaluationProperties;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.model.RiskLevel;
import com.acabouomony.payment.domain.service.risk.AmountRiskRule;
import com.acabouomony.payment.domain.service.risk.RiskRule;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for RiskEvaluationService.
 * 
 * Tests risk evaluation logic, rule combination, and configuration.
 * 
 * Spec: spec-001-core-payment-processing.md - Risk Evaluation Rules
 * Task: task-018-risk-evaluation.md
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskEvaluationService Tests")
class RiskEvaluationServiceTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    private RiskEvaluationProperties properties;
    private RiskEvaluationService riskEvaluationService;
    
    @BeforeEach
    void setUp() {
        properties = new RiskEvaluationProperties();
        properties.setEnabled(true);
        properties.setAmountThreshold(50000L);  // 500 BRL
        properties.setEnableNewCardCheck(true);
        properties.setEnableVelocityCheck(true);
        properties.setMaxTransactionsPerMinute(10);
        properties.setVelocityWindowMinutes(5);
        
        riskEvaluationService = new RiskEvaluationService(properties, transactionRepository);
    }
    
    @Test
    @DisplayName("Should evaluate low-risk transaction with low amount and card token")
    void testLowRiskTransaction() {
        // Arrange
        Transaction transaction = createTransaction(10000L, "card_token_123");
        when(transactionRepository.countByCardTokenIdAndMerchantIdAndStatusCompleted(
            "card_token_123", transaction.getMerchantId())).thenReturn(1L);
        when(transactionRepository.countByMerchantIdAndCreatedAtAfter(
            transaction.getMerchantId(), any())).thenReturn(5L);
        
        // Act
        RiskLevel riskLevel = riskEvaluationService.evaluateRisk(transaction);
        
        // Assert
        assertEquals(RiskLevel.LOW, riskLevel);
        assertTrue(riskEvaluationService.isLowRisk(transaction));
        assertFalse(riskEvaluationService.isHighRisk(transaction));
    }
    
    @Test
    @DisplayName("Should evaluate high-risk transaction with high amount")
    void testHighRiskTransactionHighAmount() {
        // Arrange
        Transaction transaction = createTransaction(100000L, "card_token_123");
        
        // Act
        RiskLevel riskLevel = riskEvaluationService.evaluateRisk(transaction);
        
        // Assert
        assertEquals(RiskLevel.HIGH, riskLevel);
        assertTrue(riskEvaluationService.isHighRisk(transaction));
        assertFalse(riskEvaluationService.isLowRisk(transaction));
    }
    
    @Test
    @DisplayName("Should evaluate high-risk transaction with new card")
    void testHighRiskTransactionNewCard() {
        // Arrange
        Transaction transaction = createTransaction(10000L, "card_token_new");
        when(transactionRepository.countByCardTokenIdAndMerchantIdAndStatusCompleted(
            "card_token_new", transaction.getMerchantId())).thenReturn(0L);
        
        // Act
        RiskLevel riskLevel = riskEvaluationService.evaluateRisk(transaction);
        
        // Assert
        assertEquals(RiskLevel.HIGH, riskLevel);
        assertTrue(riskEvaluationService.isHighRisk(transaction));
    }
    
    @Test
    @DisplayName("Should evaluate high-risk transaction with no card token")
    void testHighRiskTransactionNoCardToken() {
        // Arrange
        Transaction transaction = createTransaction(10000L, null);
        
        // Act
        RiskLevel riskLevel = riskEvaluationService.evaluateRisk(transaction);
        
        // Assert
        assertEquals(RiskLevel.HIGH, riskLevel);
        assertTrue(riskEvaluationService.isHighRisk(transaction));
    }
    
    @Test
    @DisplayName("Should evaluate high-risk transaction with high velocity")
    void testHighRiskTransactionHighVelocity() {
        // Arrange
        Transaction transaction = createTransaction(10000L, "card_token_123");
        when(transactionRepository.countByCardTokenIdAndMerchantIdAndStatusCompleted(
            "card_token_123", transaction.getMerchantId())).thenReturn(1L);
        // 60 transactions in 5 minutes = 12 per minute (exceeds threshold of 10)
        when(transactionRepository.countByMerchantIdAndCreatedAtAfter(
            transaction.getMerchantId(), any())).thenReturn(60L);
        
        // Act
        RiskLevel riskLevel = riskEvaluationService.evaluateRisk(transaction);
        
        // Assert
        assertEquals(RiskLevel.HIGH, riskLevel);
        assertTrue(riskEvaluationService.isHighRisk(transaction));
    }
    
    @Test
    @DisplayName("Should return HIGH-RISK for null transaction")
    void testNullTransaction() {
        // Act
        RiskLevel riskLevel = riskEvaluationService.evaluateRisk(null);
        
        // Assert
        assertEquals(RiskLevel.HIGH, riskLevel);
    }
    
    @Test
    @DisplayName("Should return LOW-RISK when risk evaluation disabled")
    void testRiskEvaluationDisabled() {
        // Arrange
        properties.setEnabled(false);
        riskEvaluationService = new RiskEvaluationService(properties, transactionRepository);
        Transaction transaction = createTransaction(100000L, null);
        
        // Act
        RiskLevel riskLevel = riskEvaluationService.evaluateRisk(transaction);
        
        // Assert
        assertEquals(RiskLevel.LOW, riskLevel);
    }
    
    @Test
    @DisplayName("Should allow adding custom risk rules")
    void testAddCustomRiskRule() {
        // Arrange
        RiskRule customRule = new RiskRule() {
            @Override
            public boolean isHighRisk(Transaction transaction) {
                return transaction.getAmount() > 1000000L;  // Very high amount
            }
            
            @Override
            public String getName() {
                return "CustomHighAmountRule";
            }
        };
        
        Transaction transaction = createTransaction(500000L, "card_token_123");
        when(transactionRepository.countByCardTokenIdAndMerchantIdAndStatusCompleted(
            "card_token_123", transaction.getMerchantId())).thenReturn(1L);
        when(transactionRepository.countByMerchantIdAndCreatedAtAfter(
            transaction.getMerchantId(), any())).thenReturn(5L);
        
        // Act
        riskEvaluationService.addRiskRule(customRule);
        RiskLevel riskLevel = riskEvaluationService.evaluateRisk(transaction);
        
        // Assert
        assertEquals(RiskLevel.LOW, riskLevel);  // Custom rule doesn't flag it
    }
    
    @Test
    @DisplayName("Should allow removing risk rules")
    void testRemoveRiskRule() {
        // Arrange
        int initialRuleCount = riskEvaluationService.getRiskRules().size();
        
        // Act
        riskEvaluationService.removeRiskRule("VelocityRiskRule");
        int finalRuleCount = riskEvaluationService.getRiskRules().size();
        
        // Assert
        assertEquals(initialRuleCount - 1, finalRuleCount);
    }
    
    @Test
    @DisplayName("Should handle exceptions in risk rules gracefully")
    void testRiskRuleException() {
        // Arrange
        properties.setEnableNewCardCheck(true);
        riskEvaluationService = new RiskEvaluationService(properties, transactionRepository);
        
        Transaction transaction = createTransaction(10000L, "card_token_123");
        when(transactionRepository.countByCardTokenIdAndMerchantIdAndStatusCompleted(
            "card_token_123", transaction.getMerchantId()))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act
        RiskLevel riskLevel = riskEvaluationService.evaluateRisk(transaction);
        
        // Assert - Should default to HIGH-RISK on error
        assertEquals(RiskLevel.HIGH, riskLevel);
    }
    
    @Test
    @DisplayName("Should respect configurable amount threshold")
    void testConfigurableAmountThreshold() {
        // Arrange
        properties.setAmountThreshold(100000L);  // 1000 BRL
        riskEvaluationService = new RiskEvaluationService(properties, transactionRepository);
        
        Transaction transaction = createTransaction(75000L, "card_token_123");
        when(transactionRepository.countByCardTokenIdAndMerchantIdAndStatusCompleted(
            "card_token_123", transaction.getMerchantId())).thenReturn(1L);
        when(transactionRepository.countByMerchantIdAndCreatedAtAfter(
            transaction.getMerchantId(), any())).thenReturn(5L);
        
        // Act
        RiskLevel riskLevel = riskEvaluationService.evaluateRisk(transaction);
        
        // Assert
        assertEquals(RiskLevel.LOW, riskLevel);  // Below new threshold
    }
    
    @Test
    @DisplayName("Should disable new card check when configured")
    void testDisableNewCardCheck() {
        // Arrange
        properties.setEnableNewCardCheck(false);
        riskEvaluationService = new RiskEvaluationService(properties, transactionRepository);
        
        Transaction transaction = createTransaction(10000L, null);  // No card token
        when(transactionRepository.countByMerchantIdAndCreatedAtAfter(
            transaction.getMerchantId(), any())).thenReturn(5L);
        
        // Act
        RiskLevel riskLevel = riskEvaluationService.evaluateRisk(transaction);
        
        // Assert
        assertEquals(RiskLevel.LOW, riskLevel);  // New card check disabled
    }
    
    @Test
    @DisplayName("Should disable velocity check when configured")
    void testDisableVelocityCheck() {
        // Arrange
        properties.setEnableVelocityCheck(false);
        riskEvaluationService = new RiskEvaluationService(properties, transactionRepository);
        
        Transaction transaction = createTransaction(10000L, "card_token_123");
        when(transactionRepository.countByCardTokenIdAndMerchantIdAndStatusCompleted(
            "card_token_123", transaction.getMerchantId())).thenReturn(1L);
        // High velocity would normally flag as high-risk
        when(transactionRepository.countByMerchantIdAndCreatedAtAfter(
            transaction.getMerchantId(), any())).thenReturn(100L);
        
        // Act
        RiskLevel riskLevel = riskEvaluationService.evaluateRisk(transaction);
        
        // Assert
        assertEquals(RiskLevel.LOW, riskLevel);  // Velocity check disabled
    }
    
    // Helper methods
    
    private Transaction createTransaction(long amount, String cardTokenId) {
        return Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(amount)
            .currency("BRL")
            .status(PaymentStatus.VALIDATED)
            .payloadHash("hash")
            .maskedCard("411111XXXXXX1111")
            .cardTokenId(cardTokenId)
            .version(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
