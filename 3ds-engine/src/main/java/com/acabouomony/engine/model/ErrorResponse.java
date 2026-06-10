package com.acabouomony.engine.model;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String challenge_id,
        Instant timestamp
) {
}
