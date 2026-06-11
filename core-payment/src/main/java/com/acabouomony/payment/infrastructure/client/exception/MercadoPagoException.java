package com.acabouomony.payment.infrastructure.client.exception;

/**
 * Exception for Mercado Pago API errors.
 * 
 * Wraps HTTP errors and API-specific error responses.
 * 
 * Spec: spec-001-core-payment-processing.md - Mercado Pago Integration
 * Task: task-011-mercado-pago-client.md
 */
public class MercadoPagoException extends RuntimeException {
    
    private final int httpStatus;
    private final String errorCode;
    private final boolean retryable;
    
    /**
     * Constructs MercadoPagoException with HTTP status and error code.
     * 
     * @param httpStatus HTTP status code from Mercado Pago
     * @param errorCode Error code from Mercado Pago API
     * @param message Error message
     * @param retryable Whether error is retryable
     */
    public MercadoPagoException(int httpStatus, String errorCode, String message, boolean retryable) {
        super(String.format("Mercado Pago error (HTTP %d, code=%s): %s", httpStatus, errorCode, message));
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.retryable = retryable;
    }
    
    /**
     * ConstructsException with cause. MercadoPago
     * 
     * @param httpStatus HTTP status code
     * @param errorCode Error code
     * @param message Error message
     * @param cause Root cause exception
     * @param retryable Whether error is retryable
     */
    public MercadoPagoException(int httpStatus, String errorCode, String message, Throwable cause, boolean retryable) {
        super(String.format("Mercado Pago error (HTTP %d, code=%s): %s", httpStatus, errorCode, message), cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.retryable = retryable;
    }
    
    /**
     * Constructs MercadoPagoException for timeout.
     * 
     * @param message Error message
     * @param cause Root cause exception
     */
    public MercadoPagoException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = 0;  // No HTTP status for timeout
        this.errorCode = "TIMEOUT";
        this.retryable = true;
    }
    
    // Getters
    public int getHttpStatus() {
        return httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
}
