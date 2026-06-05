package com.acabouomony.engine.security;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.acabouomony.engine.exception.InvalidTokenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final String secret;
    private final long expirationSeconds;
    private SecretKey key;

    public JwtTokenProvider(@Value("${jwt.secret:}") String secret,
                            @Value("${jwt.expiration-seconds:600}") long expirationSeconds) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters. Set JWT_SECRET environment variable.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Mono<JwtClaims> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            var challengeId = claims.get("challenge_id", String.class);
            var transactionId = claims.get("transaction_id", String.class);
            var merchantId = claims.get("merchant_id", String.class);
            var amountStr = claims.get("amount", String.class);

            if (challengeId == null || transactionId == null || merchantId == null || amountStr == null) {
                return Mono.error(new InvalidTokenException("Invalid JWT: missing required claims"));
            }

            var jwtClaims = new JwtClaims(
                    challengeId, transactionId, merchantId,
                    new BigDecimal(amountStr),
                    claims.getIssuedAt().toInstant(),
                    claims.getExpiration().toInstant()
            );

            return Mono.just(jwtClaims);

        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT: {}", e.getMessage());
            return Mono.error(new InvalidTokenException("JWT has expired"));
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return Mono.error(new InvalidTokenException("Invalid JWT token"));
        }
    }

    public String sign(String challengeId, String transactionId, String merchantId, BigDecimal amount) {
        var now = Instant.now();
        var exp = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
                .claim("challenge_id", challengeId)
                .claim("transaction_id", transactionId)
                .claim("merchant_id", merchantId)
                .claim("amount", amount.toPlainString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }
}
