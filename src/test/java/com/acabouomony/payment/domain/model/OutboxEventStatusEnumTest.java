package com.acabouomony.payment.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OutboxEventStatus enum.
 * 
 * Verifies:
 * - All 3 valid statuses defined
 * - Enum values match spec requirements
 * - Serialization/deserialization works correctly
 */
class OutboxEventStatusEnumTest {

    @Test
    void shouldDefineThreeOutboxEventStatuses() {
        // Arrange & Act
        OutboxEventStatus[] statuses = OutboxEventStatus.values();

        // Assert
        assertThat(statuses).hasLength(3);
    }

    @Test
    void shouldDefinePendingStatus() {
        assertThat(OutboxEventStatus.PENDING).isNotNull();
    }

    @Test
    void shouldDefineDeliveredStatus() {
        assertThat(OutboxEventStatus.DELIVERED).isNotNull();
    }

    @Test
    void shouldDefineFailedStatus() {
        assertThat(OutboxEventStatus.FAILED).isNotNull();
    }

    @Test
    void shouldSerializeToString() {
        // Arrange
        OutboxEventStatus status = OutboxEventStatus.PENDING;

        // Act
        String serialized = status.toString();

        // Assert
        assertThat(serialized).isEqualTo("PENDING");
    }

    @Test
    void shouldDeserializeFromString() {
        // Arrange
        String statusString = "DELIVERED";

        // Act
        OutboxEventStatus deserialized = OutboxEventStatus.valueOf(statusString);

        // Assert
        assertThat(deserialized).isEqualTo(OutboxEventStatus.DELIVERED);
    }

    @Test
    void shouldSupportEnumComparison() {
        // Arrange
        OutboxEventStatus status1 = OutboxEventStatus.PENDING;
        OutboxEventStatus status2 = OutboxEventStatus.PENDING;
        OutboxEventStatus status3 = OutboxEventStatus.DELIVERED;

        // Act & Assert
        assertThat(status1).isEqualTo(status2);
        assertThat(status1).isNotEqualTo(status3);
    }

    @Test
    void shouldSupportEnumInSwitch() {
        // Arrange
        OutboxEventStatus status = OutboxEventStatus.DELIVERED;
        String result = "";

        // Act
        switch (status) {
            case PENDING -> result = "pending";
            case DELIVERED -> result = "delivered";
            case FAILED -> result = "failed";
        }

        // Assert
        assertThat(result).isEqualTo("delivered");
    }

    @Test
    void shouldSupportEnumOrdinal() {
        // Verify that enum ordinals are consistent
        OutboxEventStatus[] statuses = OutboxEventStatus.values();
        
        assertThat(statuses[0]).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(statuses[1]).isEqualTo(OutboxEventStatus.DELIVERED);
        assertThat(statuses[2]).isEqualTo(OutboxEventStatus.FAILED);
    }
}
