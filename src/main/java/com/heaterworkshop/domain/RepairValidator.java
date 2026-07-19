package com.heaterworkshop.domain;

import com.heaterworkshop.domain.exception.InvalidDiagnosisException;

public class RepairValidator {

    public void validateDiagnosis(String diagnosis) {
        if (diagnosis == null || diagnosis.trim().isEmpty()) {
            throw new InvalidDiagnosisException("Diagnosis must not be blank.");
        }
    }
}
