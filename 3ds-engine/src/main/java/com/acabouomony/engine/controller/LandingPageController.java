package com.acabouomony.engine.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.acabouomony.engine.dto.MfaVerifyRequest;
import com.acabouomony.engine.dto.MfaVerifyResponse;
import com.acabouomony.engine.exception.InvalidTokenException;
import com.acabouomony.engine.security.JwtTokenProvider;
import com.acabouomony.engine.service.AuthVerificationService;
import com.acabouomony.engine.service.ChallengeSessionService;

import reactor.core.publisher.Mono;

@RestController
public class LandingPageController {

    private final JwtTokenProvider jwtTokenProvider;
    private final ChallengeSessionService sessionService;
    private final AuthVerificationService verificationService;

    LandingPageController(JwtTokenProvider jwtTokenProvider,
                          ChallengeSessionService sessionService,
                          AuthVerificationService verificationService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionService = sessionService;
        this.verificationService = verificationService;
    }

    @GetMapping("/challenge/{challengeId}")
    Mono<ResponseEntity<Void>> landingPage(
            @PathVariable String challengeId,
            @RequestParam("jwt") String jwt) {

        return jwtTokenProvider.verify(jwt)
                .flatMap(claims -> sessionService.resolveChallenge(challengeId))
                .flatMap(session -> redirectToAcs(session.getAcsUrl()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/v1/3ds/verify")
    Mono<ResponseEntity<MfaVerifyResponse>> verifyMfa(@RequestBody MfaVerifyRequest request) {
        if (request.challengeId() == null || request.challengeId().isBlank()) {
            return Mono.error(new InvalidTokenException("Missing required fields: challengeId and mfaToken"));
        }
        return verificationService.verifyMfa(request)
                .map(ResponseEntity::ok);
    }

    private Mono<ResponseEntity<Void>> redirectToAcs(String acsUrl) {
        var location = URI.create(acsUrl);
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .location(location)
                .build());
    }
}
