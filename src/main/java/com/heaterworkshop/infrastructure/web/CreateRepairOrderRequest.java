package com.heaterworkshop.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateRepairOrderRequest(
        @NotBlank String customerName,
        @NotBlank @Pattern(regexp = "\\+[1-9][0-9]{7,14}") String customerContact,
        @NotBlank String heaterBrand,
        @NotBlank String heaterModel,
        @NotBlank String reportedIssue) {
}
