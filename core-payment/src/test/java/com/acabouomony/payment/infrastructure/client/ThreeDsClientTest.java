package com.acabouomony.payment.infrastructure.client;

import com.acabouomony.payment.domain.exception.PaymentAcquirerException;
import com.acabouomony.payment.domain.model.RiskLevel;
import com.acabouomony.payment.infrastructure.client.dto.ThreeDsSessionRequestDTO;
import com.acabouomony.payment.infrastructure.client.dto.ThreeDsSessionResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThreeDsClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ThreeDsClient client;

    @BeforeEach
    void setUp() {
        client = new ThreeDsClient(restTemplate, "http://localhost:9090", "test-api-key");
    }

    private ThreeDsSessionRequestDTO buildRequest() {
        return ThreeDsSessionRequestDTO.builder()
                .transactionId("txn-001")
                .merchantId("merchant-1")
                .amount(15000L)
                .currency("BRL")
                .cardToken("card-abc")
                .build();
    }

    @Test
    void shouldReturnResponseOnSuccess() {
        var body = ThreeDsSessionResponseDTO.builder()
                .challengeId("ch-001")
                .acsUrl("http://acs/challenge/ch-001")
                .jwt("token-xyz")
                .build();
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(ThreeDsSessionResponseDTO.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.CREATED));

        var result = client.createSession(buildRequest(), RiskLevel.HIGH);

        assertThat(result).isPresent();
        assertThat(result.get().getChallengeId()).isEqualTo("ch-001");
        assertThat(result.get().getAcsUrl()).isEqualTo("http://acs/challenge/ch-001");
        assertThat(result.get().getJwt()).isEqualTo("token-xyz");
    }

    @Test
    void shouldReturnEmptyOnTimeoutForLowRisk() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(ThreeDsSessionResponseDTO.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        var result = client.createSession(buildRequest(), RiskLevel.LOW);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowOnTimeoutForHighRisk() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(ThreeDsSessionResponseDTO.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        assertThatThrownBy(() -> client.createSession(buildRequest(), RiskLevel.HIGH))
                .isInstanceOf(PaymentAcquirerException.class)
                .hasMessageContaining("3DS service unavailable");
    }

    @Test
    void shouldThrowOnConnectionRefusedForHighRisk() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(ThreeDsSessionResponseDTO.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> client.createSession(buildRequest(), RiskLevel.HIGH))
                .isInstanceOf(PaymentAcquirerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldSendApiKeyHeader() {
        ArgumentCaptor<HttpEntity<ThreeDsSessionRequestDTO>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        var body = ThreeDsSessionResponseDTO.builder().challengeId("ch-002").build();

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), captor.capture(),
                eq(ThreeDsSessionResponseDTO.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.CREATED));

        client.createSession(buildRequest(), RiskLevel.HIGH);

        assertThat(captor.getValue().getHeaders().getFirst("X-API-Key")).isEqualTo("test-api-key");
    }
}
