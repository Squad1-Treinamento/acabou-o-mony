package com.acabouomony.engine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ThreeDsSessionResponse(
        @JsonProperty("challenge_id") String challengeId,
        @JsonProperty("acs_url")      String acsUrl,
        @JsonProperty("jwt")          String jwt
) {}
