package com.acabouomony.payment.infrastructure.client;

import com.acabouomony.payment.domain.dto.PaymentResult;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.exception.PaymentAcquirerException;
import com.acabouomony.payment.domain.exception.PaymentTimeoutException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.service.PaymentAcquirerClient;
import com.acabouomony.payment.infrastructure.client.dto.MercadoPagoRequest;
import com.acabouomony.payment.infrastructure.client.dto.MercadoPagoResponse;
import com.acabouomony.payment.infrastructure.client.exception.MercadoPagoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Mercado Pago payment acquirer client implementation.
 * 
 * Submits payments to Mercado Pago and queries payment status.
 * Implements PaymentAcquirerClient interface.
 * 
 * Spec: spec-001-core-payment-processing.md - Mercado Pago Integration
 * Task: task-011-mercado-pago-client.md, task-013-unknown-state-handling.md
 * 
 * Timeout Configuration:
 * - Connect timeout: 500ms
 * - Read timeout: 2000ms
 * - Total timeout: 2500ms
 * 
 * Retry Logic:
 * - Max retries: 3
 * - Backoff: 100ms, 200ms, 400ms
 * - Retryable: timeout, 5xx errors
 * - Non-retryable: 4xx errors (validation/auth)
 * 
 * Exception Handling:
 * - SocketTimeoutException -> PaymentTimeoutException (triggers UNKNOWN)
 * - ConnectException -> PaymentAcquirerException (triggers UNKNOWN)
 * - HttpServerErrorException (5xx) -> PaymentAcquirerException (triggers UNKNOWN)
 * - HttpClientErrorException (4xx) -> MercadoPagoException (triggers FAILED)
 */
@Component
@Profile("!mock-acquirer")
public class MercadoPagoClient implements PaymentAcquirerClient {
    
    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoClient.class);
    
    private static final int CONNECT_TIMEOUT_MS = 500;
    private static final int READ_TIMEOUT_MS = 2000;
    private static final int MAX_RETRIES = 3;
    
    private final RestTemplate restTemplate;
    
    @Value("${mercado-pago.api-url:https://api.mercadopago.com}")
    private String apiUrl;
    
    @Value("${mercado-pago.access-token:}")
    private String accessToken;
    
    @Value("${mercado-pago.webhook-url:}")
    private String webhookUrl;
    
    public MercadoPagoClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Submits a payment to Mercado Pago.
     * 
     * Maps transaction to Mercado Pago request format, submits, and returns result.
     * 
     * Timeout behavior:
     * - If timeout occurs: throws PaymentTimeoutException
     * - Caller is responsible for catching and scheduling reconciliation
     * 
     * @param transaction The transaction to process
     * @return PaymentResult with acquirer reference and status
     * @throws PaymentTimeoutException on timeout
     * @throws PaymentAcquirerException on acquirer error
     */
    @Override
    public PaymentResult submitPayment(Transaction transaction) {
        logger.info("Submitting payment to Mercado Pago: transaction_id={}, amount={}, currency={}",
            transaction.getId(), transaction.getAmount(), transaction.getCurrency());
        
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        // Map transaction to Mercado Pago request
        MercadoPagoRequest request = mapTransactionToRequest(transaction);
        
        // Submit with retry logic
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                MercadoPagoResponse response = submitPaymentWithRetry(request);
                
                logger.info("Payment submitted successfully: transaction_id={}, mercado_pago_id={}, status={}",
                    transaction.getId(), response.getId(), response.getStatus());
                
                // Map response to PaymentResult
                return mapResponseToResult(response);
                
            } catch (PaymentTimeoutException e) {
                // Timeout is retryable
                if (attempt == MAX_RETRIES - 1) {
                    logger.error("Payment submission timeout (max retries exhausted): transaction_id={}, error={}",
                        transaction.getId(), e.getMessage());
                    throw e;  // Propagate to caller
                }
                
                // Retryable error: wait and retry
                long backoffMs = 100L * (attempt + 1);
                logger.warn("Payment submission timeout (retryable), retrying after {}ms: " +
                        "transaction_id={}, attempt={}/{}",
                    backoffMs, transaction.getId(), attempt + 1, MAX_RETRIES);
                
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted during retry backoff: transaction_id={}", transaction.getId());
                    throw new PaymentTimeoutException("Payment submission interrupted", ie);
                }
                
            } catch (PaymentAcquirerException e) {
                // Acquirer error is retryable
                if (attempt == MAX_RETRIES - 1) {
                    logger.error("Payment submission acquirer error (max retries exhausted): transaction_id={}, error={}",
                        transaction.getId(), e.getMessage());
                    throw e;  // Propagate to caller
                }
                
                // Retryable error: wait and retry
                long backoffMs = 100L * (attempt + 1);
                logger.warn("Payment submission acquirer error (retryable), retrying after {}ms: " +
                        "transaction_id={}, attempt={}/{}",
                    backoffMs, transaction.getId(), attempt + 1, MAX_RETRIES);
                
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted during retry backoff: transaction_id={}", transaction.getId());
                    throw new PaymentAcquirerException("Payment submission interrupted", ie);
                }
                
            } catch (MercadoPagoException e) {
                // Non-retryable error (4xx)
                logger.error("Payment submission failed (non-retryable): transaction_id={}, error={}",
                    transaction.getId(), e.getMessage());
                throw e;  // Propagate to caller
            }
        }
        
        // Should not reach here
        logger.error("Payment submission exhausted all retries: transaction_id={}", transaction.getId());
        throw new PaymentAcquirerException("Payment submission exhausted all retries");
    }
    
    /**
     * Queries payment status at Mercado Pago.
     * 
     * Used during reconciliation to resolve UNKNOWN state transactions.
     * 
     * @param acquirerReference Mercado Pago payment ID
     * @return PaymentStatus from Mercado Pago
     */
    @Override
    public PaymentStatus queryPaymentStatus(String acquirerReference) {
        logger.debug("Querying payment status from Mercado Pago: mercado_pago_id={}", acquirerReference);
        
        if (acquirerReference == null || acquirerReference.isEmpty()) {
            throw new IllegalArgumentException("Acquirer reference cannot be null or empty");
        }
        
        try {
            String url = apiUrl + "/v1/payments/" + acquirerReference;
            
            // TODO: Implement actual HTTP GET request with authorization
            // For now, return UNKNOWN to indicate not implemented
            logger.warn("Status query not yet implemented: mercado_pago_id={}", acquirerReference);
            return PaymentStatus.UNKNOWN;
            
        } catch (Exception e) {
            logger.error("Error querying payment status: mercado_pago_id={}, error={}", acquirerReference, e.getMessage());
            return PaymentStatus.UNKNOWN;
        }
    }
    
    /**
     * Submits payment request to Mercado Pago with timeout handling.
     * 
     * Throws PaymentTimeoutException on timeout.
     * Throws PaymentAcquirerException on network/server error.
     * Throws MercadoPagoException on client error (4xx).
     * 
     * @param request Mercado Pago request
     * @return Mercado Pago response
     * @throws PaymentTimeoutException on timeout
     * @throws PaymentAcquirerException on acquirer error
     * @throws MercadoPagoException on client error
     */
    private MercadoPagoResponse submitPaymentWithRetry(MercadoPagoRequest request) {
        // TODO: Implement actual HTTP POST request with authorization and timeout
        // When implemented, this method should:
        // 1. Make HTTP POST to apiUrl + "/v1/payments"
        // 2. Set connect timeout: CONNECT_TIMEOUT_MS (500ms)
        // 3. Set read timeout: READ_TIMEOUT_MS (2000ms)
        // 4. Include Authorization header with accessToken
        // 5. Catch and convert exceptions:
        //    - SocketTimeoutException -> PaymentTimeoutException
        //    - ConnectException -> PaymentAcquirerException
        //    - ResourceAccessException -> PaymentTimeoutException or PaymentAcquirerException
        //    - HttpServerErrorException (5xx) -> PaymentAcquirerException
        //    - HttpClientErrorException (4xx) -> MercadoPagoException
        
        // For now, throw exception to indicate not implemented
        throw new MercadoPagoException(0, "NOT_IMPLEMENTED", "Mercado Pago client not yet implemented", false);
    }
    
    /**
     * Maps transaction to Mercado Pago request format.
     * 
     * @param transaction The transaction
     * @return Mercado Pago request
     */
    private MercadoPagoRequest mapTransactionToRequest(Transaction transaction) {
        return MercadoPagoRequest.builder()
            .amount(transaction.getAmount())
            .currencyId(transaction.getCurrency())
            .description("Payment for transaction " + transaction.getId())
            .paymentMethodId("credit_card")
            .token(transaction.getCardTokenId())
            .externalReference(transaction.getId().toString())
            .notificationUrl(webhookUrl)
            .payer(MercadoPagoRequest.PayerInfo.builder()
                .email("customer@example.com")  // TODO: Get from customer info
                .firstName("Customer")  // TODO: Get from customer info
                .lastName("Name")  // TODO: Get from customer info
                .build())
            .build();
    }
    
    /**
     * Maps Mercado Pago response to PaymentResult.
     * 
     * @param response Mercado Pago response
     * @return PaymentResult
     */
    private PaymentResult mapResponseToResult(MercadoPagoResponse response) {
        PaymentStatus status = mapMercadoPagoStatus(response.getStatus());
        
        return PaymentResult.builder()
            .acquirerReference(response.getId())
            .status(status)
            .message(response.getStatusDetail())
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Maps Mercado Pago status to PaymentStatus.
     * 
     * @param mercadoPagoStatus Status from Mercado Pago
     * @return PaymentStatus
     */
    private PaymentStatus mapMercadoPagoStatus(String mercadoPagoStatus) {
        if (mercadoPagoStatus == null) {
            return PaymentStatus.UNKNOWN;
        }
        
        return switch (mercadoPagoStatus) {
            case "approved" -> PaymentStatus.COMPLETED;
            case "rejected" -> PaymentStatus.DECLINED;
            case "pending" -> PaymentStatus.PROCESSING;
            case "cancelled" -> PaymentStatus.DECLINED;
            case "refunded" -> PaymentStatus.DECLINED;
            default -> PaymentStatus.UNKNOWN;
        };
    }
}
