package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.event.ThreeDsCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreeDsPaymentFinalizerTest {

    @Mock
    private PaymentOrchestrationService orchestrationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ThreeDsPaymentFinalizer finalizer;

    @Test
    void approvedEvent_callsResumePaymentAfterAuth() {
        UUID txId = UUID.randomUUID();
        finalizer.onThreeDsCompleted(new ThreeDsCompletedEvent(txId, true));
        verify(orchestrationService).resumePaymentAfterAuth(txId);
    }

    @Test
    void declinedEvent_doesNotCallResumePaymentAfterAuth() {
        UUID txId = UUID.randomUUID();
        finalizer.onThreeDsCompleted(new ThreeDsCompletedEvent(txId, false));
        verify(orchestrationService, never()).resumePaymentAfterAuth(any());
    }

    @Test
    void exceptionInResumePayment_isCaughtAndNotRethrown() {
        UUID txId = UUID.randomUUID();
        doThrow(new RuntimeException("acquirer down")).when(orchestrationService).resumePaymentAfterAuth(txId);

        // should not throw
        finalizer.onThreeDsCompleted(new ThreeDsCompletedEvent(txId, true));

        verify(orchestrationService).resumePaymentAfterAuth(txId);
    }
}
