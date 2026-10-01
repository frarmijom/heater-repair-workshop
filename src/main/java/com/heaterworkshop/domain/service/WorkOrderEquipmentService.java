package com.heaterworkshop.domain.service;

import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;

import java.util.Objects;
import java.util.UUID;

public record WorkOrderEquipmentService(
        WorkOrderId workOrderId,
        EquipmentId equipmentId,
        UUID serviceId) {

    public WorkOrderEquipmentService {
        Objects.requireNonNull(workOrderId, "Work order ID is required.");
        Objects.requireNonNull(equipmentId, "Equipment ID is required.");
        Objects.requireNonNull(serviceId, "Service ID is required.");
    }
}
