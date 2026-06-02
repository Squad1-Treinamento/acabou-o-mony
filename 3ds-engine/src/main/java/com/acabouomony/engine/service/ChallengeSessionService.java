package com.acabouomony.engine.service;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.acabouomony.engine.exception.ChallengeExpiredException;
import com.acabouomony.engine.model.ChallengeSession;
import com.acabouomony.engine.repository.ChallengeSessionRepository;

import reactor.core.publisher.Mono;

@Component
public class ChallengeSessionService {

    private final ChallengeSessionRepository repository;

    public ChallengeSessionService(ChallengeSessionRepository repository) {
        this.repository = repository;
    }

    public Mono<ChallengeSession> resolveChallenge(String challengeId) {
        return repository.findSessionById(challengeId)
                .flatMap(session -> {
                    if (isSessionExpired(session)) {
                        return Mono.error(new ChallengeExpiredException(challengeId));
                    }
                    return Mono.just(session);
                });
    }

    private boolean isSessionExpired(ChallengeSession session) {
        var expiresAt = session.getCreatedAt().plusSeconds(session.getTtl());
        return Instant.now().isAfter(expiresAt);
    }
}
