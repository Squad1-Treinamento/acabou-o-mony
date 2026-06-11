package com.acabouomony.engine.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.acabouomony.engine.exception.InvalidTokenException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.test.StepVerifier;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hs256!!";
    private static final SecretKey SIGNING_KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, 600);
    }

    @Test
    void shouldVerifyValidToken() {
        var now = Instant.now();
        var token = Jwts.builder()
                .claim("challenge_id", "ch-001")
                .claim("transaction_id", "txn-001")
                .claim("merchant_id", "merchant-1")
                .claim("amount", "150.00")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(600)))
                .signWith(SIGNING_KEY)
                .compact();

        StepVerifier.create(provider.verify(token))
                .assertNext(claims -> {
                    assertThat(claims.challengeId()).isEqualTo("ch-001");
                    assertThat(claims.transactionId()).isEqualTo("txn-001");
                    assertThat(claims.merchantId()).isEqualTo("merchant-1");
                    assertThat(claims.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
                    assertThat(claims.issuedAt()).isNotNull();
                    assertThat(claims.expiresAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectExpiredToken() {
        var now = Instant.now();
        var token = Jwts.builder()
                .claim("challenge_id", "ch-002")
                .claim("transaction_id", "txn-002")
                .claim("merchant_id", "merchant-1")
                .claim("amount", "200.00")
                .issuedAt(Date.from(now.minusSeconds(1200)))
                .expiration(Date.from(now.minusSeconds(600)))
                .signWith(SIGNING_KEY)
                .compact();

        StepVerifier.create(provider.verify(token))
                .expectErrorSatisfies(err -> {
                    assertThat(err)
                            .isInstanceOf(InvalidTokenException.class)
                            .hasMessageContaining("expired");
                })
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() {
        var differentKey = Keys.hmacShaKeyFor("another-256-bit-secret-key-for-testing-purposes!".getBytes());
        var now = Instant.now();
        var token = Jwts.builder()
                .claim("challenge_id", "ch-003")
                .claim("transaction_id", "txn-003")
                .claim("merchant_id", "merchant-1")
                .claim("amount", "300.00")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(600)))
                .signWith(differentKey)
                .compact();

        StepVerifier.create(provider.verify(token))
                .expectErrorSatisfies(err -> {
                    assertThat(err)
                            .isInstanceOf(InvalidTokenException.class)
                            .hasMessageContaining("Invalid");
                })
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void shouldRejectMalformedToken() {
        StepVerifier.create(provider.verify("not-a-jwt-token"))
                .expectErrorSatisfies(err -> {
                    assertThat(err)
                            .isInstanceOf(InvalidTokenException.class)
                            .hasMessageContaining("Invalid");
                })
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void shouldRejectTokenWithMissingClaims() {
        var now = Instant.now();
        var token = Jwts.builder()
                .claim("challenge_id", "ch-004")
                .claim("transaction_id", "txn-004")
                .claim("merchant_id", "merchant-1")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(600)))
                .signWith(SIGNING_KEY)
                .compact();

        StepVerifier.create(provider.verify(token))
                .expectErrorSatisfies(err -> {
                    assertThat(err)
                            .isInstanceOf(InvalidTokenException.class)
                            .hasMessageContaining("missing required claims");
                })
                .verify(Duration.ofSeconds(5));
    }

}
