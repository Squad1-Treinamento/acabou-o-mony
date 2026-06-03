package com.acabouomony.engine.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.acabouomony.engine.model.AuthResult;
import com.acabouomony.engine.model.ChallengeSession;
import com.acabouomony.engine.repository.ChallengeSessionRepository;
import com.acabouomony.engine.service.CallbackNotifier;

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
        for (var id : ids) {
            repository.findSessionById(id).flatMap(s -> Mono.empty()).block();
            repository.findAuthResult(id).flatMap(a -> Mono.empty()).block();
        }
    }

    private void createSession(String challengeId, long ttlSeconds) {
        var session = new ChallengeSession(
                "txn-" + challengeId, "merchant-1", new BigDecimal("100.00"),
                "BRL", "card-token", "https://acs.bank.com/auth",
                "pending", Instant.now(), ttlSeconds);
        repository.saveSession(challengeId, session).block();
    }

    @Test
    void happyPath_shouldApproveAndNotifyCallback() {
        createSession("it-ch-001", 600);
        when(callbackNotifier.notifyCore("it-ch-001", "txn-it-ch-001", "approved"))
                .thenReturn(Mono.empty());

        webTestClient.post().uri("/api/v1/3ds/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"challengeId\":\"it-ch-001\",\"mfaToken\":\"valid-token\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("approved")
                .jsonPath("$.challengeId").isEqualTo("it-ch-001")
                .jsonPath("$.transactionId").isEqualTo("txn-it-ch-001");

        verify(callbackNotifier).notifyCore("it-ch-001", "txn-it-ch-001", "approved");
    }

    @Test
    void invalidMfaToken_shouldReturnDeclined() {
        createSession("it-ch-002", 600);
        when(callbackNotifier.notifyCore("it-ch-002", "txn-it-ch-002", "declined"))
                .thenReturn(Mono.empty());

        webTestClient.post().uri("/api/v1/3ds/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"challengeId\":\"it-ch-002\",\"mfaToken\":\"\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("declined")
                .jsonPath("$.challengeId").isEqualTo("it-ch-002");

        verify(callbackNotifier).notifyCore("it-ch-002", "txn-it-ch-002", "declined");
    }

    @Test
    void expiredSession_shouldReturn410() {
        createSession("it-ch-expired", 1);

        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        webTestClient.post().uri("/api/v1/3ds/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"challengeId\":\"it-ch-expired\",\"mfaToken\":\"any-token\"}")
                .exchange()
                .expectStatus().isEqualTo(410);

        verify(callbackNotifier, never()).notifyCore(any(), any(), any());
    }

    @Test
    void idempotency_secondCallReturnsCachedResult() {
        createSession("it-ch-idem", 600);
        when(callbackNotifier.notifyCore("it-ch-idem", "txn-it-ch-idem", "approved"))
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
                .block();

        webTestClient.post().uri("/api/v1/3ds/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"challengeId\":\"it-ch-idem\",\"mfaToken\":\"valid-token\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("approved");

        var cachedAfterSecond = repository.findAuthResult("it-ch-idem")
                .map(AuthResult::getAuthStatus)
                .block();

        assert "approved".equals(cachedStatus) : "Auth result should be persisted";
        assert cachedStatus.equals(cachedAfterSecond) : "Auth result should not change";

        verify(callbackNotifier, times(1)).notifyCore(any(), any(), any());
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
