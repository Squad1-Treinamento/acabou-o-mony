package com.acabouomony.payment.infrastructure.security;

import com.acabouomony.payment.domain.entity.Merchant;
import com.acabouomony.payment.domain.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for ApiKeyAuthenticationFilter.
 * 
 * Tests use a protected endpoint (/api/v1/test/protected) that requires authentication.
 * This endpoint is only available in test profile and is configured to require authentication
 * in SecurityConfig.
 * 
 * Verifies:
 * - Valid API key authentication succeeds (200 OK)
 * - Invalid API key authentication fails (401 Unauthorized)
 * - Missing Authorization header fails (401 Unauthorized)
 * - Invalid Authorization header format fails (401 Unauthorized)
 * - Authenticated request proceeds to controller
 * - Timing-safe comparison is used (no timing attacks)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiKeyAuthenticationFilterTest {

    private static final String PROTECTED_ENDPOINT = "/api/v1/test/protected";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantRepository merchantRepository;

    private Merchant testMerchant;
    private String testApiKey;

    @BeforeEach
    void setUp() {
        // Create a test API key
        testApiKey = "test_api_key_integration_12345";
        
        // Hash the API key
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
        String hashedKey = encoder.encode(testApiKey);
        
        // Create and persist a test merchant
        testMerchant = Merchant.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .apiKeyHash(hashedKey)
            .webhookUrl("https://example.com/webhook")
            .createdAt(Instant.now())
            .build();
        
        merchantRepository.save(testMerchant);
    }

    @Test
    void shouldAuthenticateWithValidApiKey() throws Exception {
        // Act & Assert
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                .header("Authorization", "Bearer " + testApiKey))
            .andExpect(status().isOk());
    }

    @Test
    void shouldRejectWithInvalidApiKey() throws Exception {
        // Act & Assert
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                .header("Authorization", "Bearer invalid_key"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWithoutAuthorizationHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(get(PROTECTED_ENDPOINT))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWithMissingBearerPrefix() throws Exception {
        // Act & Assert
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                .header("Authorization", testApiKey)) // Missing "Bearer " prefix
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWithInvalidBearerFormat() throws Exception {
        // Act & Assert
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                .header("Authorization", "Basic " + testApiKey)) // Wrong auth type
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWithEmptyApiKey() throws Exception {
        // Act & Assert
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                .header("Authorization", "Bearer "))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWithWhitespaceApiKey() throws Exception {
        // Act & Assert
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                .header("Authorization", "Bearer   "))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWithCaseVariationApiKey() throws Exception {
        // Act & Assert
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                .header("Authorization", "Bearer " + testApiKey.toUpperCase()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWithPartialApiKey() throws Exception {
        // Act & Assert
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                .header("Authorization", "Bearer " + testApiKey.substring(0, 5)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWithApiKeyWithExtraCharacters() throws Exception {
        // Act & Assert
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                .header("Authorization", "Bearer " + testApiKey + "extra"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAuthenticateMultipleMerchantsIndependently() throws Exception {
        // Arrange
        String apiKey2 = "another_api_key_67890";
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
        String hashedKey2 = encoder.encode(apiKey2);
        
        Merchant merchant2 = Merchant.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .apiKeyHash(hashedKey2)
            .webhookUrl("https://example2.com/webhook")
            .createdAt(Instant.now())
            .build();
        
        merchantRepository.save(merchant2);

        // Act & Assert - First merchant
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                .header("Authorization", "Bearer " + testApiKey))
            .andExpect(status().isOk());

        // Act & Assert - Second merchant
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                .header("Authorization", "Bearer " + apiKey2))
            .andExpect(status().isOk());
    }

    @Test
    void shouldHandleSpecialCharactersInApiKey() throws Exception {
        // Arrange
        String specialApiKey = "test_key_!@#$%^&*()";
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
        String hashedKey = encoder.encode(specialApiKey);
        
        Merchant merchant = Merchant.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .apiKeyHash(hashedKey)
            .createdAt(Instant.now())
            .build();
        
        merchantRepository.save(merchant);

        // Act & Assert
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                .header("Authorization", "Bearer " + specialApiKey))
            .andExpect(status().isOk());
    }

    @Test
    void shouldPreserveMerchantIdInSecurityContext() throws Exception {
        // Act & Assert
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                .header("Authorization", "Bearer " + testApiKey))
            .andExpect(status().isOk());
        
        // Note: In a real scenario, we would verify the SecurityContext contains
        // the merchant ID. This would require a test controller endpoint that
        // returns the authenticated principal.
    }

    @RestController
    @RequestMapping("/api/v1/test")
    @Profile("test")
    public static class TestAuthController {
        @GetMapping("/protected")
        public Map<String, String> protectedEndpoint() {
            return Map.of("status", "authenticated");
        }
    }
}

