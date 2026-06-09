package com.acabouomony.payment.infrastructure.worker;

import com.acabouomony.payment.domain.entity.OutboxEvent;
import com.acabouomony.payment.domain.model.OutboxEventStatus;
import com.acabouomony.payment.domain.service.WebhookDispatchService;
import com.acabouomony.payment.infrastructure.persistence.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Background worker for reliable webhook delivery to merchants.
 * 
 * Polls outbox table for PENDING webhook events and dispatches asynchronously.
 * Implements exponential backoff retry strategy with maximum 5 attempts.
 * Alerts operator on SLA violations (pending > 5 minutes).
 * 
 * Spec: spec-001-core-payment-processing.md - Transactional Outbox Rules
 * Task: task-017-webhook-dispatch-worker.md
 * 
 * Polling Strategy:
 * - Runs every 100ms (high frequency for low latency)
 * - Batch size: 100 events per poll
 * - Fire-and-forget: Submit to virtual thread executor
 * - Continue polling without waiting for dispatch completion
 * 
 * Retry Policy:
 * - Maximum 5 retry attempts
 * - Exponential backoff: 1s, 2s, 4s, 8s, 16s
 * - Total retry window: ~31 seconds
 * - After 5 retries: mark as FAILED and alert operator
 * 
 * SLA Monitoring:
 * - Alert if webhook pending > 5 minutes
 * - Investigate worker backlog or merchant endpoint issues
 * - Operator can manually trigger retry or investigate
 * 
 * Virtual Threads:
 * - Each webhook dispatch runs in separate virtual thread
 * - Non-blocking HTTP calls to merchant endpoints
 * - Scales to thousands of concurrent dispatches
 */
@Component
public class WebhookDispatchWorker {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookDispatchWorker.class);
    
    private static final int BATCH_SIZE = 100;
    private static final long SLA_THRESHOLD_MINUTES = 5;
    
    private final OutboxEventRepository outboxEventRepository;
    private final WebhookDispatchService webhookDispatchService;
    
    public WebhookDispatchWorker(
            OutboxEventRepository outboxEventRepository,
            WebhookDispatchService webhookDispatchService) {
        this.outboxEventRepository = outboxEventRepository;
        this.webhookDispatchService = webhookDispatchService;
    }
    
    /**
     * Polls and dispatches pending webhook events.
     * 
     * Runs every 100ms for low-latency webhook delivery.
     * Batches up to 100 events per poll for efficiency.
     * Submits each event to virtual thread executor for non-blocking dispatch.
     */
    @Scheduled(fixedRate = 100)
    public void dispatchPendingWebhooks() {
        try {
            // Query PENDING events (limit to batch size)
            List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(OutboxEventStatus.PENDING);
            
            if (pendingEvents.isEmpty()) {
                return;  // No work to do
            }
            
            // Limit to batch size
            int batchSize = Math.min(pendingEvents.size(), BATCH_SIZE);
            List<OutboxEvent> batch = pendingEvents.subList(0, batchSize);
            
            logger.debug("Found {} pending webhook events for dispatch", batch.size());
            
            // Process each event
            for (OutboxEvent event : batch) {
                dispatchAsync(event);
            }
            
        } catch (Exception e) {
            logger.error("Error in webhook dispatch worker: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Monitors for SLA violations (webhooks pending > 5 minutes).
     * 
     * Runs every 30 seconds to check for stale pending webhooks.
     * Alerts operator if any webhook exceeds SLA threshold.
     */
    @Scheduled(fixedRate = 30000)
    public void monitorWebhookSLA() {
        try {
            Instant slaThreshold = Instant.now().minusSeconds(SLA_THRESHOLD_MINUTES * 60);
            
            // Find all PENDING events older than SLA threshold
            List<OutboxEvent> stalePendingEvents = outboxEventRepository.findByStatus(OutboxEventStatus.PENDING)
                .stream()
                .filter(event -> event.getCreatedAt().isBefore(slaThreshold))
                .toList();
            
            if (!stalePendingEvents.isEmpty()) {
                logger.error("ALERT: WEBHOOK_SLA_VIOLATION - {} webhooks pending > {} minutes",
                    stalePendingEvents.size(), SLA_THRESHOLD_MINUTES);
                
                for (OutboxEvent event : stalePendingEvents) {
                    long ageMinutes = (Instant.now().toEpochMilli() - event.getCreatedAt().toEpochMilli()) / 60000;
                    logger.error("  - event_id={}, transaction_id={}, age_minutes={}, retry_count={}",
                        event.getId(), event.getAggregateId(), ageMinutes, event.getRetryCount());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in webhook SLA monitor: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Dispatches webhook event asynchronously.
     * 
     * Runs in virtual thread for non-blocking HTTP calls.
     * Handles retry logic and status updates.
     * 
     * @param event The outbox event to dispatch
     */
    private void dispatchAsync(OutboxEvent event) {
        // Submit to virtual thread executor
        Thread.startVirtualThread(() -> {
            try {
                logger.debug("Starting webhook dispatch: event_id={}, transaction_id={}, retry_count={}",
                    event.getId(), event.getAggregateId(), event.getRetryCount());
                
                // Dispatch webhook
                webhookDispatchService.dispatchWebhook(event);
                
                logger.info("Webhook dispatch completed: event_id={}, transaction_id={}",
                    event.getId(), event.getAggregateId());
                
            } catch (Exception e) {
                logger.error("Error dispatching webhook: event_id={}, transaction_id={}, error={}",
                    event.getId(), event.getAggregateId(), e.getMessage(), e);
            }
        });
    }
}
