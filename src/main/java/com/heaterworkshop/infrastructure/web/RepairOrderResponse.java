package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.entity.RepairStatus;

import java.time.Instant;

public record RepairOrderResponse(String id, String customerName, String customerContact,
                                  String heaterBrand, String heaterModel, String reportedIssue,
                                  String diagnosis, RepairStatus status, Instant receivedAt,
                                  Instant completedAt) {
    public static RepairOrderResponse from(RepairOrder order) {
        return new RepairOrderResponse(order.id().value(), order.customerName(),
                order.customerContact().value(), order.heaterBrand(), order.heaterModel(),
                order.reportedIssue(), order.diagnosis() == null ? null : order.diagnosis().value(),
                order.status(), order.receivedAt(), order.completedAt());
    }
}
