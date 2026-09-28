package com.heaterworkshop.domain.valueobject;

public record WorkOrderId(String value) {

    public WorkOrderId {
        if (value == null || !value.matches("ORDER-[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
            throw new IllegalArgumentException("Work order ID must use the format ORDER- followed by an uppercase UUID.");
        }
    }
}
