package com.acabouomony.engine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChallengeInitResponse(
        String status,
        @JsonProperty("challenge_id") String challengeId,
        @JsonProperty("redirect_url") String redirectUrl,
        @JsonProperty("jwt_token") String jwtToken,
        @JsonProperty("expires_in_seconds") long expiresInSeconds
) {
}
