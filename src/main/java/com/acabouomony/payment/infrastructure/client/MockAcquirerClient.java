package com.acabouomony.payment.infrastructure.client;

import com.acabouomony.payment.domain.dto.PaymentResult;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.PaymentTimeoutException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.PaymentAcquirerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Profile("mock-acquirer") // Only active when the "mock-acquirer" profile is enabled
public class MockAcquirerClient implements PaymentAcquirerClient {

    private static final Logger logger = LoggerFactory.getLogger(MockAcquirerClient.class);

    @Override
    public PaymentResult submitPayment(Transaction transaction) {
        logger.info("MOCK ACQUIRER: Simulating payment submission for transactionId: {}", transaction.getId());

        // Simulate network latency
        try {
            Thread.sleep(200); // 200ms delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // *** This is where we simulate different acquirer behaviors ***
        // See Phase 2 for the implementation of this logic.
        return simulateAcquirerResponse(transaction);
    }

    @Override
    public PaymentStatus queryPaymentStatus(String acquirerReference) {
        logger.info("MOCK ACQUIRER: Simulating status query for acquirerReference: {}", acquirerReference);
        // In a real mock, you might have more complex logic here.
        // For now, we'll assume reconciliation always succeeds.
        return PaymentStatus.COMPLETED;
    }

    private PaymentResult simulateAcquirerResponse(Transaction transaction) {
        long amount = transaction.getAmount();
        String mockReference = "mock_ref_" + UUID.randomUUID();

        // Rule-based simulation based on the transaction amount
        if (amount % 100 == 99) {
            // Amounts ending in .99 will simulate a timeout
            logger.warn("MOCK ACQUIRER: Simulating a timeout for amount: {}", amount);
            throw new PaymentTimeoutException("Mock acquirer timed out");
        }

        if (amount % 100 == 1) {
            // Amounts ending in .01 will be declined
            logger.info("MOCK ACQUIRER: Simulating a decline for amount: {}", amount);
        return new PaymentResult(
                mockReference,
                PaymentStatus.DECLINED,
                "Mock payment declined by issuer",
            Instant.now()
        );
    }

        // All other amounts will be approved
        logger.info("MOCK ACQUIRER: Simulating an approval for amount: {}", amount);
        return new PaymentResult(
            mockReference,
            PaymentStatus.COMPLETED,
            "Mock payment approved",
            Instant.now()
        );
}
}
