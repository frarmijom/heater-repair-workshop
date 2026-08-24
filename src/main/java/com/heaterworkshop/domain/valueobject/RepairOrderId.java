package com.heaterworkshop.domain.valueobject;

public record RepairOrderId(String value) {

    public RepairOrderId {
        if (value == null || !value.matches("ORDER-[0-9]{3,}")) {
            throw new IllegalArgumentException("Repair order ID must use the format ORDER- followed by at least three digits.");
        }
    }
}
