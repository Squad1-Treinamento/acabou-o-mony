package com.acabouomony.engine.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.acabouomony.engine.service.AuditLogger.auditLog;

import com.acabouomony.engine.exception.ChallengeExpiredException;
import com.acabouomony.engine.model.ChallengeSession;
import com.acabouomony.engine.repository.ChallengeSessionRepository;

import reactor.core.publisher.Mono;

@Component
public class ChallengeSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeSessionService.class);

    private final ChallengeSessionRepository repository;

    public ChallengeSessionService(ChallengeSessionRepository repository) {
        this.repository = repository;
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
