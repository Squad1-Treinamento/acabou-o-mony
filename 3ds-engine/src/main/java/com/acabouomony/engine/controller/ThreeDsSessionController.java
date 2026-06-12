package com.acabouomony.engine.controller;

import com.acabouomony.engine.dto.ThreeDsSessionRequest;
import com.acabouomony.engine.dto.ThreeDsSessionResponse;
import com.acabouomony.engine.service.ChallengeSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/3ds")
public class ThreeDsSessionController {

    private final ChallengeSessionService sessionService;

    public ThreeDsSessionController(ChallengeSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/sessions")
    public Mono<ResponseEntity<ThreeDsSessionResponse>> createSession(
            @Valid @RequestBody ThreeDsSessionRequest request) {
        return sessionService.createSession(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
}
