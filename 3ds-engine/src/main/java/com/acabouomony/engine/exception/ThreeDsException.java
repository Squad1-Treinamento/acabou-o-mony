package com.acabouomony.engine.exception;

public class ThreeDsException extends RuntimeException {

    private final String errorCode;

    public ThreeDsException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

}
