package com.acabouomony.payment.infrastructure.exception;

import com.acabouomony.payment.domain.exception.PaymentValidationException;
import com.acabouomony.payment.domain.exception.CardValidationException;
import com.acabouomony.payment.domain.exception.DuplicatePaymentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API errors.
 * 
 * Handles:
 * - PaymentValidationException (400 Bad Request)
 * - CardValidationException (400 Bad Request)
 * - DuplicatePaymentException (409 Conflict)
 * - MethodArgumentNotValidException (400 Bad Request)
 * - Generic exceptions (500 Internal Server Error)
 * 
 * Returns standardized error response format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles PaymentValidationException.
     * 
     * Returns 400 Bad Request with validation error details.
     * 
     * @param ex The PaymentValidationException
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(PaymentValidationException.class)
    public ResponseEntity<ErrorResponse> handlePaymentValidationException(
            PaymentValidationException ex,
            WebRequest request) {
        
        logger.warn("Payment validation error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Error")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles CardValidationException.
     * 
     * Returns 400 Bad Request with card validation error details.
     * 
     * @param ex The CardValidationException
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(CardValidationException.class)
    public ResponseEntity<ErrorResponse> handleCardValidationException(
            CardValidationException ex,
            WebRequest request) {
        
        logger.warn("Card validation error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Card Validation Error")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles DuplicatePaymentException.
     * 
     * Returns 409 Conflict when duplicate payment detected.
     * 
     * @param ex The DuplicatePaymentException
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePaymentException(
            DuplicatePaymentException ex,
            WebRequest request) {
        
        logger.warn("Duplicate payment detected: transaction_id={}, merchant_id={}, idempotency_key={}",
            ex.getTransactionId(), ex.getMerchantId(), ex.getIdempotencyKey());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Duplicate Payment")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    /**
     * Handles MethodArgumentNotValidException (Spring validation errors).
     * 
     * Returns 400 Bad Request with detailed field validation errors.
     * 
     * @param ex The MethodArgumentNotValidException
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        Map<String, List<String>> fieldErrors = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.computeIfAbsent(error.getField(), k -> new ArrayList<>())
                .add(error.getDefaultMessage())
        );
        
        String errorMessage = fieldErrors.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
            .collect(Collectors.joining("; "));
        
        logger.warn("Request validation error: {}", errorMessage);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Error")
            .message(errorMessage)
            .path(request.getDescription(false).replace("uri=", ""))
            .fieldErrors(fieldErrors)
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles generic exceptions.
     * 
     * Returns 500 Internal Server Error.
     * 
     * @param ex The exception
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        
        logger.error("Unexpected error", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Standard error response format.
     */
    @lombok.Data
    @lombok.Builder
    public static class ErrorResponse {
        private Instant timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, List<String>> fieldErrors;
    }
}
