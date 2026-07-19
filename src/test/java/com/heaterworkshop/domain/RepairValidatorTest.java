package com.heaterworkshop.domain;

import com.heaterworkshop.domain.exception.InvalidDiagnosisException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Repair Validator")
class RepairValidatorTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Should reject a missing or blank diagnosis")
    void shouldRejectMissingOrBlankDiagnosis(String invalidDiagnosis) {
        // Arrange
        RepairValidator validator = new RepairValidator();

        // Act
        InvalidDiagnosisException exception = assertThrows(
                InvalidDiagnosisException.class,
                () -> validator.validateDiagnosis(invalidDiagnosis)
        );

        // Assert
        assertEquals("Diagnosis must not be blank.", exception.getMessage());
    }

    @Test
    @DisplayName("Should accept a valid diagnosis")
    void shouldAcceptValidDiagnosis() {
        // Arrange
        RepairValidator validator = new RepairValidator();

        // Act and Assert
        assertDoesNotThrow(() -> validator.validateDiagnosis("Damaged ignition sensor"));
    }
}
