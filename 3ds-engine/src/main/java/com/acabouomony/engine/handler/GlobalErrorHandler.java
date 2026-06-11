package com.acabouomony.engine.handler;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.acabouomony.engine.exception.ChallengeExpiredException;
import com.acabouomony.engine.exception.InvalidTokenException;
import com.acabouomony.engine.exception.ThreeDsException;
import com.acabouomony.engine.model.ErrorResponse;

import reactor.core.publisher.Mono;

@ControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(ChallengeExpiredException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleChallengeExpired(ChallengeExpiredException ex) {
        return buildResponse(HttpStatus.GONE, ex);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInvalidToken(InvalidTokenException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(ThreeDsException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleThreeDs(ThreeDsException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    private Mono<ResponseEntity<ErrorResponse>> buildResponse(HttpStatus status, ThreeDsException ex) {
        var body = new ErrorResponse(
                status.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getChallengeId(),
                Instant.now()
        );
        return Mono.just(ResponseEntity.status(status).body(body));
    }

}
