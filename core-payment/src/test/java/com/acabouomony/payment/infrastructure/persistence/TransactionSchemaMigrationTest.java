package com.acabouomony.payment.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Flyway database migrations.
 * 
 * Verifies that:
 * - All tables are created with correct schema
 * - All indexes are created
 * - All constraints are enforced
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class TransactionSchemaMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateTransactionsTable() {
        // Verify transactions table exists
        String query = "SELECT table_name FROM information_schema.tables WHERE table_name = 'transactions'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
        assertThat(result).isNotEmpty();
    }

    @Test
    void shouldCreateAuditLogsTable() {
        // Verify audit_logs table exists
        String query = "SELECT table_name FROM information_schema.tables WHERE table_name = 'audit_logs'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
        assertThat(result).isNotEmpty();
    }

    @Test
    void shouldCreateOutboxEventsTable() {
        // Verify outbox_events table exists
        String query = "SELECT table_name FROM information_schema.tables WHERE table_name = 'outbox_events'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
        assertThat(result).isNotEmpty();
    }

    @Test
    void shouldCreateTransactionIndexes() {
        // Verify all required indexes exist on transactions table
        String query = "SELECT indexname FROM pg_indexes WHERE tablename = 'transactions'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
        
        List<String> indexNames = result.stream()
            .map(row -> (String) row.get("indexname"))
            .toList();
        
        assertThat(indexNames)
            .contains("idx_merchant_id", "idx_status", "idx_created_at", "idx_merchant_id_created_at");
    }

    @Test
    void shouldCreateAuditLogIndexes() {
        // Verify all required indexes exist on audit_logs table
        String query = "SELECT indexname FROM pg_indexes WHERE tablename = 'audit_logs'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
        
        List<String> indexNames = result.stream()
            .map(row -> (String) row.get("indexname"))
            .toList();
        
        assertThat(indexNames)
            .contains("idx_transaction_id", "idx_created_at");
    }

    @Test
    void shouldCreateOutboxEventIndexes() {
        // Verify all required indexes exist on outbox_events table
        String query = "SELECT indexname FROM pg_indexes WHERE tablename = 'outbox_events'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
        
        List<String> indexNames = result.stream()
            .map(row -> (String) row.get("indexname"))
            .toList();
        
        assertThat(indexNames)
            .contains("idx_status", "idx_created_at");
    }

    @Test
    void shouldCreateUniqueConstraintOnMerchantIdAndIdempotencyKey() {
        // Verify UNIQUE constraint exists
        String query = "SELECT constraint_name FROM information_schema.table_constraints " +
                      "WHERE table_name = 'transactions' AND constraint_type = 'UNIQUE'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
        
        List<String> constraintNames = result.stream()
            .map(row -> (String) row.get("constraint_name"))
            .toList();
        
        assertThat(constraintNames).contains("uk_merchant_id_idempotency_key");
    }

    @Test
    void shouldCreateCheckConstraintOnTransactionStatus() {
        // Verify CHECK constraint exists on transactions.status
        String query = "SELECT constraint_name FROM information_schema.table_constraints " +
                      "WHERE table_name = 'transactions' AND constraint_type = 'CHECK'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
        
        assertThat(result).isNotEmpty();
    }

    @Test
    void shouldCreateCheckConstraintOnOutboxEventStatus() {
        // Verify CHECK constraint exists on outbox_events.status
        String query = "SELECT constraint_name FROM information_schema.table_constraints " +
                      "WHERE table_name = 'outbox_events' AND constraint_type = 'CHECK'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
        
        assertThat(result).isNotEmpty();
    }

    @Test
    void shouldCreateForeignKeyConstraint() {
        // Verify foreign key constraint exists: audit_logs.transaction_id -> transactions.id
        String query = "SELECT constraint_name FROM information_schema.table_constraints " +
                      "WHERE table_name = 'audit_logs' AND constraint_type = 'FOREIGN KEY'";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
        
        assertThat(result).isNotEmpty();
    }
}
