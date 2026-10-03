package com.heaterworkshop.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateWorkOrderV2Request(
        @NotBlank String customerName,
        @NotBlank @Pattern(regexp = "\\+[1-9][0-9]{7,14}") String customerContact,
        @NotEmpty List<@Valid WorkOrderEquipmentV2Request> equipments) {
}
