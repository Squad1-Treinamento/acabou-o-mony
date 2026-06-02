package com.acabouomony.engine.exception;

public class ChallengeExpiredException extends ThreeDsException {

    public ChallengeExpiredException(String challengeId) {
        super("3DS_CHALLENGE_EXPIRED", "Challenge " + challengeId + " has expired");
    }

}
