package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.domain.entity.CustomerDecision;
import com.heaterworkshop.domain.entity.EquipmentIntakeRoute;
import com.heaterworkshop.domain.entity.EquipmentType;
import com.heaterworkshop.domain.entity.WorkOrderEquipment;
import com.heaterworkshop.domain.entity.WorkOrderEquipmentLifecycle;
import com.heaterworkshop.domain.entity.WorkOrderStatus;

import java.time.Instant;

public record WorkOrderEquipmentResponse(
        String id,
        EquipmentType type,
        String brand,
        String model,
        String capacity,
        String serialNumber,
        String notes,
        int position,
        EquipmentIntakeRoute intakeRoute,
        WorkOrderStatus status,
        String reportedIssue,
        String diagnosis,
        CustomerDecision customerDecision,
        Instant receivedAt,
        Instant completedAt) {

    public static WorkOrderEquipmentResponse from(WorkOrderEquipment equipment) {
        WorkOrderEquipmentLifecycle lifecycle = equipment.lifecycle();

        return new WorkOrderEquipmentResponse(
                equipment.id().value().toString(),
                equipment.type(),
                equipment.brand(),
                equipment.model(),
                equipment.capacity(),
                equipment.serialNumber(),
                equipment.notes(),
                equipment.position(),
                lifecycle == null ? null : lifecycle.intakeRoute(),
                lifecycle == null ? null : lifecycle.status(),
                lifecycle == null ? null : lifecycle.reportedIssue(),
                lifecycle == null || lifecycle.diagnosis() == null
                        ? null
                        : lifecycle.diagnosis().value(),
                lifecycle == null ? null : lifecycle.customerDecision(),
                lifecycle == null ? null : lifecycle.receivedAt(),
                lifecycle == null ? null : lifecycle.completedAt());
    }
}
