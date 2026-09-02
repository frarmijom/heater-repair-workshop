package com.heaterworkshop.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record StartRepairRequest(@NotBlank String diagnosis) {
}
