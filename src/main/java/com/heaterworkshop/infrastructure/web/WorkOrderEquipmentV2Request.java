package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.domain.entity.EquipmentIntakeRoute;
import com.heaterworkshop.domain.entity.EquipmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkOrderEquipmentV2Request(
        @NotNull EquipmentType type,
        @NotBlank String brand,
        @NotBlank String model,
        String capacity,
        String serialNumber,
        String notes,
        @Min(1) int position,
        @NotNull EquipmentIntakeRoute intakeRoute,
        String reportedIssue) {
}
