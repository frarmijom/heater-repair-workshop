package com.heaterworkshop.domain.valueobject;

import com.heaterworkshop.domain.exception.InvalidDiagnosisException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiagnosisTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsMissingOrBlankValues(String value) {
        InvalidDiagnosisException exception = assertThrows(
                InvalidDiagnosisException.class,
                () -> new Diagnosis(value)
        );

        assertEquals("Diagnosis must not be blank.", exception.getMessage());
    }

    @Test
    void trimsAValidDiagnosis() {
        assertEquals("Damaged ignition sensor", new Diagnosis("  Damaged ignition sensor  ").value());
    }
}
