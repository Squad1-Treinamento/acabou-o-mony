package com.acabouomony.payment.domain.exception;

public class MerchantAuthenticationException extends RuntimeException {

    public MerchantAuthenticationException(String message) {
        super(message);
    }

    public MerchantAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}