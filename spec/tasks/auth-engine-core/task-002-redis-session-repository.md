---
id: task-002
status: planned
links:
  - spec/tasks/index.md
  - spec/tech-plans/plan-001-3ds-mfa-auth-engine.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
---
# Implement Redis Session Repository with Reactive CRUD Operations

## Local Context

- **Directory:** `3ds-engine/`
- **Files to create:**
  - `3ds-engine/src/main/java/com/acabouomony/engine/config/RedisConfig.java` — ReactiveRedisTemplate bean
  - `3ds-engine/src/main/java/com/acabouomony/engine/model/ChallengeSession.java` — HASH model
  - `3ds-engine/src/main/java/com/acabouomony/engine/model/AuthResult.java` — Auth result value model
  - `3ds-engine/src/main/java/com/acabouomony/engine/repository/ChallengeSessionRepository.java` — ReactiveRedisTemplate wrapper
- **Dependencies:** `ReactiveRedisTemplate`, `RedisClusterConfiguration`, `application.yml` (Redis connection)

## Scope

1. Configure `RedisConfig` with `ReactiveRedisConnectionFactory` and `ReactiveRedisTemplate<String, Object>` beans. Read cluster nodes from `spring.redis.cluster.nodes`.
2. Create `ChallengeSession` model with HASH fields: `transaction_id`, `merchant_id`, `amount` (BigDecimal), `currency`, `card_token` (tokenized, never raw PAN), `acs_url` (String, the bank's ACS URL resolved by Core), `status` (PENDING|APPROVED|DECLINED|EXPIRED), `created_at` (Instant), `ttl` (long, seconds).
3. Create `AuthResult` simple model: `auth_status` (APPROVED|DECLINED), `challenge_id`, `transaction_id`, `authenticated_at` (Instant).
4. Implement `ChallengeSessionRepository` as `@Component` wrapping `ReactiveRedisTemplate<String, Object>`:
   - `saveSession(challengeId, ChallengeSession)` — writes HASH to `3ds:session:{challengeId}`, sets TTL from config
   - `findSessionById(challengeId)` — reads HASH, returns `Mono<ChallengeSession>` or `Mono.empty()`
   - `updateSessionStatus(challengeId, status)` — updates single field in HASH
   - `saveAuthResult(challengeId, AuthResult)` — writes STRING to `3ds:auth:{challengeId}` with 24h TTL
   - `findAuthResult(challengeId)` — reads STRING, returns `Mono<AuthResult>` or `Mono.empty()`

## Acceptance Criteria and Tests

- **Success:** Session saved to Redis is retrievable by `challenge_id` with all fields intact. Auth result saved and retrievable.
- **Failure:** Non-existent `challenge_id` returns `Mono.empty()`.
- **Tests:** Unit test with mocked `ReactiveRedisTemplate`. Verify key namespace follows `3ds:session:{id}` and `3ds:auth:{id}` patterns.

## Constraints and Negative Instructions

- No PII (Personally Identifiable Information) stored in Redis values.
- All Redis keys must use the `3ds:*` namespace prefix.
- TTL must be configurable via `application.yml`, not hardcoded.
- Must be fully reactive (`Mono`/`Flux`), never block.
