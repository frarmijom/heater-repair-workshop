package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.entity.WorkOrderStatus;
import com.heaterworkshop.domain.entity.ServiceType;
import com.heaterworkshop.domain.entity.LifecycleVersion;
import com.heaterworkshop.domain.entity.CustomerDecision;

import java.time.Instant;

public record WorkOrderResponse(String id, String customerName, String customerContact,
                                  String heaterBrand, String heaterModel, ServiceType serviceType, String reportedIssue,
                                  String diagnosis, WorkOrderStatus status, Instant receivedAt,
                                  Instant completedAt, LifecycleVersion lifecycleVersion,
                                  WorkOrderStatus legacyStatus, CustomerDecision customerDecision) {
    public static WorkOrderResponse from(WorkOrder order) {
        return new WorkOrderResponse(order.id().value(), order.customerName(),
                order.customerContact().value(), order.heaterBrand(), order.heaterModel(),
                order.serviceType(), order.reportedIssue(), order.diagnosis() == null ? null : order.diagnosis().value(),
                order.status(), order.receivedAt(), order.completedAt(),
                order.lifecycleVersion(), order.legacyStatus(), order.customerDecision());
    }
}
