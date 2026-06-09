package com.acabouomony.engine.exception;

public class InvalidTokenException extends ThreeDsException {

    public InvalidTokenException(String message) {
        super("INVALID_TOKEN", message);
    }

}
