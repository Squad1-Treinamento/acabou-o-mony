package com.acabouomony.engine.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.acabouomony.engine.service.AuditLogger.auditLog;

import com.acabouomony.engine.dto.ChallengeInitRequest;
import com.acabouomony.engine.dto.ChallengeInitResponse;
import com.acabouomony.engine.exception.ChallengeExpiredException;
import com.acabouomony.engine.exception.DuplicateChallengeException;
import com.acabouomony.engine.model.ChallengeSession;
import com.acabouomony.engine.repository.ChallengeSessionRepository;
import com.acabouomony.engine.security.JwtTokenProvider;

import reactor.core.publisher.Mono;

@Component
public class ChallengeSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeSessionService.class);

    private final ChallengeSessionRepository repository;
    private final JwtTokenProvider jwtTokenProvider;
    private final long sessionTtlSeconds;
    private final String redirectBaseUrl;

    public ChallengeSessionService(ChallengeSessionRepository repository,
                                   JwtTokenProvider jwtTokenProvider,
                                   @Value("${3ds.session-ttl-seconds:600}") long sessionTtlSeconds,
                                   @Value("${3ds.redirect-base-url:http://localhost:8081/challenge}") String redirectBaseUrl) {
        this.repository = repository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionTtlSeconds = sessionTtlSeconds;
        this.redirectBaseUrl = redirectBaseUrl;
    }

    public Mono<ChallengeSession> resolveChallenge(String challengeId) {
        return repository.findSessionById(challengeId)
                .flatMap(session -> {
                    if (isSessionExpired(session)) {
                        log.warn(auditLog("challenge.expired", challengeId,
                                session.getTransactionId(), session.getMerchantId()));
                        return Mono.error(new ChallengeExpiredException(challengeId));
                    }
                    return Mono.just(session);
                });
    }

    public Mono<ChallengeInitResponse> initiateChallenge(ChallengeInitRequest request) {
        var challengeId = UUID.randomUUID().toString();
        return repository.findSessionById(challengeId)
                .hasElement()
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new DuplicateChallengeException(challengeId));
                    }
                    var session = new ChallengeSession(
                            request.transactionId(),
                            request.merchantId(),
                            request.amount(),
                            request.currency(),
                            request.cardToken(),
                            request.acsUrl(),
                            "pending",
                            Instant.now(),
                            sessionTtlSeconds);
                    var jwt = jwtTokenProvider.sign(
                            challengeId, request.transactionId(), request.merchantId(), request.amount());
                    return repository.saveSession(challengeId, session)
                            .thenReturn(new ChallengeInitResponse(
                                    "initiated", challengeId,
                                    redirectBaseUrl + "/" + challengeId + "?jwt=" + jwt,
                                    jwt, sessionTtlSeconds));
                });
    }

    public boolean isSessionExpired(ChallengeSession session) {
        if ("expired".equals(session.getStatus())) {
            return true;
        }
        var expiresAt = session.getCreatedAt().plusSeconds(session.getTtl());
        return Instant.now().isAfter(expiresAt);
    }

}
