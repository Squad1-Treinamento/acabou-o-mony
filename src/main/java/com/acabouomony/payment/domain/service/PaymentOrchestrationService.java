package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentResult;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.PaymentAcquirerException;
import com.acabouomony.payment.domain.exception.PaymentTimeoutException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentOrchestrationService.class);

    private final PaymentAcquirerClient paymentAcquirerClient;
    private final RiskEvaluationService riskEvaluationService;
    private final UnknownStateTransitionHandler unknownStateTransitionHandler;
    private final TransactionVersionService transactionVersionService;
    private final TransactionRepository transactionRepository;

    @Async
    @Transactional
    public void processNewPaymentAsync(Transaction transaction) {
        processPayment(transaction);
    }

    public void processPayment(Transaction transaction) {
        logger.info("Processing payment: transaction_id={}, amount={}, currency={}",
                transaction.getId(), transaction.getAmount(), transaction.getCurrency());

        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        if (transaction.getStatus() != PaymentStatus.CREATED) {
            throw new IllegalStateException(
                    String.format("Transaction must be in CREATED state, but is in %s", transaction.getStatus())
            );
        }

        try {
            transactionVersionService.updateTransactionState(transaction.getId(), PaymentStatus.VALIDATED, "system");

            boolean isHighRisk = riskEvaluationService.isHighRisk(transaction);

            if (isHighRisk) {
                logger.info("High-risk transaction detected: transaction_id={}, transitioning to CHALLENGE_PENDING",
                        transaction.getId());
                transactionVersionService.updateTransactionState(transaction.getId(), PaymentStatus.CHALLENGE_PENDING, "system");
                return;
            }

            // Low-risk: process directly with the acquirer
            // FIX: Transition to PROCESSING before calling the acquirer
            logger.info("Transaction {} is low risk. Transitioning to PROCESSING.", transaction.getId());
            transactionVersionService.updateTransactionState(transaction.getId(), PaymentStatus.PROCESSING, "system");

            PaymentResult result = paymentAcquirerClient.submitPayment(transaction);

            logger.info("Payment result received: transaction_id={}, status={}, acquirer_ref={}",
                    transaction.getId(), result.getStatus(), result.getAcquirerReference());

            handleAcquirerResponse(transaction, result);
        } catch (PaymentTimeoutException e) {
            logger.warn("Payment timeout, transitioning to UNKNOWN: transaction_id={}, error={}",
                    transaction.getId(), e.getMessage());
            // THE FIX: Reload the transaction from the database to get its current state.
            Transaction freshTransaction = transactionRepository.findById(transaction.getId())
                    .orElseThrow(() -> new IllegalStateException("Transaction not found after timeout: " + transaction.getId()));

            // Pass the fresh, up-to-date object to the handler.
            unknownStateTransitionHandler.transitionToUnknownDueToTimeout(freshTransaction, e.getMessage());

        } catch (PaymentAcquirerException e) {
            logger.warn("Acquirer error, transitioning to UNKNOWN: transaction_id={}, error={}",
                    transaction.getId(), e.getMessage());
            // Also apply the fix here for consistency.
            Transaction freshTransaction = transactionRepository.findById(transaction.getId())
                    .orElseThrow(() -> new IllegalStateException("Transaction not found after acquirer error: " + transaction.getId()));
            unknownStateTransitionHandler.transitionToUnknownDueToAcquirerError(freshTransaction, e.getMessage());

        } catch (Exception e) {
            logger.error("Error processing payment: transaction_id={}, error={}", transaction.getId(), e.getMessage(), e);
            throw e;
        }
    }

    private void handleAcquirerResponse(Transaction transaction, PaymentResult result) {
        transactionVersionService.updateTransactionStateAndAcquirerRef(
                transaction.getId(),
                result.getStatus(),
                "system",
                result.getAcquirerReference()
        );
    }
}

