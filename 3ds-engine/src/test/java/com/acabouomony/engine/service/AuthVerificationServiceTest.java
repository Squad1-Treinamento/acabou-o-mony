package com.acabouomony.engine.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.acabouomony.engine.dto.MfaVerifyRequest;
import com.acabouomony.engine.exception.ChallengeExpiredException;
import com.acabouomony.engine.model.AuthResult;
import com.acabouomony.engine.model.ChallengeSession;
import com.acabouomony.engine.repository.ChallengeSessionRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthVerificationServiceTest {

    @Mock
    private ChallengeSessionRepository repository;

    @Mock
    private ChallengeSessionService sessionService;

    @Mock
    private CallbackNotifier callbackNotifier;

    @InjectMocks
    private AuthVerificationService service;

    private ChallengeSession createValidSession() {
        return new ChallengeSession(
                "txn-001", "merchant-1", new BigDecimal("150.00"),
                "BRL", "card-token", "https://acs.bank.com/auth",
                "pending", Instant.now(), 600L);
    }

    @Test
    void shouldApproveValidToken() {
        var session = createValidSession();
        when(repository.findAuthResult("ch-001")).thenReturn(Mono.empty());
        when(repository.findSessionById("ch-001")).thenReturn(Mono.just(session));
        when(sessionService.isSessionExpired(session)).thenReturn(false);
        when(repository.updateSessionStatus("ch-001", "approved")).thenReturn(Mono.empty());
        when(repository.saveAuthResult(eq("ch-001"), any(AuthResult.class))).thenReturn(Mono.empty());
        when(callbackNotifier.notifyCore("ch-001", "txn-001", "approved")).thenReturn(Mono.empty());

        var request = new MfaVerifyRequest("ch-001", "valid-mfa-token");
        StepVerifier.create(service.verifyMfa(request))
                .assertNext(response -> {
                    assert response.status().equals("approved");
                    assert response.challengeId().equals("ch-001");
                    assert response.transactionId().equals("txn-001");
                })
                .verifyComplete();
    }

    @Test
    void shouldDeclineEmptyToken() {
        var session = createValidSession();
        when(repository.findAuthResult("ch-001")).thenReturn(Mono.empty());
        when(repository.findSessionById("ch-001")).thenReturn(Mono.just(session));
        when(sessionService.isSessionExpired(session)).thenReturn(false);
        when(repository.updateSessionStatus("ch-001", "declined")).thenReturn(Mono.empty());
        when(repository.saveAuthResult(eq("ch-001"), any(AuthResult.class))).thenReturn(Mono.empty());
        when(callbackNotifier.notifyCore("ch-001", "txn-001", "declined")).thenReturn(Mono.empty());

        var request = new MfaVerifyRequest("ch-001", "");
        StepVerifier.create(service.verifyMfa(request))
                .assertNext(response -> {
                    assert response.status().equals("declined");
                    assert response.challengeId().equals("ch-001");
                    assert response.transactionId().equals("txn-001");
                })
                .verifyComplete();
    }

    @Test
    void shouldDeclineNullToken() {
        var session = createValidSession();
        when(repository.findAuthResult("ch-001")).thenReturn(Mono.empty());
        when(repository.findSessionById("ch-001")).thenReturn(Mono.just(session));
        when(sessionService.isSessionExpired(session)).thenReturn(false);
        when(repository.updateSessionStatus("ch-001", "declined")).thenReturn(Mono.empty());
        when(repository.saveAuthResult(eq("ch-001"), any(AuthResult.class))).thenReturn(Mono.empty());
        when(callbackNotifier.notifyCore("ch-001", "txn-001", "declined")).thenReturn(Mono.empty());

        var request = new MfaVerifyRequest("ch-001", null);
        StepVerifier.create(service.verifyMfa(request))
                .assertNext(response -> {
                    assert response.status().equals("declined");
                    assert response.challengeId().equals("ch-001");
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnChallengeExpiredForExpiredSession() {
        var session = createValidSession();
        when(repository.findAuthResult("ch-001")).thenReturn(Mono.empty());
        when(repository.findSessionById("ch-001")).thenReturn(Mono.just(session));
        when(sessionService.isSessionExpired(session)).thenReturn(true);
        when(repository.updateSessionStatus("ch-001", "expired")).thenReturn(Mono.empty());

        var request = new MfaVerifyRequest("ch-001", "any-token");
        StepVerifier.create(service.verifyMfa(request))
                .expectError(ChallengeExpiredException.class)
                .verify();
    }

    @Test
    void shouldReturnChallengeExpiredWhenSessionNotFound() {
        when(repository.findAuthResult("ch-999")).thenReturn(Mono.empty());
        when(repository.findSessionById("ch-999")).thenReturn(Mono.empty());

        var request = new MfaVerifyRequest("ch-999", "any-token");
        StepVerifier.create(service.verifyMfa(request))
                .expectError(ChallengeExpiredException.class)
                .verify();
    }

    @Test
    void shouldSucceedEvenWhenCallbackFails() {
        var session = createValidSession();
        when(repository.findAuthResult("ch-001")).thenReturn(Mono.empty());
        when(repository.findSessionById("ch-001")).thenReturn(Mono.just(session));
        when(sessionService.isSessionExpired(session)).thenReturn(false);
        when(repository.updateSessionStatus("ch-001", "approved")).thenReturn(Mono.empty());
        when(repository.saveAuthResult(eq("ch-001"), any(AuthResult.class))).thenReturn(Mono.empty());
        when(callbackNotifier.notifyCore("ch-001", "txn-001", "approved"))
                .thenReturn(Mono.error(new RuntimeException("Core unreachable")));

        var request = new MfaVerifyRequest("ch-001", "valid-token");
        StepVerifier.create(service.verifyMfa(request))
                .assertNext(response -> {
                    assert response.status().equals("approved");
                    assert response.challengeId().equals("ch-001");
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnCachedResultWhenIdempotencyHit() {
        var cached = new AuthResult("approved", "ch-001", "txn-001", Instant.now());
        when(repository.findAuthResult("ch-001")).thenReturn(Mono.just(cached));

        var request = new MfaVerifyRequest("ch-001", "any-token");
        StepVerifier.create(service.verifyMfa(request))
                .assertNext(response -> {
                    assert response.status().equals("approved");
                    assert response.challengeId().equals("ch-001");
                })
                .verifyComplete();

        verify(repository, never()).updateSessionStatus(any(), any());
        verify(repository, never()).saveAuthResult(any(), any());
        verify(callbackNotifier, never()).notifyCore(any(), any(), any());
    }
}
