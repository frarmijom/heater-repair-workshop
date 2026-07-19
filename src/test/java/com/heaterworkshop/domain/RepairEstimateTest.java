package com.heaterworkshop.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Repair Estimate")
class RepairEstimateTest {

    @Test
    @DisplayName("Should initialize an estimate with zero total and no required parts")
    void shouldInitializeEstimateWithZeroTotalAndNoRequiredParts() {
        // Arrange
        double expectedTotal = 0.0;

        // Act
        RepairEstimate estimate = new RepairEstimate();

        // Assert
        assertEquals(expectedTotal, estimate.getTotal());
        assertTrue(estimate.getRequiredParts().isEmpty());
        assertEquals(0, estimate.getTotalParts());
    }
}
