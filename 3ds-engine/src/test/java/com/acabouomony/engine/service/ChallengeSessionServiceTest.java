package com.acabouomony.engine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.acabouomony.engine.dto.ThreeDsSessionRequest;
import com.acabouomony.engine.exception.ChallengeExpiredException;
import com.acabouomony.engine.model.ChallengeSession;
import com.acabouomony.engine.repository.ChallengeSessionRepository;
import com.acabouomony.engine.security.JwtTokenProvider;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ChallengeSessionServiceTest {

    @Mock
    private ChallengeSessionRepository repository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private ChallengeSessionService service;

    @BeforeEach
    void setUp() {
        service = new ChallengeSessionService(repository, jwtTokenProvider, "http://localhost:8081/challenge", 600L);
    }

    @Test
    void shouldReturnSessionWhenNotExpired() {
        var session = new ChallengeSession(
                "txn-001", "merchant-1", new BigDecimal("150.00"),
                "BRL", "card-token", "https://acs.bank.com/auth",
                "pending", Instant.now().minusSeconds(10), 600L);

        when(repository.findSessionById("ch-001")).thenReturn(Mono.just(session));

        StepVerifier.create(service.resolveChallenge("ch-001"))
                .assertNext(s -> assertThat(s.getTransactionId()).isEqualTo("txn-001"))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenSessionNotFound() {
        when(repository.findSessionById("ch-999")).thenReturn(Mono.empty());

        StepVerifier.create(service.resolveChallenge("ch-999"))
                .verifyComplete();
    }

    @Test
    void shouldErrorWhenSessionExpired() {
        var session = new ChallengeSession(
                "txn-002", "merchant-1", new BigDecimal("200.00"),
                "BRL", "card-token", "https://acs.bank.com/auth",
                "pending", Instant.now().minusSeconds(1200), 600L);

        when(repository.findSessionById("ch-002")).thenReturn(Mono.just(session));

        StepVerifier.create(service.resolveChallenge("ch-002"))
                .expectError(ChallengeExpiredException.class)
                .verify();
    }

    @Test
    void shouldCreateSessionAndReturnResponse() {
        var request = new ThreeDsSessionRequest("txn-100", "merchant-5", 5000L, "BRL", "card-abc");

        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn("test-jwt");
        when(repository.saveSession(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.createSession(request))
                .assertNext(response -> {
                    assertThat(response.challengeId()).isNotBlank();
                    assertThat(response.acsUrl()).startsWith("http://localhost:8081/challenge/");
                    assertThat(response.acsUrl()).contains(response.challengeId());
                    assertThat(response.acsUrl()).doesNotContain("/challenge/challenge/");
                    assertThat(response.jwt()).isEqualTo("test-jwt");
                })
                .verifyComplete();
    }

    @Test
    void shouldCallRepositorySaveOnceWithChallengeId() {
        var request = new ThreeDsSessionRequest("txn-101", "merchant-6", 1000L, "USD", "card-xyz");

        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn("some-jwt");
        when(repository.saveSession(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.createSession(request))
                .assertNext(response -> assertThat(response.challengeId()).isNotNull())
                .verifyComplete();

        verify(repository, times(1)).saveSession(anyString(), any(ChallengeSession.class));
    }

    @Test
    void shouldGenerateUniqueChallengIds() {
        var request = new ThreeDsSessionRequest("txn-102", "merchant-7", 2500L, "BRL", "card-def");

        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn("jwt-1", "jwt-2");
        when(repository.saveSession(any(), any())).thenReturn(Mono.empty());

        List<String> ids = new java.util.ArrayList<>();

        StepVerifier.create(service.createSession(request))
                .assertNext(r -> ids.add(r.challengeId()))
                .verifyComplete();

        StepVerifier.create(service.createSession(request))
                .assertNext(r -> ids.add(r.challengeId()))
                .verifyComplete();

        assertThat(ids).hasSize(2);
        assertThat(ids.get(0)).isNotEqualTo(ids.get(1));
    }
}
