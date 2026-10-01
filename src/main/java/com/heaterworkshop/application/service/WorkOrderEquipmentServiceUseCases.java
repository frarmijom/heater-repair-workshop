package com.heaterworkshop.application.service;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.inventory.CatalogNotFoundException;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import com.heaterworkshop.domain.service.ServiceCatalogRepository;
import com.heaterworkshop.domain.service.WorkOrderEquipmentService;
import com.heaterworkshop.domain.service.WorkOrderEquipmentServiceRepository;
import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import com.heaterworkshop.domain.exception.WorkOrderNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class WorkOrderEquipmentServiceUseCases {
    private final WorkOrderRepository workOrders;
    private final ServiceCatalogRepository services;
    private final WorkOrderEquipmentServiceRepository assignments;

    public WorkOrderEquipmentServiceUseCases(
            WorkOrderRepository workOrders,
            ServiceCatalogRepository services,
            WorkOrderEquipmentServiceRepository assignments) {
        this.workOrders = workOrders;
        this.services = services;
        this.assignments = assignments;
    }

    @Transactional(readOnly = true)
    public List<WorkOrderEquipmentService> get(WorkOrderId workOrderId, EquipmentId equipmentId) {
        requireEquipment(workOrderId, equipmentId);
        return assignments.findByWorkOrderIdAndEquipmentId(workOrderId, equipmentId);
    }

    @Transactional
    public List<WorkOrderEquipmentService> replace(
            WorkOrderId workOrderId,
            EquipmentId equipmentId,
            List<UUID> serviceIds) {
        requireEquipment(workOrderId, equipmentId);
        Objects.requireNonNull(serviceIds, "serviceIds es obligatorio.");

        if (new HashSet<>(serviceIds).size() != serviceIds.size()) {
            throw new IllegalArgumentException("serviceIds no puede contener servicios duplicados.");
        }

        List<WorkOrderEquipmentService> replacement = serviceIds.stream()
                .map(serviceId -> {
                    UUID id = Objects.requireNonNull(serviceId, "serviceId es obligatorio.");
                    services.findById(id).orElseThrow(CatalogNotFoundException::new);
                    return new WorkOrderEquipmentService(workOrderId, equipmentId, id);
                })
                .toList();

        assignments.replace(workOrderId, equipmentId, replacement);
        return assignments.findByWorkOrderIdAndEquipmentId(workOrderId, equipmentId);
    }

    private WorkOrder requireEquipment(WorkOrderId workOrderId, EquipmentId equipmentId) {
        WorkOrder order = workOrders.findById(workOrderId)
                .orElseThrow(() -> new WorkOrderNotFoundException(workOrderId.value()));
        boolean belongsToOrder = order.equipments().stream()
                .anyMatch(equipment -> equipment.id().equals(equipmentId));
        if (!belongsToOrder) {
            throw new IllegalArgumentException("El equipo no pertenece a la orden de trabajo.");
        }
        return order;
    }
}
