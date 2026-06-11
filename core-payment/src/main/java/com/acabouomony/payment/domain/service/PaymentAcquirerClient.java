package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.dto.PaymentResult;
import com.acabouomony.payment.domain.entity.Transaction;
import com.acabouomony.payment.domain.model.PaymentStatus;

/**
 * Interface for payment acquirer integration.
 * 
 * Abstracts payment processing to allow multiple acquirer implementations.
 * Currently implemented by MercadoPagoClient.
 * 
 * Spec: spec-001-core-payment-processing.md - Mercado Pago Integration
 * Task: task-011-mercado-pago-client.md
 * 
 * This interface isolates acquirer-specific details behind a stable contract,
 * allowing Phase 4 reconciliation to use the same interface for status queries.
 */
public interface PaymentAcquirerClient {
    
    /**
     * Submits a payment request to the acquirer.
     * 
     * Maps transaction to acquirer wire format, submits request, and returns result.
     * 
     * Timeout behavior:
     * - If acquirer doesn't respond within timeout: returns PaymentStatus.UNKNOWN
     * - Caller is responsible for scheduling reconciliation
     * 
     * @param transaction The transaction to process
     * @return PaymentResult containing acquirer reference and status
     * @throws IllegalArgumentException if transaction is invalid
     */
    PaymentResult submitPayment(Transaction transaction);
    
    /**
     * Queries the status of a payment at the acquirer.
     * 
     * Used during reconciliation to resolve UNKNOWN state transactions.
     * 
     * @param acquirerReference The acquirer's payment ID (e.g., Mercado Pago payment ID)
     * @return PaymentStatus from acquirer
     * @throws IllegalArgumentException if acquirerReference is invalid
     */
    PaymentStatus queryPaymentStatus(String acquirerReference);
}
