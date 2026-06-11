package com.acabouomony.payment.infrastructure.persistence;

import com.acabouomony.payment.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for AuditLog entities.
 * 
 * Provides persistence for audit trail entries. Audit logs are created
 * atomically in the same database transaction as transaction state updates.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    /**
     * Find all audit log entries for a transaction.
     * 
     * @param transactionId The transaction ID
     * @return List of audit log entries
     */
    List<AuditLog> findByTransactionId(UUID transactionId);
}
