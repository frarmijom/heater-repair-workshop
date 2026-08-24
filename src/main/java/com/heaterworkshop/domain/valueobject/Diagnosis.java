package com.heaterworkshop.domain.valueobject;

import com.heaterworkshop.domain.exception.InvalidDiagnosisException;

public record Diagnosis(String value) {

    public Diagnosis {
        if (value == null || value.isBlank()) {
            throw new InvalidDiagnosisException("Diagnosis must not be blank.");
        }

        value = value.trim();
    }
}
