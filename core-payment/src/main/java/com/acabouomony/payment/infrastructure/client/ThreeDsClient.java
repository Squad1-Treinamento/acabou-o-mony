package com.acabouomony.payment.infrastructure.client;

import com.acabouomony.payment.domain.exception.PaymentAcquirerException;
import com.acabouomony.payment.domain.model.RiskLevel;
import com.acabouomony.payment.infrastructure.client.dto.ThreeDsSessionRequestDTO;
import com.acabouomony.payment.infrastructure.client.dto.ThreeDsSessionResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class ThreeDsClient {

    private static final Logger log = LoggerFactory.getLogger(ThreeDsClient.class);
    private static final String SESSIONS_PATH = "/api/v1/3ds/sessions";

    private final RestTemplate restTemplate;
    private final String threeDsBaseUrl;
    private final String apiKey;

    public ThreeDsClient(RestTemplate restTemplate,
                         @Value("${3ds.base-url}") String threeDsBaseUrl,
                         @Value("${3ds.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.threeDsBaseUrl = threeDsBaseUrl;
        this.apiKey = apiKey;
    }

    public Optional<ThreeDsSessionResponseDTO> createSession(ThreeDsSessionRequestDTO request, RiskLevel risk) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);

        try {
            var response = restTemplate.exchange(
                    threeDsBaseUrl + SESSIONS_PATH,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    ThreeDsSessionResponseDTO.class);
            return Optional.ofNullable(response.getBody());
        } catch (ResourceAccessException e) {
            log.warn("3DS service unreachable: risk={}, error={}", risk, e.getMessage());
            if (risk == RiskLevel.LOW) {
                return Optional.empty();
            }
            throw new PaymentAcquirerException("3DS service unavailable", e);
        }
    }
}
