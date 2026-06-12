package com.acabouomony.engine.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.acabouomony.engine.dto.ThreeDsSessionRequest;
import com.acabouomony.engine.dto.ThreeDsSessionResponse;
import com.acabouomony.engine.handler.GlobalErrorHandler;
import com.acabouomony.engine.service.ChallengeSessionService;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ThreeDsSessionControllerTest {

    @Mock
    private ChallengeSessionService sessionService;

    @InjectMocks
    private ThreeDsSessionController controller;

    private WebTestClient buildClient() {
        return WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalErrorHandler())
                .build();
    }

    @Test
    void shouldReturn201WithSessionDataWhenRequestIsValid() {
        var response = new ThreeDsSessionResponse(
                "ch-abc123",
                "http://localhost:8081/challenge/ch-abc123",
                "eyJhbGciOiJIUzI1NiJ9.test.jwt"
        );

        when(sessionService.createSession(any(ThreeDsSessionRequest.class)))
                .thenReturn(Mono.just(response));

        buildClient()
                .post().uri("/api/v1/3ds/sessions")
                .header("X-API-Key", "dev-api-key-change-in-production")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "transaction_id": "txn-001",
                            "merchant_id": "merchant-1",
                            "amount": 15000,
                            "currency": "BRL",
                            "card_token": "tok-abc"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.challenge_id").isEqualTo("ch-abc123")
                .jsonPath("$.acs_url").isEqualTo("http://localhost:8081/challenge/ch-abc123")
                .jsonPath("$.jwt").isEqualTo("eyJhbGciOiJIUzI1NiJ9.test.jwt");
    }

    @Test
    void shouldReturn400WhenTransactionIdIsBlank() {
        buildClient()
                .post().uri("/api/v1/3ds/sessions")
                .header("X-API-Key", "dev-api-key-change-in-production")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "transaction_id": "",
                            "merchant_id": "merchant-1",
                            "amount": 15000,
                            "currency": "BRL",
                            "card_token": "tok-abc"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
