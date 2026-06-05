package com.acabouomony.engine.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthVerificationServiceAuditTest {

    @Mock
    private ChallengeSessionRepository repository;

    @Mock
    private ChallengeSessionService sessionService;

    @Mock
    private CallbackNotifier callbackNotifier;

    @InjectMocks
    private AuthVerificationService service;

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) org.slf4j.LoggerFactory.getLogger(AuthVerificationService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAndStopAllAppenders();
    }

    private ChallengeSession createSession() {
        return new ChallengeSession(
                "txn-001", "merchant-1", new BigDecimal("150.00"),
                "BRL", "card-token", "https://acs.bank.com/auth",
                "pending", Instant.now(), 600L);
    }

    @Test
    void shouldLogApprovedEvent() {
        var session = createSession();
        when(repository.findAuthResult("ch-001")).thenReturn(Mono.empty());
        when(repository.findSessionById("ch-001")).thenReturn(Mono.just(session));
        when(sessionService.isSessionExpired(session)).thenReturn(false);
        when(repository.updateSessionStatus("ch-001", "approved")).thenReturn(Mono.empty());
        when(repository.saveAuthResult(eq("ch-001"), any(AuthResult.class))).thenReturn(Mono.empty());
        when(callbackNotifier.notifyCore("ch-001", "txn-001", "merchant-1", "approved")).thenReturn(Mono.empty());

        StepVerifier.create(service.verifyMfa(new MfaVerifyRequest("ch-001", "valid-token")))
                .expectNextCount(1)
                .verifyComplete();

        boolean found = appender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.INFO
                        && e.getFormattedMessage().contains("challenge.approved")
                        && e.getFormattedMessage().contains("ch-001"));
        assert found : "Expected challenge.approved audit log";
    }

    @Test
    void shouldLogDeclinedEvent() {
        var session = createSession();
        when(repository.findAuthResult("ch-001")).thenReturn(Mono.empty());
        when(repository.findSessionById("ch-001")).thenReturn(Mono.just(session));
        when(sessionService.isSessionExpired(session)).thenReturn(false);
        when(repository.updateSessionStatus("ch-001", "declined")).thenReturn(Mono.empty());
        when(repository.saveAuthResult(eq("ch-001"), any(AuthResult.class))).thenReturn(Mono.empty());
        when(callbackNotifier.notifyCore("ch-001", "txn-001", "merchant-1", "declined")).thenReturn(Mono.empty());

        StepVerifier.create(service.verifyMfa(new MfaVerifyRequest("ch-001", "")))
                .expectNextCount(1)
                .verifyComplete();

        boolean found = appender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.WARN
                        && e.getFormattedMessage().contains("challenge.declined")
                        && e.getFormattedMessage().contains("ch-001"));
        assert found : "Expected challenge.declined audit log";
    }

    @Test
    void shouldLogExpiredEvent() {
        var session = createSession();
        when(repository.findAuthResult("ch-001")).thenReturn(Mono.empty());
        when(repository.findSessionById("ch-001")).thenReturn(Mono.just(session));
        when(sessionService.isSessionExpired(session)).thenReturn(true);
        when(repository.updateSessionStatus("ch-001", "expired")).thenReturn(Mono.empty());

        StepVerifier.create(service.verifyMfa(new MfaVerifyRequest("ch-001", "any")))
                .expectError(ChallengeExpiredException.class)
                .verify();

        boolean found = appender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.WARN
                        && e.getFormattedMessage().contains("challenge.expired")
                        && e.getFormattedMessage().contains("ch-001"));
        assert found : "Expected challenge.expired audit log";
    }

    @Test
    void shouldLogCachedEvent() {
        var cached = new AuthResult("approved", "ch-001", "txn-001", Instant.now());
        when(repository.findAuthResult("ch-001")).thenReturn(Mono.just(cached));
        when(repository.findSessionById("ch-001")).thenReturn(Mono.empty());

        StepVerifier.create(service.verifyMfa(new MfaVerifyRequest("ch-001", "any")))
                .expectNextCount(1)
                .verifyComplete();

        boolean found = appender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.WARN
                        && e.getFormattedMessage().contains("challenge.cached")
                        && e.getFormattedMessage().contains("ch-001"));
        assert found : "Expected challenge.cached audit log";
    }
}
