package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.domain.service.WorkOrderEquipmentService;

public record WorkOrderEquipmentServiceResponse(String serviceId) {
    public static WorkOrderEquipmentServiceResponse from(WorkOrderEquipmentService assignment) {
        return new WorkOrderEquipmentServiceResponse(assignment.serviceId().toString());
    }
}
