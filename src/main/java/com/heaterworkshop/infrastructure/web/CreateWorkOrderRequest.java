package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.domain.entity.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateWorkOrderRequest(
        @NotBlank String customerName,
        @NotBlank @Pattern(regexp = "\\+[1-9][0-9]{7,14}") String customerContact,
        @NotBlank String heaterBrand,
        @NotBlank String heaterModel,
        @NotNull ServiceType serviceType,
        String reportedIssue) {
}
