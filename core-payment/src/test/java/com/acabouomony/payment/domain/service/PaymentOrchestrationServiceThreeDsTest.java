package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentResult;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.event.ThreeDsCompletedEvent;
import com.acabouomony.payment.domain.exception.PaymentTimeoutException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.client.ThreeDsClient;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrchestrationServiceThreeDsTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private PaymentAcquirerClient paymentAcquirerClient;
    @Mock private RiskEvaluationService riskEvaluationService;
    @Mock private StateTransitionValidator stateTransitionValidator;
    @Mock private AuditLogService auditLogService;
    @Mock private OutboxEventService outboxEventService;
    @Mock private UnknownStateTransitionHandler unknownStateTransitionHandler;
    @Mock private ThreeDsClient threeDsClient;
    @Mock private ApplicationEventPublisher eventPublisher;

    private PaymentOrchestrationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentOrchestrationService(
                transactionRepository, paymentAcquirerClient, riskEvaluationService,
                stateTransitionValidator, auditLogService, outboxEventService,
                unknownStateTransitionHandler, threeDsClient, eventPublisher);
    }

    private Transaction buildTx(UUID id, PaymentStatus status) {
        return Transaction.builder()
                .id(id)
                .merchantId(UUID.randomUUID())
                .idempotencyKey(UUID.randomUUID())
                .amount(10000L)
                .currency("BRL")
                .status(status)
                .payloadHash("hash")
                .version(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void completeThreeDsAuthentication_approved_transitionsToAuthenticated() {
        UUID txId = UUID.randomUUID();
        Transaction tx = buildTx(txId, PaymentStatus.CHALLENGE_PENDING);
        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenReturn(tx);

        service.completeThreeDsAuthentication(txId, true);

        verify(transactionRepository).save(any());
        ArgumentCaptor<ThreeDsCompletedEvent> eventCaptor = ArgumentCaptor.forClass(ThreeDsCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().approved()).isTrue();
        assertThat(eventCaptor.getValue().txId()).isEqualTo(txId);
    }

    @Test
    void completeThreeDsAuthentication_declined_transitionsToDeclined() {
        UUID txId = UUID.randomUUID();
        Transaction tx = buildTx(txId, PaymentStatus.CHALLENGE_PENDING);
        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenReturn(tx);

        service.completeThreeDsAuthentication(txId, false);

        ArgumentCaptor<ThreeDsCompletedEvent> eventCaptor = ArgumentCaptor.forClass(ThreeDsCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().approved()).isFalse();
    }

    @Test
    void completeThreeDsAuthentication_idempotentIfAlreadyAuthenticated() {
        UUID txId = UUID.randomUUID();
        Transaction tx = buildTx(txId, PaymentStatus.AUTHENTICATED);
        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

        service.completeThreeDsAuthentication(txId, true);

        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void completeThreeDsAuthentication_idempotentIfAlreadyDeclined() {
        UUID txId = UUID.randomUUID();
        Transaction tx = buildTx(txId, PaymentStatus.DECLINED);
        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

        service.completeThreeDsAuthentication(txId, false);

        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void resumePaymentAfterAuth_successFlow() {
        UUID txId = UUID.randomUUID();
        Transaction tx = buildTx(txId, PaymentStatus.AUTHENTICATED);
        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenReturn(tx);
        PaymentResult result = mock(PaymentResult.class);
        when(result.getStatus()).thenReturn(PaymentStatus.COMPLETED);
        when(paymentAcquirerClient.submitPayment(any())).thenReturn(result);

        service.resumePaymentAfterAuth(txId);

        verify(paymentAcquirerClient).submitPayment(tx);
    }

    @Test
    void resumePaymentAfterAuth_timeoutDelegatesToUnknownHandler() {
        UUID txId = UUID.randomUUID();
        Transaction tx = buildTx(txId, PaymentStatus.AUTHENTICATED);
        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenReturn(tx);
        when(paymentAcquirerClient.submitPayment(any())).thenThrow(new PaymentTimeoutException("timeout"));

        service.resumePaymentAfterAuth(txId);

        verify(unknownStateTransitionHandler).transitionToUnknownDueToTimeout(eq(tx), any());
    }
}
