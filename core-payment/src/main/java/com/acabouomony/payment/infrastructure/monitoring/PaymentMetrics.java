package com.acabouomony.payment.infrastructure.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Metrics collection for payment processing.
 * 
 * Tracks:
 * - UNKNOWN state transitions (timeouts, acquirer errors)
 * - Timeout durations
 * - Optimistic lock conflicts
 * - State transition latencies
 * 
 * Spec: spec-001-core-payment-processing.md - Monitoring & Alerting
 * Task: task-013-unknown-state-handling.md
 * 
 * NOTE: This is a simple logging-based implementation since Micrometer is not available.
 * In production, this should be replaced with actual metrics collection (Micrometer/Prometheus).
 *
 * Metrics logged at INFO level:
 * - METRIC: payment.unknown.transitions.total
 * - METRIC: payment.timeout.errors.total
 * - METRIC: payment.acquirer.errors.total
 * - METRIC: payment.optimistic.lock.conflicts.total
 * - METRIC: payment.state.transition.duration
 * - METRIC: payment.mercado.pago.call.duration
 */
@Component
public class PaymentMetrics {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentMetrics.class);
    /**
     * Records a transition to UNKNOWN state.
     */
    public void recordUnknownStateTransition() {
        logger.info("METRIC: payment.unknown.transitions.total +1");
    }
    
    /**
     * Records a timeout error.
     */
    public void recordTimeoutError() {
        logger.info("METRIC: payment.timeout.errors.total +1");
    }
    
    /**
     * Records an acquirer error.
     */
    public void recordAcquirerError() {
        logger.info("METRIC: payment.acquirer.errors.total +1");
    }
    
    /**
     * Records an optimistic lock conflict.
     */
    public void recordOptimisticLockConflict() {
        logger.info("METRIC: payment.optimistic.lock.conflicts.total +1");
    }
    
    /**
     * Records state transition duration.
     * 
     * @param durationMs Duration in milliseconds
     */
    public void recordStateTransitionDuration(long durationMs) {
        logger.info("METRIC: payment.state.transition.duration {}ms", durationMs);
    }
    
    /**
     * Records Mercado Pago API call duration.
     * 
     * @param durationMs Duration in milliseconds
     */
    public void recordMercadoPagoCallDuration(long durationMs) {
        logger.info("METRIC: payment.mercado.pago.call.duration {}ms", durationMs);
    }
}

