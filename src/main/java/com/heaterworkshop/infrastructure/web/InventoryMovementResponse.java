package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.domain.inventory.InventoryMovement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryMovementResponse(UUID id, UUID itemId, String type, String direction,
                                        BigDecimal quantity, BigDecimal stockBefore, BigDecimal stockAfter,
                                        BigDecimal unitCostSnapshot, String requestId, Instant occurredAt,
                                        String actor, String reason, String referenceType, String referenceId,
                                        String skuSnapshot, String itemNameSnapshot,
                                        String unitNameSnapshot, String unitSymbolSnapshot) {
    public static InventoryMovementResponse from(InventoryMovement value) {
        return new InventoryMovementResponse(value.id(), value.itemId(), value.type().name(), value.direction().name(),
                value.quantity(), value.stockBefore(), value.stockAfter(), value.unitCostSnapshot(),
                value.requestId(), value.occurredAt(), value.actor(), value.reason(), value.referenceType(),
                value.referenceId(), value.skuSnapshot(), value.itemNameSnapshot(), value.unitNameSnapshot(),
                value.unitSymbolSnapshot());
    }
}
