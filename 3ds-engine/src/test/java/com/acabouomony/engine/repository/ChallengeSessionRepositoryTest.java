package com.acabouomony.engine.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import com.acabouomony.engine.model.AuthResult;
import com.acabouomony.engine.model.ChallengeSession;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ChallengeSessionRepositoryTest {

    @Mock
    private ReactiveRedisTemplate<String, Object> redis;

    @Mock
    private ReactiveRedisTemplate<String, AuthResult> authRedis;

    @Mock
    private ReactiveHashOperations<String, Object, Object> hashOps;

    @Mock
    private ReactiveValueOperations<String, Object> valueOps;

    @Mock
    private ReactiveValueOperations<String, AuthResult> authValueOps;

    private ChallengeSessionRepository repo;

    @BeforeEach
    void setUp() {
        repo = new ChallengeSessionRepository(redis, authRedis, 600L, 86400L);
    }

    private ChallengeSession createSession() {
        return new ChallengeSession(
                "txn-001", "merchant-1", new BigDecimal("150.00"),
                "BRL", "card-token-abc", "https://acs.bank.com/auth",
                "pending", Instant.parse("2026-06-02T10:00:00Z"), 600L);
    }

    @Test
    void saveSessionShouldPutAllHashAndSetExpire() {
        when(redis.opsForHash()).thenReturn(hashOps);

        var session = createSession();
        var key = "3ds:session:ch-001";

        when(hashOps.putAll(eq(key), any(Map.class))).thenReturn(Mono.just(true));
        when(redis.expire(eq(key), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(repo.saveSession("ch-001", session))
                .verifyComplete();

        verify(hashOps).putAll(eq(key), any(Map.class));
        verify(redis).expire(eq(key), eq(Duration.ofSeconds(600L)));
    }

    @Test
    void findSessionByIdShouldReturnSessionWhenExists() {
        when(redis.opsForHash()).thenReturn(hashOps);

        var key = "3ds:session:ch-001";
        var entries = Map.<Object, Object>of(
                "transaction_id", "txn-001",
                "merchant_id", "merchant-1",
                "amount", "150.00",
                "currency", "BRL",
                "card_token", "card-token-abc",
                "acs_url", "https://acs.bank.com/auth",
                "status", "pending",
                "created_at", "2026-06-02T10:00:00Z",
                "ttl", "600");

        when(hashOps.entries(key)).thenReturn(
                reactor.core.publisher.Flux.fromIterable(entries.entrySet()));

        StepVerifier.create(repo.findSessionById("ch-001"))
                .assertNext(session -> {
                    assertThat(session.getTransactionId()).isEqualTo("txn-001");
                    assertThat(session.getMerchantId()).isEqualTo("merchant-1");
                    assertThat(session.getAmount()).isEqualByComparingTo("150.00");
                    assertThat(session.getCurrency()).isEqualTo("BRL");
                    assertThat(session.getCardToken()).isEqualTo("card-token-abc");
                    assertThat(session.getAcsUrl()).isEqualTo("https://acs.bank.com/auth");
                    assertThat(session.getStatus()).isEqualTo("pending");
                })
                .verifyComplete();
    }

    @Test
    void findSessionByIdShouldEmitEmptyWhenNotFound() {
        when(redis.opsForHash()).thenReturn(hashOps);

        var key = "3ds:session:ch-999";

        when(hashOps.entries(key)).thenReturn(
                reactor.core.publisher.Flux.<Map.Entry<Object, Object>>empty());

        StepVerifier.create(repo.findSessionById("ch-999"))
                .verifyComplete();
    }

    @Test
    void updateSessionStatusShouldPutStatusField() {
        when(redis.opsForHash()).thenReturn(hashOps);

        var key = "3ds:session:ch-001";

        when(hashOps.put(key, "status", "approved")).thenReturn(Mono.just(true));

        StepVerifier.create(repo.updateSessionStatus("ch-001", "approved"))
                .verifyComplete();

        verify(hashOps).put(key, "status", "approved");
    }

    @Test
    void saveAuthResultShouldSetValueWithTtl() {
        when(authRedis.opsForValue()).thenReturn(authValueOps);

        var result = new AuthResult("approved", "ch-001", "txn-001", Instant.now());
        var key = "3ds:auth:ch-001";

        when(authValueOps.set(eq(key), eq(result), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(repo.saveAuthResult("ch-001", result))
                .verifyComplete();

        verify(authValueOps).set(eq(key), eq(result), eq(Duration.ofSeconds(86400L)));
    }

    @Test
    void findAuthResultShouldReturnResultWhenExists() {
        when(authRedis.opsForValue()).thenReturn(authValueOps);

        var result = new AuthResult("approved", "ch-001", "txn-001", Instant.now());
        var key = "3ds:auth:ch-001";

        when(authValueOps.get(key)).thenReturn(Mono.just(result));

        StepVerifier.create(repo.findAuthResult("ch-001"))
                .expectNext(result)
                .verifyComplete();
    }

    @Test
    void findAuthResultShouldEmitEmptyWhenNotFound() {
        when(authRedis.opsForValue()).thenReturn(authValueOps);

        var key = "3ds:auth:ch-999";

        when(authValueOps.get(key)).thenReturn(Mono.empty());

        StepVerifier.create(repo.findAuthResult("ch-999"))
                .verifyComplete();
    }
}
