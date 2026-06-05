package com.acabouomony.engine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.acabouomony.engine.exception.ChallengeExpiredException;
import com.acabouomony.engine.model.ChallengeSession;
import com.acabouomony.engine.repository.ChallengeSessionRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ChallengeSessionServiceTest {

    @Mock
    private ChallengeSessionRepository repository;

    private ChallengeSessionService service;

    @BeforeEach
    void setUp() {
        service = new ChallengeSessionService(repository);
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
}
