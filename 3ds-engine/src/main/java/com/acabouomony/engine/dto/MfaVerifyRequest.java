package com.acabouomony.engine.dto;

public record MfaVerifyRequest(
        String challengeId,
        String mfaToken
) {
}
