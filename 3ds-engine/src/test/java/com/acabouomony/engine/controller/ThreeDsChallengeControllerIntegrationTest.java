package com.acabouomony.engine.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.acabouomony.engine.dto.MfaVerifyRequest;
import com.acabouomony.engine.model.AuthResult;
import com.acabouomony.engine.model.ChallengeSession;
import com.acabouomony.engine.repository.ChallengeSessionRepository;
import com.acabouomony.engine.service.CallbackNotifier;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ThreeDsChallengeControllerIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            "redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
        registry.add("3ds.security.enabled", () -> "false");
        registry.add("3ds.rate-limit-enabled", () -> "false");
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
            return http
                    .authorizeExchange(e -> e.anyExchange().permitAll())
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .build();
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ChallengeSessionRepository repository;

    @MockBean
    private CallbackNotifier callbackNotifier;

    @BeforeEach
    void cleanRedis() {
        var ids = new String[]{"it-ch-001", "it-ch-002", "it-ch-expired", "it-ch-idem"};
        Flux.fromArray(ids)
                .flatMap(id -> repository.deleteSession(id).then(repository.deleteAuthResult(id)))
                .then()
                .block(Duration.ofSeconds(5));
    }

    private void createSession(String challengeId, long ttlSeconds) {
        var session = new ChallengeSession(
                "txn-" + challengeId, "merchant-1", new BigDecimal("100.00"),
                "BRL", "card-token", "https://acs.bank.com/auth",
                "pending", Instant.now(), ttlSeconds);
        repository.saveSession(challengeId, session).block(Duration.ofSeconds(5));
    }

    private void awaitExpired(String challengeId) {
        Flux.interval(Duration.ZERO, Duration.ofMillis(200))
                .flatMap(tick -> {
                    var result = webTestClient.post().uri("/api/v1/3ds/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(new MfaVerifyRequest(challengeId, "any-token"))
                            .exchange()
                            .returnResult(Void.class);
                    if (result.getStatus().value() == 410) {
                        return Mono.just(true);
                    }
                    return Mono.empty();
                })
                .next()
                .block(Duration.ofSeconds(15));
    }

    @Test
    void happyPath_shouldApproveAndNotifyCallback() {
        createSession("it-ch-001", 600);
        when(callbackNotifier.notifyCore("it-ch-001", "txn-it-ch-001", "merchant-1", "approved"))
                .thenReturn(Mono.empty());

        webTestClient.post().uri("/api/v1/3ds/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"challengeId\":\"it-ch-001\",\"mfaToken\":\"valid-token\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("approved")
                .jsonPath("$.challenge_id").isEqualTo("it-ch-001")
                .jsonPath("$.transaction_id").isEqualTo("txn-it-ch-001");

        verify(callbackNotifier).notifyCore("it-ch-001", "txn-it-ch-001", "merchant-1", "approved");
    }

    @Test
    void invalidMfaToken_shouldReturnDeclined() {
        createSession("it-ch-002", 600);
        when(callbackNotifier.notifyCore("it-ch-002", "txn-it-ch-002", "merchant-1", "declined"))
                .thenReturn(Mono.empty());

        webTestClient.post().uri("/api/v1/3ds/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"challengeId\":\"it-ch-002\",\"mfaToken\":\"\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("declined")
                .jsonPath("$.challenge_id").isEqualTo("it-ch-002");

        verify(callbackNotifier).notifyCore("it-ch-002", "txn-it-ch-002", "merchant-1", "declined");
    }

    @Test
    void expiredSession_shouldReturn410() {
        createSession("it-ch-expired", 1);

        awaitExpired("it-ch-expired");

        verify(callbackNotifier, never()).notifyCore(any(), any(), any(), any());
    }

    @Test
    void idempotency_secondCallReturnsCachedResult() {
        createSession("it-ch-idem", 600);
        when(callbackNotifier.notifyCore("it-ch-idem", "txn-it-ch-idem", "merchant-1", "approved"))
                .thenReturn(Mono.empty());

        webTestClient.post().uri("/api/v1/3ds/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"challengeId\":\"it-ch-idem\",\"mfaToken\":\"valid-token\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("approved");

        var cachedStatus = repository.findAuthResult("it-ch-idem")
                .map(AuthResult::getAuthStatus)
                .block(Duration.ofSeconds(5));

        webTestClient.post().uri("/api/v1/3ds/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"challengeId\":\"it-ch-idem\",\"mfaToken\":\"valid-token\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("approved");

        var cachedAfterSecond = repository.findAuthResult("it-ch-idem")
                .map(AuthResult::getAuthStatus)
                .block(Duration.ofSeconds(5));

        assertThat(cachedStatus).isEqualTo("approved");
        assertThat(cachedAfterSecond).isEqualTo(cachedStatus);

        verify(callbackNotifier, times(1)).notifyCore(any(), any(), any(), any());
    }

    @Test
    void missingChallengeId_shouldReturn400() {
        webTestClient.post().uri("/api/v1/3ds/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"mfaToken\":\"some-token\"}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void sessionNotFound_shouldReturn410() {
        webTestClient.post().uri("/api/v1/3ds/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"challengeId\":\"it-ch-nonexistent\",\"mfaToken\":\"any-token\"}")
                .exchange()
                .expectStatus().isEqualTo(410);
    }
}
