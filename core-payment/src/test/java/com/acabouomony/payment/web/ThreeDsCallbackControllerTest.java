package com.acabouomony.payment.web;

import com.acabouomony.payment.domain.service.PaymentOrchestrationService;
import com.acabouomony.payment.infrastructure.security.ApiKeyAuthenticationFilter;
import com.acabouomony.payment.infrastructure.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = ThreeDsCallbackController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, ApiKeyAuthenticationFilter.class})
    }
)
class ThreeDsCallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentOrchestrationService orchestrationService;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private String callbackJson(String txId, String authStatus) throws Exception {
        return mapper.writeValueAsString(Map.of(
                "challenge_id", "ch-001",
                "transaction_id", txId,
                "merchant_id", "merchant-1",
                "auth_status", authStatus,
                "authenticated_at", Instant.now().toString()
        ));
    }

    @Test
    void shouldReturn200OnApprovedCallback() throws Exception {
        UUID txId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/payments/3ds-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackJson(txId.toString(), "approved")))
                .andExpect(status().isOk());

        verify(orchestrationService).completeThreeDsAuthentication(txId, true);
    }

    @Test
    void shouldReturn200OnDeclinedCallback() throws Exception {
        UUID txId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/payments/3ds-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackJson(txId.toString(), "declined")))
                .andExpect(status().isOk());

        verify(orchestrationService).completeThreeDsAuthentication(txId, false);
    }

    @Test
    void shouldReturn200OnDuplicateCallback() throws Exception {
        UUID txId = UUID.randomUUID();
        String body = callbackJson(txId.toString(), "approved");

        mockMvc.perform(post("/api/v1/payments/3ds-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/payments/3ds-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(orchestrationService, times(2)).completeThreeDsAuthentication(txId, true);
    }
}
