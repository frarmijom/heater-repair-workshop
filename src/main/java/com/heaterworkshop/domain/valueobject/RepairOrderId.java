package com.heaterworkshop.domain.valueobject;

public record RepairOrderId(String value) {

    public RepairOrderId {
        if (value == null || !value.matches("ORDER-[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
            throw new IllegalArgumentException("Repair order ID must use the format ORDER- followed by an uppercase UUID.");
        }
    }
}
