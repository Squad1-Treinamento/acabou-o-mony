package com.acabouomony.engine.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import static com.acabouomony.engine.service.AuditLogger.auditLog;

import com.acabouomony.engine.dto.CallbackRequest;

@Component
public class CallbackNotifier {

    private static final Logger log = LoggerFactory.getLogger(CallbackNotifier.class);
    private static final Duration SHUTDOWN_GRACE = Duration.ofSeconds(30);

    private final WebClient webClient;
    private final AtomicLong inFlight = new AtomicLong();

    public CallbackNotifier(@Value("${3ds.callback-url}") String callbackUrl,
                            WebClient.Builder webClientBuilder) {
        var httpClient = HttpClient.create()
                .protocol(HttpProtocol.H2);
        this.webClient = webClientBuilder
                .baseUrl(callbackUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private static RetryBackoffSpec callbackRetry(String challengeId) {
        return Retry.backoff(3, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(5))
                .doBeforeRetry(rs -> log.warn(
                        "{\"event\":\"callback.retry\",\"challenge_id\":\"{}\",\"attempt\":{}}",
                        challengeId, rs.totalRetries() + 1));
    }

    public Mono<Void> notifyCore(String challengeId, String transactionId, String merchantId, String authStatus) {
        inFlight.incrementAndGet();
        var request = new CallbackRequest(challengeId, transactionId, merchantId, authStatus, Instant.now());

        return webClient.post()
                .uri("/api/v1/payments/3ds-callback")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status instanceof HttpStatus hs && hs.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "Callback failed with " + response.statusCode()))))
                .bodyToMono(Void.class)
                .retryWhen(callbackRetry(challengeId))
                .doOnSuccess(v -> log.info(
                        auditLog("callback.sent", challengeId, transactionId, merchantId)))
                .doOnError(e -> log.warn(
                        auditLog("callback.failed", challengeId, transactionId, merchantId)))
                .doFinally(signal -> inFlight.decrementAndGet());
    }

    @PreDestroy
    void shutdown() {
        var deadline = Instant.now().plus(SHUTDOWN_GRACE);
        while (inFlight.get() > 0 && Instant.now().isBefore(deadline)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (inFlight.get() > 0) {
            log.warn("{} callback(s) did not complete within grace period", inFlight.get());
        }
    }
}
