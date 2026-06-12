package com.acabouomony.payment.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ThreeDsSessionResponseDTO {
    @JsonProperty("challenge_id") private String challengeId;
    @JsonProperty("acs_url")      private String acsUrl;
    @JsonProperty("jwt")          private String jwt;
}
