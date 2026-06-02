package com.acabouomony.engine.exception;

public class DuplicateChallengeException extends ThreeDsException {

    public DuplicateChallengeException(String challengeId) {
        super("DUPLICATE_CHALLENGE", "Challenge " + challengeId + " has already been processed");
    }

}
