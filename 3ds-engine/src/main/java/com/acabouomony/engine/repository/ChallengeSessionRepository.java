package com.acabouomony.engine.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import com.acabouomony.engine.model.AuthResult;
import com.acabouomony.engine.model.ChallengeSession;

import reactor.core.publisher.Mono;

@Component
public class ChallengeSessionRepository {

    private static final String SESSION_KEY_PREFIX = "3ds:session:";
    private static final String AUTH_KEY_PREFIX = "3ds:auth:";

    private final ReactiveRedisTemplate<String, Object> redis;
    private final ReactiveRedisTemplate<String, AuthResult> authRedis;
    private final long sessionTtlSeconds;
    private final long authResultTtlSeconds;

    public ChallengeSessionRepository(
            ReactiveRedisTemplate<String, Object> redis,
            @Qualifier("authResultRedisTemplate") ReactiveRedisTemplate<String, AuthResult> authRedis,
            @Value("${3ds.session-ttl-seconds:600}") long sessionTtlSeconds,
            @Value("${3ds.auth-result-ttl-seconds:86400}") long authResultTtlSeconds) {
        this.redis = redis;
        this.authRedis = authRedis;
        this.sessionTtlSeconds = sessionTtlSeconds;
        this.authResultTtlSeconds = authResultTtlSeconds;
    }

    public Mono<Void> saveSession(String challengeId, ChallengeSession session) {
        var key = SESSION_KEY_PREFIX + challengeId;
        var hash = toSessionHash(session);
        return redis.opsForHash()
                .putAll(key, hash)
                .then(redis.expire(key, Duration.ofSeconds(sessionTtlSeconds)))
                .then();
    }

    public Mono<ChallengeSession> findSessionById(String challengeId) {
        var key = SESSION_KEY_PREFIX + challengeId;
        return redis.opsForHash()
                .entries(key)
                .collectMap(e -> (String) e.getKey(), e -> e.getValue())
                .filter(m -> !m.isEmpty())
                .map(this::fromSessionHash);
    }

    public Mono<Void> deleteSession(String challengeId) {
        var key = SESSION_KEY_PREFIX + challengeId;
        return redis.delete(key).then();
    }

    public Mono<Void> deleteAuthResult(String challengeId) {
        var key = AUTH_KEY_PREFIX + challengeId;
        return authRedis.delete(key).then();
    }


    public Mono<Void> updateSessionStatus(String challengeId, String status) {
        var key = SESSION_KEY_PREFIX + challengeId;
        return redis.opsForHash()
                .put(key, "status", status)
                .then();
    }

    public Mono<Void> saveAuthResult(String challengeId, AuthResult result) {
        var key = AUTH_KEY_PREFIX + challengeId;
        return authRedis.opsForValue()
                .set(key, result, Duration.ofSeconds(authResultTtlSeconds))
                .then();
    }

    public Mono<AuthResult> findAuthResult(String challengeId) {
        var key = AUTH_KEY_PREFIX + challengeId;
        return authRedis.opsForValue().get(key);
    }

    private Map<String, Object> toSessionHash(ChallengeSession s) {
        return Map.of(
                "transaction_id", s.getTransactionId(),
                "merchant_id", s.getMerchantId(),
                "amount", s.getAmount().toString(),
                "currency", s.getCurrency(),
                "card_token", s.getCardToken(),
                "acs_url", s.getAcsUrl(),
                "status", s.getStatus(),
                "created_at", s.getCreatedAt().toString(),
                "ttl", String.valueOf(s.getTtl())
        );
    }

    @SuppressWarnings("unchecked")
    private ChallengeSession fromSessionHash(Map<String, Object> hash) {
        var s = new ChallengeSession();
        s.setTransactionId((String) hash.get("transaction_id"));
        s.setMerchantId((String) hash.get("merchant_id"));
        s.setAmount(new java.math.BigDecimal((String) hash.get("amount")));
        s.setCurrency((String) hash.get("currency"));
        s.setCardToken((String) hash.get("card_token"));
        s.setAcsUrl((String) hash.get("acs_url"));
        s.setStatus((String) hash.get("status"));
        s.setCreatedAt(Instant.parse((String) hash.get("created_at")));
        s.setTtl(Long.parseLong((String) hash.get("ttl")));
        return s;
    }

}
