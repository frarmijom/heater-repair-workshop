package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.entity.RepairStatus;

public record RepairOrderResponse(String id, String customerContact, RepairStatus status, String diagnosis) {
    public static RepairOrderResponse from(RepairOrder order) {
        return new RepairOrderResponse(order.id().value(), order.customerContact().value(), order.status(),
                order.diagnosis() == null ? null : order.diagnosis().value());
    }
}
