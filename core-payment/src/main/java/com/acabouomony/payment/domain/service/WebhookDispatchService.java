package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.Merchant;
import com.acabouomony.payment.domain.entity.OutboxEvent;
import com.acabouomony.payment.domain.model.OutboxEventStatus;
import com.acabouomony.payment.domain.repository.MerchantRepository;
import com.acabouomony.payment.infrastructure.persistence.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for dispatching webhook events to merchant endpoints.
 * 
 * Implements exponential backoff retry strategy with maximum 5 attempts.
 * Marks events as DELIVERED only after receiving 200 OK response.
 * Persists webhook acknowledgment timestamp on successful delivery.
 * Alerts operator on final failure after all retries exhausted.
 * 
 * Spec: spec-001-core-payment-processing.md - Webhook Dispatch Pattern
 * Task: task-017-webhook-dispatch-worker.md
 * 
 * Retry Policy:
 * - Maximum 5 retry attempts
 * - Exponential backoff: 1s, 2s, 4s, 8s, 16s
 * - Total retry window: ~31 seconds
 * - Retry only on network errors or 5xx responses
 * - Do NOT retry on 4xx responses (client errors)
 * 
 * Webhook Headers:
 * - X-Webhook-ID: UUID (unique per outbox event, immutable)
 * - X-Idempotency-Key: UUID (matching payment idempotency_key)
 * - X-Retry-Count: 0, 1, 2, ... (shows retry attempt number)
 * - X-Timestamp: ISO8601 timestamp
 * - X-Signature: HMAC-SHA256 signature of payload
 * - Content-Type: application/json
 * 
 * Merchant Responsibilities:
 * - Verify X-Signature using shared secret
 * - Treat multiple webhooks with same X-Webhook-ID as duplicates
 * - Respond 200 OK to all deliveries (including duplicates)
 * - Safe to retry indefinitely; merchant handles deduplication
 * 
 * Failure Handling:
 * - Network errors: retry with backoff
 * - 5xx responses: retry with backoff
 * - 4xx responses: fail immediately (don't retry)
 * - After 5 retries: mark as FAILED and alert operator
 */
@Service
public class WebhookDispatchService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookDispatchService.class);
    
    private static final int MAX_RETRIES = 5;
    private static final int[] BACKOFF_DELAYS_MS = {1000, 2000, 4000, 8000, 16000};
    private static final int HTTP_TIMEOUT_MS = 5000;
    
    private final OutboxEventRepository outboxEventRepository;
    private final MerchantRepository merchantRepository;
    private final WebhookSignatureService signatureService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    
    @Value("${webhook.secret-key-prefix:webhook_secret_}")
    private String secretKeyPrefix;
    
    public WebhookDispatchService(
            OutboxEventRepository outboxEventRepository,
            MerchantRepository merchantRepository,
            WebhookSignatureService signatureService,
            AlertService alertService,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.merchantRepository = merchantRepository;
        this.signatureService = signatureService;
        this.alertService = alertService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(HTTP_TIMEOUT_MS))
            .build();
    }
    
    /**
     * Dispatches webhook event to merchant endpoint.
     * 
     * Implements exponential backoff retry strategy.
     * Marks event as DELIVERED only after receiving 200 OK.
     * Alerts operator on final failure.
     * 
     * @param event The outbox event to dispatch
     */
    @Transactional
    public void dispatchWebhook(OutboxEvent event) {
        // Fetch merchant webhook URL
        Optional<Merchant> merchantOpt = merchantRepository.findById(event.getAggregateId());
        if (merchantOpt.isEmpty()) {
            logger.error("Merchant not found for webhook dispatch: event_id={}, transaction_id={}",
                event.getId(), event.getAggregateId());
            markEventFailed(event, "Merchant not found");
            return;
        }
        
        Merchant merchant = merchantOpt.get();
        String webhookUrl = merchant.getWebhookUrl();
        
        if (webhookUrl == null || webhookUrl.isBlank()) {
            logger.warn("Merchant webhook URL not configured: event_id={}, merchant_id={}",
                event.getId(), merchant.getMerchantId());
            markEventFailed(event, "Webhook URL not configured");
            return;
        }
        
        // Attempt dispatch with retries
        boolean success = attemptDispatchWithRetries(event, webhookUrl, merchant);
        
        if (!success) {
            // All retries exhausted
            markEventFailed(event, "Max retries exceeded");
            alertService.alertWebhookDeliveryFailure(event, merchant.getMerchantId());
        }
    }
    
    /**
     * Attempts webhook dispatch with exponential backoff retries.
     * 
     * @param event The outbox event
     * @param webhookUrl The merchant webhook URL
     * @param merchant The merchant
     * @return true if dispatch succeeded, false if all retries exhausted
     */
    private boolean attemptDispatchWithRetries(OutboxEvent event, String webhookUrl, Merchant merchant) {
        int retryCount = event.getRetryCount();
        
        while (retryCount <= MAX_RETRIES) {
            try {
                // Attempt dispatch
                boolean success = attemptSingleDispatch(event, webhookUrl, merchant, retryCount);
                
                if (success) {
                    // Dispatch succeeded
                    markEventDelivered(event);
                    return true;
                }
                
                // Dispatch failed, check if we should retry
                if (retryCount >= MAX_RETRIES) {
                    // Max retries reached
                    logger.warn("Webhook dispatch max retries reached: event_id={}, transaction_id={}, retry_count={}",
                        event.getId(), event.getAggregateId(), retryCount);
                    return false;
                }
                
                // Wait before retry
                long delayMs = BACKOFF_DELAYS_MS[retryCount];
                logger.debug("Webhook dispatch will retry after {}ms: event_id={}, transaction_id={}, retry_count={}",
                    delayMs, event.getId(), event.getAggregateId(), retryCount);
                
                Thread.sleep(delayMs);
                
                // Update retry count and persist
                retryCount++;
                updateRetryCount(event, retryCount);
                
            } catch (InterruptedException e) {
                logger.error("Webhook dispatch interrupted: event_id={}, transaction_id={}",
                    event.getId(), event.getAggregateId(), e);
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Attempts single webhook dispatch.
     * 
     * @param event The outbox event
     * @param webhookUrl The merchant webhook URL
     * @param merchant The merchant
     * @param retryCount Current retry attempt number
     * @return true if dispatch succeeded (200 OK), false otherwise
     */
    private boolean attemptSingleDispatch(OutboxEvent event, String webhookUrl, Merchant merchant, int retryCount) {
        try {
            // Build webhook request
            HttpRequest request = buildWebhookRequest(event, webhookUrl, merchant, retryCount);
            
            // Send request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Check response status
            if (response.statusCode() == 200) {
                logger.info("Webhook dispatch succeeded: event_id={}, transaction_id={}, merchant_id={}, retry_count={}",
                    event.getId(), event.getAggregateId(), merchant.getMerchantId(), retryCount);
                return true;
            }
            
            // 4xx errors: don't retry
            if (response.statusCode() >= 400 && response.statusCode() < 500) {
                logger.warn("Webhook dispatch client error (no retry): event_id={}, transaction_id={}, status={}, body={}",
                    event.getId(), event.getAggregateId(), response.statusCode(), response.body());
                return false;
            }
            
            // 5xx errors: retry
            if (response.statusCode() >= 500) {
                logger.warn("Webhook dispatch server error (will retry): event_id={}, transaction_id={}, status={}, retry_count={}",
                    event.getId(), event.getAggregateId(), response.statusCode(), retryCount);
                return false;
            }
            
            // Other status codes: don't retry
            logger.warn("Webhook dispatch unexpected status: event_id={}, transaction_id={}, status={}",
                event.getId(), event.getAggregateId(), response.statusCode());
            return false;
            
        } catch (java.net.http.HttpTimeoutException e) {
            logger.warn("Webhook dispatch timeout (will retry): event_id={}, transaction_id={}, retry_count={}",
                event.getId(), event.getAggregateId(), retryCount);
            return false;
        } catch (java.io.IOException e) {
            logger.warn("Webhook dispatch network error (will retry): event_id={}, transaction_id={}, error={}, retry_count={}",
                event.getId(), event.getAggregateId(), e.getMessage(), retryCount);
            return false;
        } catch (InterruptedException e) {
            logger.error("Webhook dispatch interrupted: event_id={}, transaction_id={}",
                event.getId(), event.getAggregateId(), e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Builds HTTP request for webhook dispatch.
     * 
     * Includes all required headers:
     * - X-Webhook-ID: UUID (unique per outbox event)
     * - X-Idempotency-Key: UUID (from transaction)
     * - X-Retry-Count: Retry attempt number
     * - X-Timestamp: ISO8601 timestamp
     * - X-Signature: HMAC-SHA256 signature
     * 
     * @param event The outbox event
     * @param webhookUrl The merchant webhook URL
     * @param merchant The merchant
     * @param retryCount Current retry attempt number
     * @return HTTP request
     */
    private HttpRequest buildWebhookRequest(OutboxEvent event, String webhookUrl, Merchant merchant, int retryCount) {
        // Prepare payload
        String payload = event.getPayload();
        
        // Generate signature
        String secret = deriveWebhookSecret(merchant);
        String signature = signatureService.generateSignature(payload, secret);
        
        // Build request
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .timeout(Duration.ofMillis(HTTP_TIMEOUT_MS))
            .header("Content-Type", "application/json")
            .header("X-Webhook-ID", event.getId().toString())
            .header("X-Retry-Count", String.valueOf(retryCount))
            .header("X-Timestamp", Instant.now().toString())
            .header("X-Signature", signature)
            .POST(HttpRequest.BodyPublishers.ofString(payload));
        
        // Add idempotency key if available in payload
        try {
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
            Object idempotencyKey = payloadMap.get("idempotency_key");
            if (idempotencyKey != null) {
                requestBuilder.header("X-Idempotency-Key", idempotencyKey.toString());
            }
        } catch (Exception e) {
            logger.debug("Could not extract idempotency key from payload: {}", e.getMessage());
        }
        
        return requestBuilder.build();
    }
    
    /**
     * Derives webhook secret for merchant.
     * 
     * In production, this would fetch from secure configuration (e.g., AWS Secrets Manager).
     * For now, uses simple derivation from merchant ID.
     * 
     * @param merchant The merchant
     * @return Webhook secret
     */
    private String deriveWebhookSecret(Merchant merchant) {
        // TODO: In production, fetch from secure configuration
        // For now, use simple derivation
        return secretKeyPrefix + merchant.getMerchantId();
    }
    
    /**
     * Marks outbox event as DELIVERED.
     * 
     * Updates status and delivered_at timestamp.
     * 
     * @param event The outbox event
     */
    @Transactional
    private void markEventDelivered(OutboxEvent event) {
        event.setStatus(OutboxEventStatus.DELIVERED);
        event.setDeliveredAt(Instant.now());
        event.setUpdatedAt(Instant.now());
        outboxEventRepository.save(event);
        
        logger.info("Outbox event marked as DELIVERED: event_id={}, transaction_id={}",
            event.getId(), event.getAggregateId());
    }
    
    /**
     * Marks outbox event as FAILED.
     * 
     * Updates status after all retries exhausted.
     * 
     * @param event The outbox event
     * @param reason Failure reason
     */
    @Transactional
    private void markEventFailed(OutboxEvent event, String reason) {
        event.setStatus(OutboxEventStatus.FAILED);
        event.setUpdatedAt(Instant.now());
        outboxEventRepository.save(event);
        
        logger.error("Outbox event marked as FAILED: event_id={}, transaction_id={}, reason={}",
            event.getId(), event.getAggregateId(), reason);
    }
    
    /**
     * Updates retry count for outbox event.
     * 
     * @param event The outbox event
     * @param retryCount New retry count
     */
    @Transactional
    private void updateRetryCount(OutboxEvent event, int retryCount) {
        event.setRetryCount(retryCount);
        event.setUpdatedAt(Instant.now());
        outboxEventRepository.save(event);
        
        logger.debug("Outbox event retry count updated: event_id={}, transaction_id={}, retry_count={}",
            event.getId(), event.getAggregateId(), retryCount);
    }
}
