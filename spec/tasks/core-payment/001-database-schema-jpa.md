---
id: task-001
status: complete
links:
  - spec/tech-plans/plan-001-core-payment-processing.md (Phase 1)
  - spec/specs/spec-001-core-payment-processing.md (Persistence Rules)
  - ARCHITECTURE.md
---

# Implement Database Schema & JPA Entities

Create PostgreSQL schema and Spring Data JPA entity classes for transaction persistence layer.

## Local Context

**Files to create/modify:**
- `src/main/resources/db/migration/V1__create_transactions_schema.sql` (Flyway migration)
- `src/main/java/com/acabouomony/payment/domain/entity/Transaction.java`
- `src/main/java/com/acabouomony/payment/domain/entity/AuditLog.java`
- `src/main/java/com/acabouomony/payment/domain/entity/OutboxEvent.java`

**Local dependencies:**
- Spring Data JPA
- Hibernate JPA provider
- PostgreSQL driver
- Flyway for migrations

## Scope

1. **Create Flyway migration V1__create_transactions_schema.sql:**
   - transactions table (UUID PK, merchant_id, idempotency_key, amount, currency, status, payload_hash, masked_card, card_token_id, acquirer_reference, version, created_at, updated_at)
   - UNIQUE constraint on (merchant_id, idempotency_key)
   - Indexes: merchant_id, status, created_at
   - audit_logs table (id, transaction_id FK, old_status, new_status, actor, checksum, created_at)
   - Indexes: transaction_id, created_at
   - outbox_events table (id, event_type, aggregate_id, payload, status, retry_count, created_at, updated_at, delivered_at)
   - Indexes: status, created_at

2. **Create Transaction JPA entity:**
   - @Entity, @Table(name="transactions")
   - @Id private UUID id
   - @Version private Integer version (optimistic locking)
   - All required fields as per schema
   - No business logic (persistence only)

3. **Create AuditLog JPA entity:**
   - @Entity, @Table(name="audit_logs")
   - @Id private UUID id
   - @ManyToOne Transaction relationship
   - All required fields

4. **Create OutboxEvent JPA entity:**
   - @Entity, @Table(name="outbox_events")
   - @Id private UUID id
   - All required fields including status (PENDING/DELIVERED/FAILED)

5. **Configure Hibernate:**
   - Set spring.jpa.hibernate.ddl-auto=validate (migrations managed by Flyway)
   - Set spring.jpa.show-sql=false (production)

## Acceptance Criteria & Tests

**Success cases:**
- ✓ Migration runs successfully, creates all 3 tables with correct schema
- ✓ Transaction entity loads from database with all fields populated
- ✓ @Version field is not null (defaults to 0)
- ✓ UNIQUE constraint on (merchant_id, idempotency_key) enforced at DB level
- ✓ Inserting duplicate (merchant_id, idempotency_key) raises constraint violation

**Failure cases:**
- ✗ Inserting transaction without merchant_id fails (NOT NULL constraint)
- ✗ Inserting transaction with invalid status fails (CHECK constraint if added)
- ✗ Invalid UUID format rejected

**Required tests:**
- Integration test: Create transaction, verify all fields persisted correctly
- Integration test: Attempt duplicate (merchant_id, idempotency_key) insert, verify exception
- Integration test: Verify @Version field initialized to 0
- Integration test: Create audit log linked to transaction via foreign key
- Migration test: Verify Flyway migration applies cleanly to fresh database

**Verification:**
```bash
mvn test -Dtest="*SchemaTest,*EntityTest"
```

## Constraints & Negative Instructions

- Do NOT add business logic to entities (persistence only)
- Do NOT create multiple migration files (use single V1 for atomic schema)
- Do NOT add application-level validation (database constraints are source of truth)
- Do NOT modify entities after migration (schema is immutable for Phase 1)
- Do NOT add @OneToMany collections (causes N+1 queries; use explicit queries instead)
