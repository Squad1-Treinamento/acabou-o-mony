package com.acabouomony.payment.domain.model;

/**
 * Outbox event status enumeration.
 * 
 * Represents the lifecycle of webhook events in the transactional outbox pattern.
 * Spec: spec-001-core-payment-processing.md - Transactional Outbox Rules
 */
public enum OutboxEventStatus {
    /**
     * Event created and pending delivery to merchant webhook endpoint.
     */
    PENDING,

    /**
     * Event successfully delivered to merchant (received 200 OK).
     */
    DELIVERED,

    /**
     * Event delivery failed after maximum retry attempts (5 retries).
     */
    FAILED
}
