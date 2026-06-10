package com.acabouomony.payment.domain.model;

/**
 * Enumeration of payment lifecycle states.
 * 
 * States represent the journey of a payment through the system:
 * - CREATED: Initial state when payment request received
 * - VALIDATED: Request validated, passed security checks
 * - CHALLENGE_PENDING: Awaiting 3DS authentication (high-risk transactions)
 * - AUTHENTICATED: 3DS challenge completed successfully
 * - AUTHORIZED: Acquirer has approved the payment and reserved funds.
 * - PROCESSING: Submitted to acquirer (Mercado Pago), awaiting response
 * - UNKNOWN: Timeout or unexpected error during acquirer communication
 * - COMPLETED: Payment successful and funds captured.
 * - DECLINED: Payment rejected by acquirer
 * - FAILED: Payment failed due to system error or timeout
 * - VOIDED: The authorization was cancelled before capture.
 * - REFUNDED: The captured funds were returned to the cardholder.
 */
public enum PaymentStatus {
    CREATED,
    VALIDATED,
    CHALLENGE_PENDING,
    AUTHENTICATED,
    AUTHORIZED,
    PROCESSING,
    UNKNOWN,
    COMPLETED,
    DECLINED,
    FAILED,
    VOIDED,
    REFUNDED
}
