package com.heaterworkshop.infrastructure.web;
import com.heaterworkshop.domain.entity.*;
import java.time.Instant;
import java.util.List;
public record WorkOrderResponse(String id, String customerName, String customerContact,
                                String heaterBrand, String heaterModel, List<WorkOrderEquipmentResponse> equipments,
                                ServiceType serviceType, String reportedIssue, String diagnosis, WorkOrderStatus status,
                                Instant receivedAt, Instant completedAt, LifecycleVersion lifecycleVersion,
                                WorkOrderStatus legacyStatus, CustomerDecision customerDecision) {
    public static WorkOrderResponse from(WorkOrder order) {
        return new WorkOrderResponse(order.id().value(), order.customerName(), order.customerContact().value(),
                order.heaterBrand(), order.heaterModel(), order.equipments().stream().map(WorkOrderEquipmentResponse::from).toList(),
                order.serviceType(), order.reportedIssue(), order.diagnosis() == null ? null : order.diagnosis().value(),
                order.status(), order.receivedAt(), order.completedAt(), order.lifecycleVersion(), order.legacyStatus(), order.customerDecision());
    }
}
