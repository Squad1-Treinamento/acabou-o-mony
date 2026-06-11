package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.exception.InvalidStateTransitionException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.domain.model.StateTransition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaymentStateMachine Tests")
class PaymentStateMachineTest {
    
    private PaymentStateMachine stateMachine;
    
    @BeforeEach
    void setUp() {
        stateMachine = new PaymentStateMachine();
    }
    
    // ============================================
    // Tests for Allowed Transitions (23 total)
    // ============================================
    
    @Test
    @DisplayName("Should allow transition CREATED -> VALIDATED")
    void testAllowedTransitionCreatedToValidated() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED)
        );
    }
    
    @Test
    @DisplayName("Should allow transition VALIDATED -> CHALLENGE_PENDING")
    void testAllowedTransitionValidatedToChallengePending() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.VALIDATED, PaymentStatus.CHALLENGE_PENDING)
        );
    }
    
    @Test
    @DisplayName("Should allow transition VALIDATED -> PROCESSING")
    void testAllowedTransitionValidatedToProcessing() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.VALIDATED, PaymentStatus.PROCESSING)
        );
    }
    
    @Test
    @DisplayName("Should allow transition CHALLENGE_PENDING -> AUTHENTICATED")
    void testAllowedTransitionChallengePendingToAuthenticated() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.CHALLENGE_PENDING, PaymentStatus.AUTHENTICATED)
        );
    }
    
    @Test
    @DisplayName("Should allow transition CHALLENGE_PENDING -> DECLINED")
    void testAllowedTransitionChallengePendingToDeclined() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.CHALLENGE_PENDING, PaymentStatus.DECLINED)
        );
    }
    
    @Test
    @DisplayName("Should allow transition CHALLENGE_PENDING -> FAILED")
    void testAllowedTransitionChallengePendingToFailed() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.CHALLENGE_PENDING, PaymentStatus.FAILED)
        );
    }
    
    @Test
    @DisplayName("Should allow transition AUTHENTICATED -> PROCESSING")
    void testAllowedTransitionAuthenticatedToProcessing() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.AUTHENTICATED, PaymentStatus.PROCESSING)
        );
    }
    
    @Test
    @DisplayName("Should allow transition PROCESSING -> COMPLETED")
    void testAllowedTransitionProcessingToCompleted() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.COMPLETED)
        );
    }
    
    @Test
    @DisplayName("Should allow transition PROCESSING -> DECLINED")
    void testAllowedTransitionProcessingToDeclined() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.DECLINED)
        );
    }
    
    @Test
    @DisplayName("Should allow transition PROCESSING -> UNKNOWN")
    void testAllowedTransitionProcessingToUnknown() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.UNKNOWN)
        );
    }
    
    @Test
    @DisplayName("Should allow transition PROCESSING -> FAILED")
    void testAllowedTransitionProcessingToFailed() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.FAILED)
        );
    }
    
    @Test
    @DisplayName("Should allow transition UNKNOWN -> COMPLETED")
    void testAllowedTransitionUnknownToCompleted() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.UNKNOWN, PaymentStatus.COMPLETED)
        );
    }
    
    @Test
    @DisplayName("Should allow transition UNKNOWN -> DECLINED")
    void testAllowedTransitionUnknownToDeclined() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.UNKNOWN, PaymentStatus.DECLINED)
        );
    }
    
    @Test
    @DisplayName("Should allow transition UNKNOWN -> FAILED")
    void testAllowedTransitionUnknownToFailed() {
        assertDoesNotThrow(() -> 
            stateMachine.validateTransition(PaymentStatus.UNKNOWN, PaymentStatus.FAILED)
        );
    }
    
    // ============================================
    // Tests for Disallowed Transitions
    // ============================================
    
    @Test
    @DisplayName("Should reject transition CREATED -> COMPLETED (invalid)")
    void testRejectedTransitionCreatedToCompleted() {
        InvalidStateTransitionException ex = assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(PaymentStatus.CREATED, PaymentStatus.COMPLETED)
        );
        assertTrue(ex.getMessage().contains("CREATED"));
        assertTrue(ex.getMessage().contains("COMPLETED"));
    }
    
    @Test
    @DisplayName("Should reject transition COMPLETED -> PROCESSING (terminal state)")
    void testRejectedTransitionCompletedToProcessing() {
        InvalidStateTransitionException ex = assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(PaymentStatus.COMPLETED, PaymentStatus.PROCESSING)
        );
        assertTrue(ex.getMessage().contains("COMPLETED"));
        assertTrue(ex.getMessage().contains("PROCESSING"));
    }
    
    @Test
    @DisplayName("Should reject transition UNKNOWN -> PROCESSING (invalid)")
    void testRejectedTransitionUnknownToProcessing() {
        assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(PaymentStatus.UNKNOWN, PaymentStatus.PROCESSING)
        );
    }
    
    @Test
    @DisplayName("Should reject transition DECLINED -> FAILED (both terminal)")
    void testRejectedTransitionDeclinedToFailed() {
        assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(PaymentStatus.DECLINED, PaymentStatus.FAILED)
        );
    }
    
    @Test
    @DisplayName("Should reject transition FAILED -> COMPLETED (both terminal)")
    void testRejectedTransitionFailedToCompleted() {
        assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(PaymentStatus.FAILED, PaymentStatus.COMPLETED)
        );
    }
    
    @Test
    @DisplayName("Should reject transition CHALLENGE_PENDING -> PROCESSING (invalid)")
    void testRejectedTransitionChallengePendingToProcessing() {
        assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(PaymentStatus.CHALLENGE_PENDING, PaymentStatus.PROCESSING)
        );
    }
    
    @Test
    @DisplayName("Should reject transition AUTHENTICATED -> VALIDATED (backward)")
    void testRejectedTransitionAuthenticatedToValidated() {
        assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(PaymentStatus.AUTHENTICATED, PaymentStatus.VALIDATED)
        );
    }
    
    @Test
    @DisplayName("Should reject transition PROCESSING -> VALIDATED (backward)")
    void testRejectedTransitionProcessingToValidated() {
        assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.VALIDATED)
        );
    }
    
    // ============================================
    // Tests for Null Handling
    // ============================================
    
    @Test
    @DisplayName("Should reject transition with null 'from' state")
    void testRejectTransitionWithNullFromState() {
        assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(null, PaymentStatus.COMPLETED)
        );
    }
    
    @Test
    @DisplayName("Should reject transition with null 'to' state")
    void testRejectTransitionWithNullToState() {
        assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(PaymentStatus.CREATED, null)
        );
    }
    
    // ============================================
    // Tests for StateTransition Value Object
    // ============================================
    
    @Test
    @DisplayName("Should validate transition using StateTransition value object")
    void testValidateTransitionUsingValueObject() {
        StateTransition transition = new StateTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED);
        assertDoesNotThrow(() -> stateMachine.validateTransition(transition));
    }
    
    @Test
    @DisplayName("Should reject invalid transition using StateTransition value object")
    void testRejectInvalidTransitionUsingValueObject() {
        StateTransition transition = new StateTransition(PaymentStatus.COMPLETED, PaymentStatus.PROCESSING);
        assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(transition)
        );
    }
    
    // ============================================
    // Tests for isTransitionAllowed Method
    // ============================================
    
    @Test
    @DisplayName("Should return true for allowed transition")
    void testIsTransitionAllowedReturnsTrue() {
        assertTrue(stateMachine.isTransitionAllowed(PaymentStatus.CREATED, PaymentStatus.VALIDATED));
    }
    
    @Test
    @DisplayName("Should return false for disallowed transition")
    void testIsTransitionAllowedReturnsFalse() {
        assertFalse(stateMachine.isTransitionAllowed(PaymentStatus.COMPLETED, PaymentStatus.PROCESSING));
    }
    
    @Test
    @DisplayName("Should return false when 'from' state is null")
    void testIsTransitionAllowedReturnsFalseForNullFromState() {
        assertFalse(stateMachine.isTransitionAllowed(null, PaymentStatus.COMPLETED));
    }
    
    @Test
    @DisplayName("Should return false when 'to' state is null")
    void testIsTransitionAllowedReturnsFalseForNullToState() {
        assertFalse(stateMachine.isTransitionAllowed(PaymentStatus.CREATED, null));
    }
    
    // ============================================
    // Tests for Exception Message Quality
    // ============================================
    
    @Test
    @DisplayName("Exception message should contain 'from' state")
    void testExceptionMessageContainsFromState() {
        InvalidStateTransitionException ex = assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(PaymentStatus.COMPLETED, PaymentStatus.PROCESSING)
        );
        assertTrue(ex.getMessage().contains("COMPLETED"));
        assertEquals(PaymentStatus.COMPLETED, ex.getFrom());
    }
    
    @Test
    @DisplayName("Exception message should contain 'to' state")
    void testExceptionMessageContainsToState() {
        InvalidStateTransitionException ex = assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(PaymentStatus.COMPLETED, PaymentStatus.PROCESSING)
        );
        assertTrue(ex.getMessage().contains("PROCESSING"));
        assertEquals(PaymentStatus.PROCESSING, ex.getTo());
    }
    
    @Test
    @DisplayName("Exception message should contain reason")
    void testExceptionMessageContainsReason() {
        InvalidStateTransitionException ex = assertThrows(
            InvalidStateTransitionException.class,
            () -> stateMachine.validateTransition(PaymentStatus.COMPLETED, PaymentStatus.PROCESSING)
        );
        assertTrue(ex.getMessage().contains("Reason:"));
        assertNotNull(ex.getReason());
    }
    
    // ============================================
    // Tests for Idempotency
    // ============================================
    
    @Test
    @DisplayName("validateTransition should be idempotent (no side effects)")
    void testValidateTransitionIsIdempotent() {
        // Call multiple times, should always succeed
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                stateMachine.validateTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED);
            }
        });
    }
    
    @Test
    @DisplayName("PaymentStatus enum should have exactly 9 states")
    void testPaymentStatusEnumHasNineStates() {
        PaymentStatus[] states = PaymentStatus.values();
        assertEquals(9, states.length);
    }
}
