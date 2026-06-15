package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.OutboxEvent;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.OutboxEventStatus;
import com.acabouomony.payment.infrastructure.persistence.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for creating outbox events for webhook notifications.
 * 
 * Outbox events are persisted in same transaction as payment update.
 * Webhook worker polls outbox table and dispatches events asynchronously.
 * 
 * Spec: spec-001-core-payment-processing.md - Transactional Outbox Rules
 * Task: task-016-outbox-persistence.md
 * 
 * Atomic Persistence:
 * - Outbox event created within same @Transactional method as transaction update
 * - If transaction update fails, outbox event creation rolls back
 * - If outbox event creation fails, transaction update rolls back
 * - All-or-nothing consistency guarantee
 * 
 * Outbox Event Lifecycle:
 * 1. Event created with status PENDING
 * 2. Webhook worker polls for PENDING events
 * 3. Worker dispatches event to merchant webhook endpoint
 * 4. On success (200 OK): mark as DELIVERED
 * 5. On failure: retry with exponential backoff (max 5 retries)
 * 6. After 5 retries: mark as FAILED and alert operator
 */
@Service
public class OutboxEventService {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxEventService.class);
    
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    public OutboxEventService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Creates outbox event for completed payment.
     * 
     * Called within same transaction as payment state update.
     * 
     * @param transaction The completed transaction
     */
    public void createPaymentCompletedEvent(Transaction transaction) {
        logger.debug("Creating payment.completed event: transaction_id={}", transaction.getId());
        
        Map<String, Object> payload = buildPaymentPayload(transaction);
        payload.put("event_type", "payment.completed");
        
        createOutboxEvent("payment.completed", transaction.getId(), payload);
    }
    
    /**
     * Creates outbox event for declined payment.
     * 
     * Called within same transaction as payment state update.
     * 
     * @param transaction The declined transaction
     */
    public void createPaymentDeclinedEvent(Transaction transaction) {
        logger.debug("Creating payment.declined event: transaction_id={}", transaction.getId());
        
        Map<String, Object> payload = buildPaymentPayload(transaction);
        payload.put("event_type", "payment.declined");
        
        createOutboxEvent("payment.declined", transaction.getId(), payload);
    }
    
    /**
     * Creates outbox event for failed payment.
     * 
     * Called within same transaction as payment state update.
     * 
     * @param transaction The failed transaction
     */
    public void createPaymentFailedEvent(Transaction transaction) {
        logger.debug("Creating payment.failed event: transaction_id={}", transaction.getId());
        
        Map<String, Object> payload = buildPaymentPayload(transaction);
        payload.put("event_type", "payment.failed");
        
        createOutboxEvent("payment.failed", transaction.getId(), payload);
    }
    
    /**
     * Creates outbox event for unknown payment (timeout).
     * 
     * Called within same transaction as payment state update.
     * 
     * @param transaction The unknown transaction
     */
    public void createPaymentUnknownEvent(Transaction transaction) {
        logger.debug("Creating payment.unknown event: transaction_id={}", transaction.getId());
        
        Map<String, Object> payload = buildPaymentPayload(transaction);
        payload.put("event_type", "payment.unknown");
        payload.put("message", "Payment outcome uncertain due to timeout. Reconciliation in progress.");
        
        createOutboxEvent("payment.unknown", transaction.getId(), payload);
    }
    
    /**
     * Creates outbox event for reconciliation completion.
     * 
     * Called within same transaction as payment state update.
     * 
     * @param transaction The reconciled transaction
     */
    public void createPaymentReconciledEvent(Transaction transaction) {
        logger.debug("Creating payment.reconciled event: transaction_id={}", transaction.getId());
        
        Map<String, Object> payload = buildPaymentPayload(transaction);
        payload.put("event_type", "payment.reconciled");
        payload.put("message", "Payment status resolved through reconciliation");
        
        createOutboxEvent("payment.reconciled", transaction.getId(), payload);
    }
    
    /**
     * Creates generic outbox event.
     * 
     * Persists event to database within current transaction.
     * If this method is called within a @Transactional context,
     * the event persists atomically with the parent transaction.
     * 
     * @param eventType The event type
     * @param aggregateId The transaction ID
     * @param payload The event payload
     */
    private void createOutboxEvent(String eventType, UUID aggregateId, Map<String, Object> payload) {
        try {
            // Serialize payload to JSON
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            // Create outbox event
            OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .eventType(eventType)
                .aggregateId(aggregateId)
                .payload(payloadJson)
                .status(OutboxEventStatus.PENDING)
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .signature("")
                .build();
            
            // Persist to database (within current transaction)
            outboxEventRepository.save(event);
            
            logger.info("Outbox event created: event_type={}, transaction_id={}, event_id={}",
                eventType, aggregateId, event.getId());
            
        } catch (Exception e) {
            logger.error("Error creating outbox event: event_type={}, transaction_id={}, error={}",
                eventType, aggregateId, e.getMessage(), e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }
    
    /**
     * Builds payment payload for webhook.
     * 
     * @param transaction The transaction
     * @return Payload map
     */
    private Map<String, Object> buildPaymentPayload(Transaction transaction) {
        Map<String, Object> payload = new HashMap<>();
        
        payload.put("transaction_id", transaction.getId().toString());
        payload.put("merchant_id", transaction.getMerchantId().toString());
        payload.put("idempotency_key", transaction.getIdempotencyKey().toString());
        payload.put("amount", transaction.getAmount());
        payload.put("currency", transaction.getCurrency());
        payload.put("status", transaction.getStatus().name());
        payload.put("masked_card", transaction.getMaskedCard());
        payload.put("acquirer_reference", transaction.getAcquirerReference());
        payload.put("created_at", transaction.getCreatedAt().toString());
        payload.put("updated_at", transaction.getUpdatedAt().toString());
        
        return payload;
    }
}
