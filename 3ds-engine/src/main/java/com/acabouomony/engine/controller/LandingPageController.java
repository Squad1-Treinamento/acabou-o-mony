package com.acabouomony.engine.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.acabouomony.engine.security.JwtTokenProvider;
import com.acabouomony.engine.service.ChallengeSessionService;

import reactor.core.publisher.Mono;

@RestController
public class LandingPageController {

    private final JwtTokenProvider jwtTokenProvider;
    private final ChallengeSessionService sessionService;

    LandingPageController(JwtTokenProvider jwtTokenProvider, ChallengeSessionService sessionService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionService = sessionService;
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

    private Mono<ResponseEntity<Void>> redirectToAcs(String acsUrl) {
        var location = URI.create(acsUrl);
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .location(location)
                .build());
    }
}
