package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.event.ThreeDsCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class ThreeDsPaymentFinalizer {

    private static final Logger log = LoggerFactory.getLogger(ThreeDsPaymentFinalizer.class);

    private final PaymentOrchestrationService orchestrationService;
    private final AuditLogService auditLogService;

    public ThreeDsPaymentFinalizer(PaymentOrchestrationService orchestrationService,
                                   AuditLogService auditLogService) {
        this.orchestrationService = orchestrationService;
        this.auditLogService = auditLogService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onThreeDsCompleted(ThreeDsCompletedEvent event) {
        if (!event.approved()) {
            log.info("3DS declined, no payment submission: txId={}", event.txId());
            return;
        }
        try {
            orchestrationService.resumePaymentAfterAuth(event.txId());
        } catch (Exception e) {
            log.error("Failed to resume payment after 3DS auth: txId={}, error={}", event.txId(), e.getMessage(), e);
        }
    }
}
