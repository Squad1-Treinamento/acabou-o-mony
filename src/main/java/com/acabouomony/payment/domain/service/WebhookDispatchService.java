package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.Merchant;
import com.acabouomony.payment.domain.entity.OutboxEvent;
import com.acabouomony.payment.domain.model.OutboxEventStatus;
import com.acabouomony.payment.domain.repository.MerchantRepository;
import com.acabouomony.payment.infrastructure.persistence.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WebhookDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDispatchService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final MerchantRepository merchantRepository;
    private final WebhookSignatureService signatureService;
    private final AlertService alertService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${webhook.max-retries}")
    private int maxRetries;

    @Value("${webhook.retry-backoff-delays}")
    private long[] backoffDelays;

    @Autowired
    public WebhookDispatchService(OutboxEventRepository outboxEventRepository, MerchantRepository merchantRepository, WebhookSignatureService signatureService, AlertService alertService, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.merchantRepository = merchantRepository;
        this.signatureService = signatureService;
        this.alertService = alertService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Dispatches a webhook based on an OutboxEvent, with retries.
     * This is the main entry point called by the WebhookDispatchWorker.
     *
     * @param event The outbox event to process.
     */
    public void dispatchWebhook(OutboxEvent event) {
        UUID merchantId = extractMerchantIdFromPayload(event.getPayload());
        if (merchantId == null) {
            logger.error("Could not extract merchantId from payload for event_id={}", event.getId());
            markEventFailed(event, "Missing merchantId in payload");
            return;
        }

        Optional<Merchant> merchantOpt = merchantRepository.findById(merchantId);
        if (merchantOpt.isEmpty() || merchantOpt.get().getWebhookUrl() == null) {
            logger.error("Merchant {} not found or has no webhook URL for event_id={}", merchantId, event.getId());
            markEventFailed(event, "Merchant not found or webhook URL missing");
            return;
        }

        Merchant merchant = merchantOpt.get();
        boolean success = attemptDispatchWithRetries(event, merchant);

        if (!success) {
            markEventFailed(event, "Webhook delivery failed after max retries.");
            alertService.alertWebhookDeliveryFailure(event, merchantId);
        }
    }

    private boolean attemptDispatchWithRetries(OutboxEvent event, Merchant merchant) {
        int retryCount = event.getRetryCount();

        while (retryCount < maxRetries) {
            if (attemptSingleDispatch(event, merchant, retryCount)) {
                markEventDelivered(event);
                return true;
            }

            retryCount++;
            updateRetryCount(event, retryCount);

            if (retryCount < maxRetries) {
                try {
                    long delayMs = backoffDelays[retryCount - 1];
                    logger.warn("Webhook dispatch failed for event_id={}. Retrying in {}ms (attempt {}/{})",
                            event.getId(), delayMs, retryCount, maxRetries);
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Webhook retry backoff interrupted for event_id={}", event.getId());
                    return false;
                }
            }
        }
        return false;
    }

    private boolean attemptSingleDispatch(OutboxEvent event, Merchant merchant, int retryCount) {
        try {
            HttpEntity<String> entity = buildRequestEntity(event, merchant, retryCount);
            logger.info("Dispatching webhook: event_id={}, url={}, attempt={}", event.getId(), merchant.getWebhookUrl(), retryCount + 1);

            ResponseEntity<String> response = restTemplate.postForEntity(merchant.getWebhookUrl(), entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Webhook dispatch successful: event_id={}, status_code={}", event.getId(), response.getStatusCode().value());
                return true;
            }
            // Non-2xx success codes are treated as failures for retry purposes.
            logger.warn("Webhook dispatch failed with non-2xx success code: event_id={}, status_code={}", event.getId(), response.getStatusCode().value());
            return false;

        } catch (HttpClientErrorException e) { // 4xx errors
            logger.error("Webhook dispatch client error (will not retry): event_id={}, status_code={}, body={}",
                    event.getId(), e.getStatusCode().value(), e.getResponseBodyAsString());
            // Fail immediately on 4xx, don't retry.
            return false;
        } catch (HttpServerErrorException e) { // 5xx errors
            logger.warn("Webhook dispatch server error (will retry): event_id={}, status_code={}",
                    event.getId(), e.getStatusCode().value());
            return false;
        } catch (ResourceAccessException e) { // Network errors (timeout, connection refused)
            logger.warn("Webhook dispatch network error (will retry): event_id={}, error={}", event.getId(), e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("An unexpected error occurred during webhook dispatch for event_id={}", event.getId(), e);
            return false;
        }
    }

    private HttpEntity<String> buildRequestEntity(OutboxEvent event, Merchant merchant, int retryCount) {
        String payload = event.getPayload();
        String signature = signatureService.generateSignature(payload, merchant.getWebhookSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-ID", event.getId().toString());
        headers.set("X-Retry-Count", String.valueOf(retryCount));
        headers.set("X-Timestamp", Instant.now().toString());
        headers.set("X-Signature", signature);

        try {
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
            if (payloadMap.containsKey("idempotencyKey")) {
                headers.set("X-Idempotency-Key", payloadMap.get("idempotencyKey").toString());
            }
        } catch (Exception e) {
            logger.debug("Could not extract idempotency key from payload for event_id={}", event.getId());
        }

        return new HttpEntity<>(payload, headers);
    }

    private void markEventDelivered(OutboxEvent event) {
        event.setStatus(OutboxEventStatus.DELIVERED);
        event.setDeliveredAt(Instant.now());
        event.setUpdatedAt(Instant.now());
        outboxEventRepository.save(event);
    }

    private void markEventFailed(OutboxEvent event, String reason) {
        logger.error("Marking webhook event as FAILED: event_id={}, reason='{}'", event.getId(), reason);
        event.setStatus(OutboxEventStatus.FAILED);
        event.setUpdatedAt(Instant.now());
        outboxEventRepository.save(event);
    }

    private void updateRetryCount(OutboxEvent event, int newRetryCount) {
        event.setRetryCount(newRetryCount);
        event.setUpdatedAt(Instant.now());
        outboxEventRepository.save(event);
    }

    private UUID extractMerchantIdFromPayload(String payload) {
        try {
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
            if (payloadMap.containsKey("merchantId")) {
                return UUID.fromString(payloadMap.get("merchantId").toString());
            }
        } catch (Exception e) {
            logger.error("Failed to parse payload to extract merchantId", e);
        }
        return null;
    }
}