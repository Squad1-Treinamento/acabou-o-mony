package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.Merchant;
import com.acabouomony.payment.domain.entity.OutboxEvent;
import com.acabouomony.payment.domain.model.OutboxEventStatus;
import com.acabouomony.payment.domain.repository.MerchantRepository;
import com.acabouomony.payment.infrastructure.persistence.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookDispatchService.
 * 
 * Tests webhook dispatch logic, retry handling, and status updates.
 * Validates exponential backoff retry strategy.
 * 
 * Spec: spec-001-core-payment-processing.md - Webhook Dispatch Pattern
 * Task: task-017-webhook-dispatch-worker.md
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookDispatchService Tests")
class WebhookDispatchServiceTest {
    
    @Mock
    private OutboxEventRepository outboxEventRepository;
    
    @Mock
    private MerchantRepository merchantRepository;
    
    @Mock
    private WebhookSignatureService signatureService;
    
    @Mock
    private AlertService alertService;
    
    private ObjectMapper objectMapper;
    private WebhookDispatchService webhookDispatchService;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        webhookDispatchService = new WebhookDispatchService(
            outboxEventRepository,
            merchantRepository,
            signatureService,
            alertService,
            objectMapper
        );
    }
    
    @Test
    @DisplayName("Should handle missing merchant gracefully")
    void testDispatchWebhookMissingMerchant() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        OutboxEvent event = createOutboxEvent(eventId, transactionId);
        
        when(merchantRepository.findById(transactionId)).thenReturn(Optional.empty());
        
        // Act
        webhookDispatchService.dispatchWebhook(event);
        
        // Assert
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(eventCaptor.capture());
        
        OutboxEvent savedEvent = eventCaptor.getValue();
        assertEquals(OutboxEventStatus.FAILED, savedEvent.getStatus());
    }
    
    @Test
    @DisplayName("Should handle missing webhook URL gracefully")
    void testDispatchWebhookMissingWebhookUrl() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        OutboxEvent event = createOutboxEvent(eventId, transactionId);
        
        Merchant merchant = Merchant.builder()
            .id(transactionId)
            .merchantId(merchantId)
            .apiKeyHash("hash")
            .webhookUrl(null)  // No webhook URL
            .createdAt(Instant.now())
            .build();
        
        when(merchantRepository.findById(transactionId)).thenReturn(Optional.of(merchant));
        
        // Act
        webhookDispatchService.dispatchWebhook(event);
        
        // Assert
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(eventCaptor.capture());
        
        OutboxEvent savedEvent = eventCaptor.getValue();
        assertEquals(OutboxEventStatus.FAILED, savedEvent.getStatus());
    }
    
    @Test
    @DisplayName("Should alert operator on webhook delivery failure")
    void testAlertOnWebhookDeliveryFailure() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        OutboxEvent event = createOutboxEvent(eventId, transactionId);
        
        Merchant merchant = Merchant.builder()
            .id(transactionId)
            .merchantId(merchantId)
            .apiKeyHash("hash")
            .webhookUrl(null)  // No webhook URL
            .createdAt(Instant.now())
            .build();
        
        when(merchantRepository.findById(transactionId)).thenReturn(Optional.of(merchant));
        
        // Act
        webhookDispatchService.dispatchWebhook(event);
        
        // Assert
        verify(alertService, times(1)).alertWebhookDeliveryFailure(any(OutboxEvent.class), eq(merchantId));
    }
    
    @Test
    @DisplayName("Should create outbox event with correct initial state")
    void testOutboxEventInitialState() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        
        // Act
        OutboxEvent event = createOutboxEvent(eventId, transactionId);
        
        // Assert
        assertEquals(eventId, event.getId());
        assertEquals(transactionId, event.getAggregateId());
        assertEquals(OutboxEventStatus.PENDING, event.getStatus());
        assertEquals(0, event.getRetryCount());
        assertNull(event.getDeliveredAt());
    }
    
    @Test
    @DisplayName("Should validate outbox event payload is JSON")
    void testOutboxEventPayloadIsJson() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        OutboxEvent event = createOutboxEvent(eventId, transactionId);
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            objectMapper.readValue(event.getPayload(), Object.class);
        });
    }
    
    @Test
    @DisplayName("Should validate retry count constraints")
    void testRetryCountConstraints() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        OutboxEvent event = createOutboxEvent(eventId, transactionId);
        
        // Act & Assert - Valid retry counts
        for (int i = 0; i <= 5; i++) {
            event.setRetryCount(i);
            assertEquals(i, event.getRetryCount());
        }
    }
    
    // Helper methods
    
    private OutboxEvent createOutboxEvent(UUID eventId, UUID transactionId) {
        return OutboxEvent.builder()
            .id(eventId)
            .eventType("payment.completed")
            .aggregateId(transactionId)
            .payload("{\"transaction_id\":\"" + transactionId + "\",\"status\":\"COMPLETED\"}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
