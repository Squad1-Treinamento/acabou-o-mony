package com.acabouomony.engine.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.acabouomony.engine.dto.ChallengeInitRequest;
import com.acabouomony.engine.dto.ChallengeInitResponse;
import com.acabouomony.engine.service.ChallengeSessionService;

import reactor.core.publisher.Mono;

@RestController
public class ThreeDsChallengeController {

    private final ChallengeSessionService sessionService;

    public ThreeDsChallengeController(ChallengeSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/api/v1/payments/3ds-challenge")
    public Mono<ResponseEntity<ChallengeInitResponse>> initiateChallenge(
            @Valid @RequestBody ChallengeInitRequest request) {
        return sessionService.initiateChallenge(request)
                .map(ResponseEntity::ok);
    }
}
