package com.heaterworkshop.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record RecordDiagnosisRequest(@NotBlank String diagnosis) {
}
