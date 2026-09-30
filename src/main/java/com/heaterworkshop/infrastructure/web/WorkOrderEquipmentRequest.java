package com.heaterworkshop.infrastructure.web;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
public record WorkOrderEquipmentRequest(@NotBlank String brand, @NotBlank String model, String capacity, String serialNumber, String notes, @Min(1) int position) {}
