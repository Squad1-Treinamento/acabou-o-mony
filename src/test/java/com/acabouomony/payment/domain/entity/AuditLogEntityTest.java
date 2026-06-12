package com.acabouomony.payment.domain.entity;

import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.AuditLogRepository;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for AuditLog entity persistence.
 * 
 * Verifies:
 * - Audit logs linked to transactions via foreign key
 * - Checksum persisted correctly
 * - Audit logs are INSERT-ONLY (UPDATE attempts fail)
 * - Immutability enforced at database level
 * - Multiple audit logs for same transaction linked correctly
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class AuditLogEntityTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        // Create a transaction for audit log tests
        transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(1000L)
            .currency("USD")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash1")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        transactionRepository.save(transaction);
    }

    @Test
    void shouldCreateAuditLogLinkedToTransaction() {
        // Arrange
        UUID auditLogId = UUID.randomUUID();
        Instant now = Instant.now();

        AuditLog auditLog = AuditLog.builder()
            .id(auditLogId)
            .transaction(transaction)
            .oldStatus(null)
            .newStatus(PaymentStatus.CREATED)
            .actor("system")
            .checksum("checksum123")
            .createdAt(now)
            .build();

        // Act
        AuditLog saved = auditLogRepository.save(auditLog);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(auditLogId);
        assertThat(saved.getTransaction().getId()).isEqualTo(transaction.getId());
        assertThat(saved.getOldStatus()).isNull();
        assertThat(saved.getNewStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(saved.getActor()).isEqualTo("system");
        assertThat(saved.getChecksum()).isEqualTo("checksum123");
        assertThat(saved.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void shouldPersistChecksumCorrectly() {
        // Arrange
        String checksum = "abc123def456ghi789";
        AuditLog auditLog = AuditLog.builder()
            .id(UUID.randomUUID())
            .transaction(transaction)
            .oldStatus(PaymentStatus.CREATED)
            .newStatus(PaymentStatus.VALIDATED)
            .actor("system")
            .checksum(checksum)
            .createdAt(Instant.now())
            .build();

        // Act
        AuditLog saved = auditLogRepository.save(auditLog);

        // Assert
        assertThat(saved.getChecksum()).isEqualTo(checksum);
    }

    @Test
    void shouldRejectAuditLogWithoutTransaction() {
        // Arrange
        AuditLog auditLog = AuditLog.builder()
            .id(UUID.randomUUID())
            .transaction(null)
            .oldStatus(null)
            .newStatus(PaymentStatus.CREATED)
            .actor("system")
            .checksum("checksum123")
            .createdAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> auditLogRepository.save(auditLog))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectAuditLogWithoutNewStatus() {
        // Arrange
        AuditLog auditLog = AuditLog.builder()
            .id(UUID.randomUUID())
            .transaction(transaction)
            .oldStatus(PaymentStatus.CREATED)
            .newStatus(null)
            .actor("system")
            .checksum("checksum123")
            .createdAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> auditLogRepository.save(auditLog))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectAuditLogWithoutActor() {
        // Arrange
        AuditLog auditLog = AuditLog.builder()
            .id(UUID.randomUUID())
            .transaction(transaction)
            .oldStatus(null)
            .newStatus(PaymentStatus.CREATED)
            .actor(null)
            .checksum("checksum123")
            .createdAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> auditLogRepository.save(auditLog))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectAuditLogWithoutChecksum() {
        // Arrange
        AuditLog auditLog = AuditLog.builder()
            .id(UUID.randomUUID())
            .transaction(transaction)
            .oldStatus(null)
            .newStatus(PaymentStatus.CREATED)
            .actor("system")
            .checksum(null)
            .createdAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> auditLogRepository.save(auditLog))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectAuditLogWithoutCreatedAt() {
        // Arrange
        AuditLog auditLog = AuditLog.builder()
            .id(UUID.randomUUID())
            .transaction(transaction)
            .oldStatus(null)
            .newStatus(PaymentStatus.CREATED)
            .actor("system")
            .checksum("checksum123")
            .createdAt(null)
            .build();

        // Act & Assert
        assertThatThrownBy(() -> auditLogRepository.save(auditLog))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowNullOldStatus() {
        // Arrange
        AuditLog auditLog = AuditLog.builder()
            .id(UUID.randomUUID())
            .transaction(transaction)
            .oldStatus(null)
            .newStatus(PaymentStatus.CREATED)
            .actor("system")
            .checksum("checksum123")
            .createdAt(Instant.now())
            .build();

        // Act
        AuditLog saved = auditLogRepository.save(auditLog);

        // Assert
        assertThat(saved.getOldStatus()).isNull();
    }

    @Test
    void shouldLinkMultipleAuditLogsToSameTransaction() {
        // Arrange
        AuditLog auditLog1 = AuditLog.builder()
            .id(UUID.randomUUID())
            .transaction(transaction)
            .oldStatus(null)
            .newStatus(PaymentStatus.CREATED)
            .actor("system")
            .checksum("checksum1")
            .createdAt(Instant.now())
            .build();

        AuditLog auditLog2 = AuditLog.builder()
            .id(UUID.randomUUID())
            .transaction(transaction)
            .oldStatus(PaymentStatus.CREATED)
            .newStatus(PaymentStatus.VALIDATED)
            .actor("system")
            .checksum("checksum2")
            .createdAt(Instant.now().plusSeconds(1))
            .build();

        // Act
        AuditLog saved1 = auditLogRepository.save(auditLog1);
        AuditLog saved2 = auditLogRepository.save(auditLog2);

        // Assert
        assertThat(saved1.getTransaction().getId()).isEqualTo(transaction.getId());
        assertThat(saved2.getTransaction().getId()).isEqualTo(transaction.getId());
        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
    }

    @Test
    void shouldRejectUpdateOnAuditLog() {
        // Arrange
        AuditLog auditLog = AuditLog.builder()
            .id(UUID.randomUUID())
            .transaction(transaction)
            .oldStatus(null)
            .newStatus(PaymentStatus.CREATED)
            .actor("system")
            .checksum("checksum123")
            .createdAt(Instant.now())
            .build();

        AuditLog saved = auditLogRepository.save(auditLog);

        // Act & Assert
        // Attempt to update the audit log (should fail due to trigger)
        saved.setActor("hacker");
        assertThatThrownBy(() -> auditLogRepository.save(saved))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("immutable");
    }

    @Test
    void shouldSupportAllValidPaymentStatusTransitions() {
        // Verify that all valid status transitions can be recorded
        PaymentStatus[] allStatuses = PaymentStatus.values();

        for (PaymentStatus status : allStatuses) {
            AuditLog auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .transaction(transaction)
                .oldStatus(null)
                .newStatus(status)
                .actor("system")
                .checksum("checksum_" + status.name())
                .createdAt(Instant.now())
                .build();

            AuditLog saved = auditLogRepository.save(auditLog);
            assertThat(saved.getNewStatus()).isEqualTo(status);
        }
    }
}
