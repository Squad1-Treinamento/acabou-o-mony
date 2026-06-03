package com.acabouomony.engine.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.acabouomony.engine.dto.MfaVerifyRequest;
import com.acabouomony.engine.dto.MfaVerifyResponse;
import com.acabouomony.engine.exception.ChallengeExpiredException;
import com.acabouomony.engine.model.AuthResult;
import com.acabouomony.engine.repository.ChallengeSessionRepository;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class AuthVerificationService {

    private static final Logger log = LoggerFactory.getLogger(AuthVerificationService.class);

    private final ChallengeSessionRepository repository;
    private final ChallengeSessionService sessionService;
    private final CallbackNotifier callbackNotifier;

    public AuthVerificationService(ChallengeSessionRepository repository,
                                   ChallengeSessionService sessionService,
                                   CallbackNotifier callbackNotifier) {
        this.repository = repository;
        this.sessionService = sessionService;
        this.callbackNotifier = callbackNotifier;
    }

    public Mono<MfaVerifyResponse> verifyMfa(MfaVerifyRequest request) {
        return repository.findAuthResult(request.challengeId())
                .map(this::toCachedResponse)
                .switchIfEmpty(
                        repository.findSessionById(request.challengeId())
                                .switchIfEmpty(Mono.error(
                                        new ChallengeExpiredException(request.challengeId())))
                                .flatMap(session -> {
                                    if (sessionService.isSessionExpired(session)) {
                                        return repository
                                                .updateSessionStatus(request.challengeId(), "expired")
                                                .then(Mono.error(
                                                        new ChallengeExpiredException(request.challengeId())));
                                    }
                                    return processMfa(request, session);
                                })
                );
    }

    private Mono<MfaVerifyResponse> processMfa(MfaVerifyRequest request,
                                                com.acabouomony.engine.model.ChallengeSession session) {
        var token = request.mfaToken();
        if (token == null || token.isBlank()) {
            log.warn("MFA verification failed for challenge {}: empty token",
                    request.challengeId());
            return decline(request.challengeId(), session.getTransactionId());
        }
        log.info("MFA verification succeeded for challenge {}",
                request.challengeId());
        return approve(request.challengeId(), session.getTransactionId());
    }

    private Mono<MfaVerifyResponse> approve(String challengeId, String transactionId) {
        var result = new AuthResult("approved", challengeId, transactionId, Instant.now());
        return repository.updateSessionStatus(challengeId, "approved")
                .then(repository.saveAuthResult(challengeId, result))
                .doOnSuccess(ignored -> callbackNotifier
                        .notifyCore(challengeId, transactionId, "approved")
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe())
                .then(Mono.just(new MfaVerifyResponse("approved", challengeId, transactionId)));
    }

    private Mono<MfaVerifyResponse> decline(String challengeId, String transactionId) {
        var result = new AuthResult("declined", challengeId, transactionId, Instant.now());
        return repository.updateSessionStatus(challengeId, "declined")
                .then(repository.saveAuthResult(challengeId, result))
                .doOnSuccess(ignored -> callbackNotifier
                        .notifyCore(challengeId, transactionId, "declined")
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe())
                .then(Mono.just(new MfaVerifyResponse("declined", challengeId, transactionId)));
    }

    private MfaVerifyResponse toCachedResponse(AuthResult cached) {
        log.warn("Idempotency hit for challenge {} - returning cached result {}",
                cached.getChallengeId(), cached.getAuthStatus());
        return new MfaVerifyResponse(
                cached.getAuthStatus(),
                cached.getChallengeId(),
                cached.getTransactionId());
    }
}
