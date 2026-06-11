package com.acabouomony.payment.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PaymentStatus enum.
 * 
 * Verifies:
 * - All 9 valid statuses defined
 * - Enum values match spec requirements
 * - Serialization/deserialization works correctly
 */
class PaymentStatusEnumTest {

    @Test
    void shouldDefineAllNinePaymentStatuses() {
        // Arrange & Act
        PaymentStatus[] statuses = PaymentStatus.values();

        // Assert
        assertThat(statuses).hasSize(9);
    }

    @Test
    void shouldDefineCreatedStatus() {
        assertThat(PaymentStatus.CREATED).isNotNull();
    }

    @Test
    void shouldDefineValidatedStatus() {
        assertThat(PaymentStatus.VALIDATED).isNotNull();
    }

    @Test
    void shouldDefineChallengePendingStatus() {
        assertThat(PaymentStatus.CHALLENGE_PENDING).isNotNull();
    }

    @Test
    void shouldDefineAuthenticatedStatus() {
        assertThat(PaymentStatus.AUTHENTICATED).isNotNull();
    }

    @Test
    void shouldDefineProcessingStatus() {
        assertThat(PaymentStatus.PROCESSING).isNotNull();
    }

    @Test
    void shouldDefineUnknownStatus() {
        assertThat(PaymentStatus.UNKNOWN).isNotNull();
    }

    @Test
    void shouldDefineCompletedStatus() {
        assertThat(PaymentStatus.COMPLETED).isNotNull();
    }

    @Test
    void shouldDefineDeclinedStatus() {
        assertThat(PaymentStatus.DECLINED).isNotNull();
    }

    @Test
    void shouldDefineFailedStatus() {
        assertThat(PaymentStatus.FAILED).isNotNull();
    }

    @Test
    void shouldSerializeToString() {
        // Arrange
        PaymentStatus status = PaymentStatus.CREATED;

        // Act
        String serialized = status.toString();

        // Assert
        assertThat(serialized).isEqualTo("CREATED");
    }

    @Test
    void shouldDeserializeFromString() {
        // Arrange
        String statusString = "VALIDATED";

        // Act
        PaymentStatus deserialized = PaymentStatus.valueOf(statusString);

        // Assert
        assertThat(deserialized).isEqualTo(PaymentStatus.VALIDATED);
    }

    @Test
    void shouldSupportEnumComparison() {
        // Arrange
        PaymentStatus status1 = PaymentStatus.PROCESSING;
        PaymentStatus status2 = PaymentStatus.PROCESSING;
        PaymentStatus status3 = PaymentStatus.COMPLETED;

        // Act & Assert
        assertThat(status1).isEqualTo(status2);
        assertThat(status1).isNotEqualTo(status3);
    }

    @Test
    void shouldSupportEnumInSwitch() {
        // Arrange
        PaymentStatus status = PaymentStatus.COMPLETED;
        String result = "";

        // Act
        switch (status) {
            case CREATED -> result = "initial";
            case VALIDATED -> result = "validated";
            case CHALLENGE_PENDING -> result = "challenge";
            case AUTHENTICATED -> result = "authenticated";
            case PROCESSING -> result = "processing";
            case UNKNOWN -> result = "unknown";
            case COMPLETED -> result = "completed";
            case DECLINED -> result = "declined";
            case FAILED -> result = "failed";
        }

        // Assert
        assertThat(result).isEqualTo("completed");
    }

    @Test
    void shouldSupportEnumOrdinal() {
        // Verify that enum ordinals are consistent
        PaymentStatus[] statuses = PaymentStatus.values();
        
        assertThat(statuses[0]).isEqualTo(PaymentStatus.CREATED);
        assertThat(statuses[1]).isEqualTo(PaymentStatus.VALIDATED);
        assertThat(statuses[2]).isEqualTo(PaymentStatus.CHALLENGE_PENDING);
        assertThat(statuses[3]).isEqualTo(PaymentStatus.AUTHENTICATED);
        assertThat(statuses[4]).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(statuses[5]).isEqualTo(PaymentStatus.UNKNOWN);
        assertThat(statuses[6]).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(statuses[7]).isEqualTo(PaymentStatus.DECLINED);
        assertThat(statuses[8]).isEqualTo(PaymentStatus.FAILED);
    }
}
