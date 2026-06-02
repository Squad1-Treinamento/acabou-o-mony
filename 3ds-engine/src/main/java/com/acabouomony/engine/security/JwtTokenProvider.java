package com.acabouomony.engine.security;

import java.math.BigDecimal;
import java.time.Instant;

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

    private static final String DEV_FALLBACK_SECRET = "dev-secret-key-that-is-at-least-256-bits-long-for-hs256!!";

    private final SecretKey key;

    public JwtTokenProvider(@Value("${jwt.secret:}") String secret) {
        String effective = (secret == null || secret.isBlank() || secret.length() < 32)
                ? DEV_FALLBACK_SECRET
                : secret;
        if (effective == DEV_FALLBACK_SECRET) {
            log.warn("jwt.secret is not configured or too short — using dev fallback. Set JWT_SECRET env var in production.");
        }
        this.key = Keys.hmacShaKeyFor(effective.getBytes());
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
}
