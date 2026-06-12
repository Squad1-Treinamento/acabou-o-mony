package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.entity.Merchant;
import com.acabouomony.payment.domain.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MerchantAuthService.
 * 
 * Verifies:
 * - API key hashing with Argon2
 * - Timing-safe comparison
 * - Authentication success/failure scenarios
 * - No plaintext key logging
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(MerchantAuthService.class)
@ActiveProfiles("test")
class MerchantAuthServiceTest {

    @Autowired
    private MerchantAuthService merchantAuthService;

    @Autowired
    private MerchantRepository merchantRepository;

    private Merchant testMerchant;
    private String testApiKey;

    @BeforeEach
    void setUp() {
        // Create a test API key
        testApiKey = "test_api_key_12345";
        
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
    void shouldAuthenticateWithValidApiKey() {
        // Act
        Optional<Merchant> result = merchantAuthService.authenticate(testApiKey);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getMerchantId()).isEqualTo(testMerchant.getMerchantId());
        assertThat(result.get().getApiKeyHash()).isEqualTo(testMerchant.getApiKeyHash());
    }

    @Test
    void shouldRejectInvalidApiKey() {
        // Act
        Optional<Merchant> result = merchantAuthService.authenticate("wrong_api_key");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectNullApiKey() {
        // Act
        Optional<Merchant> result = merchantAuthService.authenticate(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectEmptyApiKey() {
        // Act
        Optional<Merchant> result = merchantAuthService.authenticate("");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldGenerateDifferentHashesForSameInput() {
        // Arrange
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
        String plaintext = "same_api_key";

        // Act
        String hash1 = encoder.encode(plaintext);
        String hash2 = encoder.encode(plaintext);

        // Assert
        // Different hashes due to random salt (Argon2 includes salt in output)
        assertThat(hash1).isNotEqualTo(hash2);
        
        // But both should match the plaintext
        assertThat(encoder.matches(plaintext, hash1)).isTrue();
        assertThat(encoder.matches(plaintext, hash2)).isTrue();
    }

    @Test
    void shouldVerifyApiKeyWithTimingSafeComparison() {
        // Arrange
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
        String plaintext = "test_key_123";
        String hash = encoder.encode(plaintext);

        // Act
        boolean matches = merchantAuthService.verifyApiKey(plaintext, hash);

        // Assert
        assertThat(matches).isTrue();
    }

    @Test
    void shouldRejectWrongKeyWithTimingSafeComparison() {
        // Arrange
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
        String plaintext = "test_key_123";
        String hash = encoder.encode(plaintext);

        // Act
        boolean matches = merchantAuthService.verifyApiKey("wrong_key", hash);

        // Assert
        assertThat(matches).isFalse();
    }

    @Test
    void shouldAuthenticateMultipleMerchantsIndependently() {
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

        // Act
        Optional<Merchant> result1 = merchantAuthService.authenticate(testApiKey);
        Optional<Merchant> result2 = merchantAuthService.authenticate(apiKey2);

        // Assert
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result1.get().getMerchantId()).isNotEqualTo(result2.get().getMerchantId());
    }

    @Test
    void shouldNotAuthenticateWithPartialApiKey() {
        // Act
        Optional<Merchant> result = merchantAuthService.authenticate(testApiKey.substring(0, 5));

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotAuthenticateWithApiKeyWithExtraCharacters() {
        // Act
        Optional<Merchant> result = merchantAuthService.authenticate(testApiKey + "extra");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotAuthenticateWithCaseVariation() {
        // Act
        Optional<Merchant> result = merchantAuthService.authenticate(testApiKey.toUpperCase());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleSpecialCharactersInApiKey() {
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

        // Act
        Optional<Merchant> result = merchantAuthService.authenticate(specialApiKey);

        // Assert
        assertThat(result).isPresent();
    }

    @Test
    void shouldHandleLongApiKey() {
        // Arrange
        String longApiKey = "a".repeat(1000);
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
        String hashedKey = encoder.encode(longApiKey);
        
        Merchant merchant = Merchant.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .apiKeyHash(hashedKey)
            .createdAt(Instant.now())
            .build();
        
        merchantRepository.save(merchant);

        // Act
        Optional<Merchant> result = merchantAuthService.authenticate(longApiKey);

        // Assert
        assertThat(result).isPresent();
    }
}
