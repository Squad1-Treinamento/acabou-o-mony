package com.acabouomony.engine.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(
        @NotBlank String challengeId,
        String mfaToken
) {
}
