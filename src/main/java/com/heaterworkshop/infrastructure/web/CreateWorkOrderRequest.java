package com.heaterworkshop.infrastructure.web;
import com.heaterworkshop.domain.entity.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
public record CreateWorkOrderRequest(
        @NotBlank String customerName,
        @NotBlank @Pattern(regexp = "\\+[1-9][0-9]{7,14}") String customerContact,
        String heaterBrand,
        String heaterModel,
        List<@Valid WorkOrderEquipmentRequest> equipments,
        @NotNull ServiceType serviceType,
        String reportedIssue) {}
