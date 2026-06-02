package com.acabouomony.engine.handler;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.acabouomony.engine.exception.ChallengeExpiredException;
import com.acabouomony.engine.exception.DuplicateChallengeException;
import com.acabouomony.engine.exception.InvalidTokenException;
import com.acabouomony.engine.exception.ThreeDsException;
import com.acabouomony.engine.model.ErrorResponse;

@ControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(ChallengeExpiredException.class)
    public ResponseEntity<ErrorResponse> handleChallengeExpired(ChallengeExpiredException ex) {
        return buildResponse(HttpStatus.GONE, ex);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(DuplicateChallengeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateChallenge(DuplicateChallengeException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(ThreeDsException.class)
    public ResponseEntity<ErrorResponse> handleThreeDs(ThreeDsException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, ThreeDsException ex) {
        var body = new ErrorResponse(
                status.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                null,
                Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }

}
