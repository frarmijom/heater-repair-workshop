package com.heaterworkshop.domain.inventory;

public enum InventoryMovementType {
    INITIAL_ENTRY,
    ENTRY,
    WORK_ORDER_CONSUMPTION,
    ADJUSTMENT,
    REVERSAL;

    public boolean isReversible() {
        return this == ENTRY || this == ADJUSTMENT;
    }
}