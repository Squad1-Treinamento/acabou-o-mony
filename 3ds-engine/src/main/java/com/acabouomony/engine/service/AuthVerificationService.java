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

import static com.acabouomony.engine.service.AuditLogger.auditLog;

import java.time.Duration;

import reactor.core.publisher.Mono;

@Component
public class AuthVerificationService {

    private static final Logger log = LoggerFactory.getLogger(AuthVerificationService.class);
    private static final Duration CALLBACK_TIMEOUT = Duration.ofSeconds(5);

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
                                        log.warn(auditLog("challenge.expired",
                                                request.challengeId(),
                                                session.getTransactionId(),
                                                session.getMerchantId()));
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
            log.warn(auditLog("challenge.declined",
                    request.challengeId(), session.getTransactionId(), session.getMerchantId()));
            return decline(request.challengeId(), session.getTransactionId(), session.getMerchantId());
        }
        log.info(auditLog("challenge.approved",
                request.challengeId(), session.getTransactionId(), session.getMerchantId()));
        return approve(request.challengeId(), session.getTransactionId(), session.getMerchantId());
    }

    private Mono<MfaVerifyResponse> approve(String challengeId, String transactionId, String merchantId) {
        var result = new AuthResult("approved", challengeId, transactionId, Instant.now());
        return repository.updateSessionStatus(challengeId, "approved")
                .then(repository.saveAuthResult(challengeId, result))
                .flatMap(ignored -> fireCallback(challengeId, transactionId, merchantId, "approved"))
                .then(Mono.just(new MfaVerifyResponse("approved", challengeId, transactionId)));
    }

    private Mono<MfaVerifyResponse> decline(String challengeId, String transactionId, String merchantId) {
        var result = new AuthResult("declined", challengeId, transactionId, Instant.now());
        return repository.updateSessionStatus(challengeId, "declined")
                .then(repository.saveAuthResult(challengeId, result))
                .flatMap(ignored -> fireCallback(challengeId, transactionId, merchantId, "declined"))
                .then(Mono.just(new MfaVerifyResponse("declined", challengeId, transactionId)));
    }

    private Mono<Void> fireCallback(String challengeId, String transactionId, String merchantId, String authStatus) {
        return callbackNotifier.notifyCore(challengeId, transactionId, merchantId, authStatus)
                .timeout(CALLBACK_TIMEOUT)
                .onErrorResume(e -> {
                    log.error("Callback failed for {}: {}", challengeId, e.getMessage());
                    return Mono.empty();
                });
    }

    private MfaVerifyResponse toCachedResponse(AuthResult cached) {
        log.warn(auditLog("challenge.cached",
                cached.getChallengeId(), cached.getTransactionId(), null));
        return new MfaVerifyResponse(
                cached.getAuthStatus(),
                cached.getChallengeId(),
                cached.getTransactionId());
    }
}
