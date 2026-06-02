package com.acabouomony.engine.controller;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.acabouomony.engine.exception.InvalidTokenException;
import com.acabouomony.engine.handler.GlobalErrorHandler;
import com.acabouomony.engine.model.ChallengeSession;
import com.acabouomony.engine.security.JwtClaims;
import com.acabouomony.engine.security.JwtTokenProvider;
import com.acabouomony.engine.service.ChallengeSessionService;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class LandingPageControllerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ChallengeSessionService sessionService;

    @InjectMocks
    private LandingPageController controller;

    private WebTestClient webClient;

    private WebTestClient buildClient() {
        return WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalErrorHandler())
                .build();
    }

    @Test
    void shouldRedirectToAcsUrlWhenValid() {
        var claims = new JwtClaims("ch-001", "txn-001", "merchant-1",
                new BigDecimal("150.00"), Instant.now(), Instant.now().plusSeconds(600));
        var session = new ChallengeSession(
                "txn-001", "merchant-1", new BigDecimal("150.00"),
                "BRL", "card-token", "https://acs.bank.com/auth",
                "pending", Instant.now(), 600L);

        when(jwtTokenProvider.verify("valid-token")).thenReturn(Mono.just(claims));
        when(sessionService.resolveChallenge("ch-001")).thenReturn(Mono.just(session));

        webClient = buildClient();

        webClient.get().uri("/challenge/ch-001?jwt=valid-token")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueEquals("Location", "https://acs.bank.com/auth");
    }

    @Test
    void shouldReturn400WhenInvalidJwt() {
        when(jwtTokenProvider.verify("bad-token"))
                .thenReturn(Mono.error(new InvalidTokenException("Invalid JWT token")));

        webClient = buildClient();

        webClient.get().uri("/challenge/ch-001?jwt=bad-token")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn404WhenSessionNotFound() {
        var claims = new JwtClaims("ch-999", "txn-999", "merchant-1",
                new BigDecimal("100.00"), Instant.now(), Instant.now().plusSeconds(600));

        when(jwtTokenProvider.verify("valid-token")).thenReturn(Mono.just(claims));
        when(sessionService.resolveChallenge("ch-999")).thenReturn(Mono.empty());

        webClient = buildClient();

        webClient.get().uri("/challenge/ch-999?jwt=valid-token")
                .exchange()
                .expectStatus().isNotFound();
    }
}
