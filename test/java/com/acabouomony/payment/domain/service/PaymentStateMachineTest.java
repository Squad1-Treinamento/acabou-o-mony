package com.acabouomony.payment.domain.service;

import com.acabouomony.payment.domain.exception.InvalidStateTransitionException;
import com.acabouomony.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

/**
 * State machine tests for PaymentStatus transitions.
 * Validates 23 allowed transitions as defined in implementation and ensures
 * all other transitions throw InvalidStateTransitionException with proper message.
 */
public class PaymentStateMachineTest {

    private final PaymentStateMachine stateMachine = new PaymentStateMachine();

    @Test
    @DisplayName("PaymentStatus enum has exactly 9 values")
    void testEnumHasNineStates() {
        assertThat(PaymentStatus.values()).hasSize(9);
    }

    @Test
    @DisplayName("All 23 allowed transitions pass validateTransition")
    void testAllAllowedTransitions() {
        // Enumerate all 23 allowed transitions as per implemented map
        // 1) CREATED -> VALIDATED
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED));
        // 2-5: VALIDATED -> CHALLENGE_PENDING, PROCESSING, UNKNOWN, AUTHENTICATED
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.VALIDATED, PaymentStatus.CHALLENGE_PENDING));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.VALIDATED, PaymentStatus.PROCESSING));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.VALIDATED, PaymentStatus.UNKNOWN));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.VALIDATED, PaymentStatus.AUTHENTICATED));
        // CHALLENGE_PENDING -> AUTHENTICATED, PROCESSING, DECLINED, FAILED, UNKNOWN
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.CHALLENGE_PENDING, PaymentStatus.AUTHENTICATED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.CHALLENGE_PENDING, PaymentStatus.PROCESSING));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.CHALLENGE_PENDING, PaymentStatus.DECLINED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.CHALLENGE_PENDING, PaymentStatus.FAILED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.CHALLENGE_PENDING, PaymentStatus.UNKNOWN));
        // AUTHENTICATED -> PROCESSING, DECLINED, COMPLETED
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.AUTHENTICATED, PaymentStatus.PROCESSING));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.AUTHENTICATED, PaymentStatus.DECLINED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.AUTHENTICATED, PaymentStatus.COMPLETED));
        // PROCESSING -> COMPLETED, DECLINED, UNKNOWN, FAILED, AUTHENTICATED
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.COMPLETED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.DECLINED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.UNKNOWN));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.FAILED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.AUTHENTICATED));
        // UNKNOWN -> COMPLETED, DECLINED, FAILED, PROCESSING, AUTHENTICATED
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.UNKNOWN, PaymentStatus.COMPLETED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.UNKNOWN, PaymentStatus.DECLINED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.UNKNOWN, PaymentStatus.FAILED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.UNKNOWN, PaymentStatus.PROCESSING));
        assertDoesNotThrow(() -> stateMachine.validateTransition(PaymentStatus.UNKNOWN, PaymentStatus.AUTHENTICATED));
    }

    @Test
    @DisplayName("All other transitions throw InvalidStateTransitionException with proper message")
    void testAllOtherTransitionsAreInvalid() {
        // Build full matrix and verify non-allowed transitions throw
        for (PaymentStatus from : PaymentStatus.values()) {
            for (PaymentStatus to : PaymentStatus.values()) {
                boolean isAllowed = isExplicitlyAllowed(from, to);
                if (!isAllowed) {
                    Executable exec = () -> stateMachine.validateTransition(from, to);
                    InvalidStateTransitionException ex = catchThrowableOfType(exec.execute(), InvalidStateTransitionException.class);
                    assertThat(ex).isNotNull();
                    assertThat(ex.getFrom()).isEqualTo(from);
                    assertThat(ex.getTo()).isEqualTo(to);
                    assertThat(ex.getMessage()).contains("Invalid transition");
                }
            }
        }
    }

    // Helper to mirror allowed transitions set in implementation
    private boolean isExplicitlyAllowed(PaymentStatus from, PaymentStatus to) {
        switch (from) {
            case CREATED:
                return to == PaymentStatus.VALIDATED;
            case VALIDATED:
                return to == PaymentStatus.CHALLENGE_PENDING
                    || to == PaymentStatus.PROCESSING
                    || to == PaymentStatus.UNKNOWN
                    || to == PaymentStatus.AUTHENTICATED;
            case CHALLENGE_PENDING:
                return to == PaymentStatus.AUTHENTICATED
                    || to == PaymentStatus.PROCESSING
                    || to == PaymentStatus.DECLINED
                    || to == PaymentStatus.FAILED
                    || to == PaymentStatus.UNKNOWN;
            case AUTHENTICATED:
                return to == PaymentStatus.PROCESSING
                    || to == PaymentStatus.DECLINED
                    || to == PaymentStatus.COMPLETED;
            case PROCESSING:
                return to == PaymentStatus.COMPLETED
                    || to == PaymentStatus.DECLINED
                    || to == PaymentStatus.UNKNOWN
                    || to == PaymentStatus.FAILED
                    || to == PaymentStatus.AUTHENTICATED;
            case UNKNOWN:
                return to == PaymentStatus.COMPLETED
                    || to == PaymentStatus.DECLINED
                    || to == PaymentStatus.FAILED
                    || to == PaymentStatus.PROCESSING
                    || to == PaymentStatus.AUTHENTICATED;
            case COMPLETED:
            case DECLINED:
            case FAILED:
                // Terminal states
                return false;
            default:
                return false;
        }
    }
}
