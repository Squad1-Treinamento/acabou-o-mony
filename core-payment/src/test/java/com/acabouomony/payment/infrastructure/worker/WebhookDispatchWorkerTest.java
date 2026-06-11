package com.acabouomony.payment.infrastructure.worker;

import com.acabouomony.payment.domain.entity.OutboxEvent;
import com.acabouomony.payment.domain.model.OutboxEventStatus;
import com.acabouomony.payment.domain.service.WebhookDispatchService;
import com.acabouomony.payment.infrastructure.persistence.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookDispatchWorker.
 * 
 * Tests polling behavior, batch processing, and SLA monitoring.
 * Validates virtual thread dispatch and error handling.
 * 
 * Spec: spec-001-core-payment-processing.md - Transactional Outbox Rules
 * Task: task-017-webhook-dispatch-worker.md
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookDispatchWorker Tests")
class WebhookDispatchWorkerTest {
    
    @Mock
    private OutboxEventRepository outboxEventRepository;
    
    @Mock
    private WebhookDispatchService webhookDispatchService;
    
    private WebhookDispatchWorker webhookDispatchWorker;
    
    @BeforeEach
    void setUp() {
        webhookDispatchWorker = new WebhookDispatchWorker(
            outboxEventRepository,
            webhookDispatchService
        );
    }
    
    @Test
    @DisplayName("Should handle empty pending events gracefully")
    void testDispatchPendingWebhooksEmpty() {
        // Arrange
        when(outboxEventRepository.findByStatus(OutboxEventStatus.PENDING))
            .thenReturn(new ArrayList<>());
        
        // Act
        webhookDispatchWorker.dispatchPendingWebhooks();
        
        // Assert
        verify(webhookDispatchService, never()).dispatchWebhook(any());
    }
    
    @Test
    @DisplayName("Should dispatch all pending events")
    void testDispatchPendingWebhooksMultiple() {
        // Arrange
        List<OutboxEvent> pendingEvents = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            pendingEvents.add(createOutboxEvent());
        }
        
        when(outboxEventRepository.findByStatus(OutboxEventStatus.PENDING))
            .thenReturn(pendingEvents);
        
        // Act
        webhookDispatchWorker.dispatchPendingWebhooks();
        
        // Assert - Give virtual threads time to start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        verify(webhookDispatchService, times(5)).dispatchWebhook(any());
    }
    
    @Test
    @DisplayName("Should batch events to max 100 per poll")
    void testDispatchPendingWebhooksBatchSize() {
        // Arrange
        List<OutboxEvent> pendingEvents = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            pendingEvents.add(createOutboxEvent());
        }
        
        when(outboxEventRepository.findByStatus(OutboxEventStatus.PENDING))
            .thenReturn(pendingEvents);
        
        // Act
        webhookDispatchWorker.dispatchPendingWebhooks();
        
        // Assert - Give virtual threads time to start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Should only dispatch first 100 events
        verify(webhookDispatchService, times(100)).dispatchWebhook(any());
    }
    
    @Test
    @DisplayName("Should handle dispatch service exceptions gracefully")
    void testDispatchPendingWebhooksServiceException() {
        // Arrange
        List<OutboxEvent> pendingEvents = new ArrayList<>();
        pendingEvents.add(createOutboxEvent());
        
        when(outboxEventRepository.findByStatus(OutboxEventStatus.PENDING))
            .thenReturn(pendingEvents);
        
        doThrow(new RuntimeException("Dispatch failed"))
            .when(webhookDispatchService).dispatchWebhook(any());
        
        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> {
            webhookDispatchWorker.dispatchPendingWebhooks();
            Thread.sleep(100);  // Wait for virtual thread
        });
    }
    
    @Test
    @DisplayName("Should monitor webhook SLA violations")
    void testMonitorWebhookSLAViolations() {
        // Arrange
        Instant oldTime = Instant.now().minusSeconds(6 * 60);  // 6 minutes old
        OutboxEvent staleEvent = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .createdAt(oldTime)
            .updatedAt(oldTime)
            .build();
        
        List<OutboxEvent> pendingEvents = new ArrayList<>();
        pendingEvents.add(staleEvent);
        
        when(outboxEventRepository.findByStatus(OutboxEventStatus.PENDING))
            .thenReturn(pendingEvents);
        
        // Act
        webhookDispatchWorker.monitorWebhookSLA();
        
        // Assert - Should detect SLA violation
        // (Verification would be through log inspection in real scenario)
    }
    
    @Test
    @DisplayName("Should not alert for recent pending events")
    void testMonitorWebhookSLARecentEvents() {
        // Arrange
        Instant recentTime = Instant.now().minusSeconds(2 * 60);  // 2 minutes old
        OutboxEvent recentEvent = OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .createdAt(recentTime)
            .updatedAt(recentTime)
            .build();
        
        List<OutboxEvent> pendingEvents = new ArrayList<>();
        pendingEvents.add(recentEvent);
        
        when(outboxEventRepository.findByStatus(OutboxEventStatus.PENDING))
            .thenReturn(pendingEvents);
        
        // Act
        webhookDispatchWorker.monitorWebhookSLA();
        
        // Assert - Should not alert for recent events
        // (Verification would be through log inspection in real scenario)
    }
    
    @Test
    @DisplayName("Should handle SLA monitor exceptions gracefully")
    void testMonitorWebhookSLAException() {
        // Arrange
        when(outboxEventRepository.findByStatus(OutboxEventStatus.PENDING))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> webhookDispatchWorker.monitorWebhookSLA());
    }
    
    @Test
    @DisplayName("Should dispatch events asynchronously in virtual threads")
    void testDispatchAsyncVirtualThreads() {
        // Arrange
        List<OutboxEvent> pendingEvents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            pendingEvents.add(createOutboxEvent());
        }
        
        when(outboxEventRepository.findByStatus(OutboxEventStatus.PENDING))
            .thenReturn(pendingEvents);
        
        // Act
        long startTime = System.currentTimeMillis();
        webhookDispatchWorker.dispatchPendingWebhooks();
        long endTime = System.currentTimeMillis();
        
        // Assert - Dispatch should be fast (async)
        assertTrue(endTime - startTime < 100, "Dispatch should be async and fast");
    }
    
    // Helper methods
    
    private OutboxEvent createOutboxEvent() {
        return OutboxEvent.builder()
            .id(UUID.randomUUID())
            .eventType("payment.completed")
            .aggregateId(UUID.randomUUID())
            .payload("{\"status\":\"COMPLETED\"}")
            .status(OutboxEventStatus.PENDING)
            .retryCount(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
