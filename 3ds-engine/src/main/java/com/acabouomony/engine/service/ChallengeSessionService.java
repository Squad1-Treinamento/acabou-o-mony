package com.acabouomony.engine.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.acabouomony.engine.service.AuditLogger.auditLog;

import com.acabouomony.engine.dto.ThreeDsSessionRequest;
import com.acabouomony.engine.dto.ThreeDsSessionResponse;
import com.acabouomony.engine.exception.ChallengeExpiredException;
import com.acabouomony.engine.model.ChallengeSession;
import com.acabouomony.engine.repository.ChallengeSessionRepository;
import com.acabouomony.engine.security.JwtTokenProvider;

import reactor.core.publisher.Mono;

@Component
public class ChallengeSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeSessionService.class);

    private final ChallengeSessionRepository repository;
    private final JwtTokenProvider jwtTokenProvider;
    private final String acsBaseUrl;
    private final long sessionTtlSeconds;

    public ChallengeSessionService(ChallengeSessionRepository repository,
                                   JwtTokenProvider jwtTokenProvider,
                                   @Value("${3ds.redirect-base-url}") String acsBaseUrl,
                                   @Value("${3ds.session-ttl-seconds:600}") long sessionTtlSeconds) {
        this.repository = repository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.acsBaseUrl = acsBaseUrl;
        this.sessionTtlSeconds = sessionTtlSeconds;
    }

    public Mono<ThreeDsSessionResponse> createSession(ThreeDsSessionRequest request) {
        String challengeId = UUID.randomUUID().toString();
        String acsUrl      = acsBaseUrl + "/" + challengeId;
        String jwt         = jwtTokenProvider.generateToken(
                challengeId, request.transactionId(), request.merchantId(), request.amount());

        ChallengeSession session = new ChallengeSession(
                request.transactionId(),
                request.merchantId(),
                BigDecimal.valueOf(request.amount()),
                request.currency(),
                request.cardToken(),
                acsUrl,
                "pending",
                Instant.now(),
                sessionTtlSeconds
        );

        return repository.saveSession(challengeId, session)
                .thenReturn(new ThreeDsSessionResponse(challengeId, acsUrl, jwt));
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

    public boolean isSessionExpired(ChallengeSession session) {
        if ("expired".equals(session.getStatus())) {
            return true;
        }
        var expiresAt = session.getCreatedAt().plusSeconds(session.getTtl());
        return Instant.now().isAfter(expiresAt);
    }

}
