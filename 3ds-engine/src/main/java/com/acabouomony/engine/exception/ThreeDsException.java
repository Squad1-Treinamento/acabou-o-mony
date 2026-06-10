package com.acabouomony.engine.exception;

public class ThreeDsException extends RuntimeException {

    private final String errorCode;
    private final String challengeId;

    public ThreeDsException(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public ThreeDsException(String errorCode, String message, String challengeId) {
        super(message);
        this.errorCode = errorCode;
        this.challengeId = challengeId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getChallengeId() {
        return challengeId;
    }

}
