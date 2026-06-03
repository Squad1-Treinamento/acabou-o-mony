package com.acabouomony.engine.service;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import static com.acabouomony.engine.service.AuditLogger.auditLog;

import com.acabouomony.engine.dto.CallbackRequest;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Component
public class CallbackNotifier {

    private static final Logger log = LoggerFactory.getLogger(CallbackNotifier.class);

    private final WebClient webClient;

    public CallbackNotifier(@Value("${3ds.callback-url}") String callbackUrl,
                            WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(callbackUrl).build();
    }

    private static RetryBackoffSpec callbackRetry(String challengeId) {
        return Retry.backoff(3, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(5))
                .doBeforeRetry(rs -> log.warn(
                        "{\"event\":\"callback.retry\",\"challenge_id\":\"{}\",\"attempt\":{}}",
                        challengeId, rs.totalRetries() + 1));
    }

    public Mono<Void> notifyCore(String challengeId, String transactionId, String merchantId, String authStatus) {
        var request = new CallbackRequest(challengeId, transactionId, merchantId, authStatus, Instant.now());

        return webClient.post()
                .uri("/api/v1/payments/3ds-callback")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatus::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "Callback failed with " + response.statusCode()))))
                .bodyToMono(Void.class)
                .retryWhen(callbackRetry(challengeId))
                .doOnSuccess(v -> log.info(
                        auditLog("callback.sent", challengeId, transactionId, merchantId)))
                .doOnError(e -> log.warn(
                        auditLog("callback.failed", challengeId, transactionId, merchantId)));
    }
}
