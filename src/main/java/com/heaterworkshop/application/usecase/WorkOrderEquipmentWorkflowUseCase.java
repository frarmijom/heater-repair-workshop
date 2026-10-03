package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.LifecycleVersion;
import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.entity.WorkOrderEquipment;
import com.heaterworkshop.domain.exception.WorkOrderNotFoundException;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;

import java.util.function.Consumer;

public final class WorkOrderEquipmentWorkflowUseCase {

    private final WorkOrderRepository repository;

    public WorkOrderEquipmentWorkflowUseCase(WorkOrderRepository repository) {
        this.repository = repository;
    }

    public WorkOrder beginDiagnosis(WorkOrderId orderId, EquipmentId equipmentId) {
        return change(orderId, equipmentId, lifecycle -> lifecycle.beginDiagnosis());
    }

    public WorkOrder recordDiagnosis(
            WorkOrderId orderId,
            EquipmentId equipmentId,
            Diagnosis diagnosis) {

        return change(orderId, equipmentId, lifecycle -> lifecycle.recordDiagnosis(diagnosis));
    }

    public WorkOrder completeDiagnosis(WorkOrderId orderId, EquipmentId equipmentId) {
        return change(orderId, equipmentId, lifecycle -> lifecycle.completeDiagnosis());
    }

    public WorkOrder approve(
            WorkOrderId orderId,
            EquipmentId equipmentId,
            boolean partsAvailable) {

        return change(
                orderId,
                equipmentId,
                lifecycle -> lifecycle.approve(partsAvailable));
    }

    public WorkOrder reject(WorkOrderId orderId, EquipmentId equipmentId) {
        return change(orderId, equipmentId, lifecycle -> lifecycle.reject());
    }

    public WorkOrder waitForParts(WorkOrderId orderId, EquipmentId equipmentId) {
        return change(orderId, equipmentId, lifecycle -> lifecycle.waitForParts());
    }

    public WorkOrder startWork(WorkOrderId orderId, EquipmentId equipmentId) {
        return change(orderId, equipmentId, lifecycle -> lifecycle.startWork());
    }

    public WorkOrder complete(
            WorkOrderId orderId,
            EquipmentId equipmentId,
            java.time.Instant completedAt) {

        return change(
                orderId,
                equipmentId,
                lifecycle -> lifecycle.complete(completedAt));
    }

    private WorkOrder change(
            WorkOrderId orderId,
            EquipmentId equipmentId,
            Consumer<com.heaterworkshop.domain.entity.WorkOrderEquipmentLifecycle> action) {

        WorkOrder order = repository.findById(orderId)
                .orElseThrow(() -> new WorkOrderNotFoundException(orderId.value()));

        if (order.lifecycleVersion() != LifecycleVersion.V2) {
            throw new IllegalArgumentException(
                    "Equipment lifecycle operations require a V2 work order.");
        }

        WorkOrderEquipment equipment = order.equipments().stream()
                .filter(candidate -> candidate.id().equals(equipmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Equipment does not belong to work order."));

        action.accept(equipment.lifecycle());
        repository.save(order);
        return order;
    }
}
