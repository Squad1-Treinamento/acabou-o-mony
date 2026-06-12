package com.acabouomony.engine.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.acabouomony.engine.exception.ChallengeExpiredException;
import com.acabouomony.engine.model.ChallengeSession;
import com.acabouomony.engine.repository.ChallengeSessionRepository;
import com.acabouomony.engine.security.JwtTokenProvider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ChallengeSessionServiceAuditTest {

    @Mock
    private ChallengeSessionRepository repository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private ChallengeSessionService service;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        service = new ChallengeSessionService(repository, jwtTokenProvider, "http://localhost:8081/challenge", 600L);
        logger = (Logger) org.slf4j.LoggerFactory.getLogger(ChallengeSessionService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAndStopAllAppenders();
    }

    private ChallengeSession createExpiredSession() {
        return new ChallengeSession(
                "txn-expired", "merchant-1", new BigDecimal("100.00"),
                "BRL", "card-token", "https://acs.bank.com/auth",
                "pending", Instant.now().minusSeconds(1200), 600L);
    }

    @Test
    void resolveChallengeShouldLogExpiredEventWhenExpired() {
        var session = createExpiredSession();
        when(repository.findSessionById("ch-expired")).thenReturn(Mono.just(session));

        StepVerifier.create(service.resolveChallenge("ch-expired"))
                .expectError(ChallengeExpiredException.class)
                .verify();

        boolean found = appender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.WARN
                        && e.getFormattedMessage().contains("challenge.expired")
                        && e.getFormattedMessage().contains("ch-expired"));
        assert found : "Expected challenge.expired audit log";
    }
}
