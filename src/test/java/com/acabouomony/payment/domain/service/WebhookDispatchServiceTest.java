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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    
    @Mock
    private RestTemplate restTemplate;

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
            restTemplate,
            objectMapper
        );

        // Inject config properties for testing
        ReflectionTestUtils.setField(webhookDispatchService, "maxRetries", 3);
        ReflectionTestUtils.setField(webhookDispatchService, "backoffDelays", new long[]{10, 20, 30});
    }
    
    @Test
    @DisplayName("Should mark event as FAILED if merchant is not found")
    void testDispatchWebhookMissingMerchant() {
        // Arrange
        UUID merchantId = UUID.randomUUID();
        OutboxEvent event = createOutboxEvent(UUID.randomUUID(), UUID.randomUUID(), merchantId);
        
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.empty());
        
        // Act
        webhookDispatchService.dispatchWebhook(event);
        
        // Assert
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        
        OutboxEvent savedEvent = eventCaptor.getValue();
        assertEquals(OutboxEventStatus.FAILED, savedEvent.getStatus());
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }
    
    @Test
    @DisplayName("Should mark event as FAILED if webhook URL is missing")
    void testDispatchWebhookMissingWebhookUrl() {
        // Arrange
        UUID merchantId = UUID.randomUUID();
        OutboxEvent event = createOutboxEvent(UUID.randomUUID(), UUID.randomUUID(), merchantId);
        Merchant merchant = Merchant.builder().id(merchantId).webhookUrl(null).build();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));

        // Act
        webhookDispatchService.dispatchWebhook(event);
        
        // Assert
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        
        OutboxEvent savedEvent = eventCaptor.getValue();
        assertEquals(OutboxEventStatus.FAILED, savedEvent.getStatus());
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    @DisplayName("Should dispatch successfully on first attempt")
    void testSuccessfulDispatch() {
        // Arrange
        UUID merchantId = UUID.randomUUID();
        OutboxEvent event = createOutboxEvent(UUID.randomUUID(), UUID.randomUUID(), merchantId);
        Merchant merchant = Merchant.builder().id(merchantId).webhookUrl("http://example.com/webhook").webhookSecret("secret").build();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(signatureService.generateSignature(anyString(), anyString())).thenReturn("test-signature");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        // Act
        webhookDispatchService.dispatchWebhook(event);

        // Assert
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(String.class));
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        assertEquals(OutboxEventStatus.DELIVERED, eventCaptor.getValue().getStatus());
        verify(alertService, never()).alertWebhookDeliveryFailure(any(), any());
    }
    
    @Test
    @DisplayName("Should fail after all retries and send alert on server error")
    void testDispatchFailsAfterAllRetriesOnServerError() {
        // Arrange
        UUID merchantId = UUID.randomUUID();
        OutboxEvent event = createOutboxEvent(UUID.randomUUID(), UUID.randomUUID(), merchantId);
        Merchant merchant = Merchant.builder().id(merchantId).webhookUrl("http://example.com/webhook").webhookSecret("secret").build();
        
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(signatureService.generateSignature(anyString(), anyString())).thenReturn("test-signature");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        webhookDispatchService.dispatchWebhook(event);

        // Assert
        verify(restTemplate, times(3)).postForEntity(anyString(), any(), eq(String.class)); // 1 initial + 2 retries
        verify(alertService, times(1)).alertWebhookDeliveryFailure(event, merchantId);

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        // Called for each retry count update + final failure
        verify(outboxEventRepository, times(3)).save(eventCaptor.capture());
        assertEquals(OutboxEventStatus.FAILED, eventCaptor.getValue().getStatus());
    }

    // Helper methods
    private OutboxEvent createOutboxEvent(UUID eventId, UUID transactionId, UUID merchantId) {
        String payload = String.format(
            "{\"transaction_id\":\"%s\",\"merchantId\":\"%s\",\"status\":\"COMPLETED\",\"idempotencyKey\":\"some-key\"}",
            transactionId, merchantId
        );
        return OutboxEvent.builder()
            .id(eventId)
            .eventType("payment.completed")
            .aggregateId(transactionId)
            .payload(payload)
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
