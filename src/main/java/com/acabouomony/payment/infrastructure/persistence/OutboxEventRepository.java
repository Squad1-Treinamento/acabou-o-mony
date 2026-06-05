package com.acabouomony.payment.infrastructure.persistence;

import com.acabouomony.payment.domain.entity.OutboxEvent;
import com.acabouomony.payment.domain.model.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for OutboxEvent entity.
 * 
 * Provides database access for transactional outbox pattern implementation.
 * Spec: spec-001-core-payment-processing.md - Transactional Outbox Rules
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    /**
     * Find all pending outbox events for processing.
     * 
     * @return List of events with status PENDING
     */
    List<OutboxEvent> findByStatus(OutboxEventStatus status);

    /**
     * Find all failed outbox events for operator review.
     * 
     * @return List of events with status FAILED
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status);
}
