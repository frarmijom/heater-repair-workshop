package com.heaterworkshop.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateRepairOrderRequest(
        @NotBlank @Pattern(regexp = "ORDER-[0-9]{3,}") String id,
        @NotBlank @Pattern(regexp = "\\+[1-9][0-9]{7,14}") String customerContact) {
}
