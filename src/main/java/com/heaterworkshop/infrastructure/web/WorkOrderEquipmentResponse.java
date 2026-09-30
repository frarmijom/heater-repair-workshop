package com.heaterworkshop.infrastructure.web;
import com.heaterworkshop.domain.entity.WorkOrderEquipment;
public record WorkOrderEquipmentResponse(String id, String brand, String model, String capacity, String serialNumber, String notes, int position) {
    public static WorkOrderEquipmentResponse from(WorkOrderEquipment equipment) {
        return new WorkOrderEquipmentResponse(equipment.id().value().toString(), equipment.brand(), equipment.model(), equipment.capacity(), equipment.serialNumber(), equipment.notes(), equipment.position());
    }
}
