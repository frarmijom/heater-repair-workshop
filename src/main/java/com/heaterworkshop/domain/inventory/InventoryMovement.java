package com.heaterworkshop.domain.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public record InventoryMovement(UUID id, UUID itemId, InventoryMovementType type,
                                InventoryMovementDirection direction, BigDecimal quantity,
                                BigDecimal stockBefore, BigDecimal stockAfter,
                                BigDecimal unitCostSnapshot, String requestId, Instant occurredAt,
                                String actor, String reason, String referenceType, String referenceId,
                                String workOrderId, UUID reversalOfMovementId,
                                String skuSnapshot, String itemNameSnapshot,
                                String unitNameSnapshot, String unitSymbolSnapshot) {
    public InventoryMovement {
        Objects.requireNonNull(id);
        Objects.requireNonNull(itemId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(direction);
        Objects.requireNonNull(occurredAt);
        quantity = InventoryQuantity.exact(quantity, 3, "quantity");
        stockBefore = InventoryQuantity.nonNegative(stockBefore, 3, "stockBefore");
        stockAfter = InventoryQuantity.nonNegative(stockAfter, 3, "stockAfter");
        unitCostSnapshot = InventoryQuantity.nonNegative(unitCostSnapshot, 4, "unitCostSnapshot");
        if (quantity.signum() <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        BigDecimal expectedAfter = direction == InventoryMovementDirection.INCREASE
                ? stockBefore.add(quantity) : stockBefore.subtract(quantity);
        if (expectedAfter.signum() < 0 || expectedAfter.compareTo(stockAfter) != 0) {
            throw new IllegalArgumentException("stockBefore y stockAfter no corresponden al movimiento.");
        }
        requestId = CatalogText.required(requestId, 128);
        actor = CatalogText.required(actor, 254);
        reason = reason == null ? null : CatalogText.required(reason, 1000);
        referenceType = referenceType == null ? null : CatalogText.required(referenceType, 80);
        referenceId = referenceId == null ? null : CatalogText.required(referenceId, 160);
        if ((referenceType == null) != (referenceId == null)) throw new IllegalArgumentException("La referencia está incompleta.");
        if (workOrderId != null) workOrderId = CatalogText.required(workOrderId, 64);
        skuSnapshot = CatalogText.required(skuSnapshot, 64);
        itemNameSnapshot = CatalogText.required(itemNameSnapshot, 160);
        unitNameSnapshot = CatalogText.required(unitNameSnapshot, 120);
        unitSymbolSnapshot = CatalogText.required(unitSymbolSnapshot, 16);
        if ((type == InventoryMovementType.INITIAL_ENTRY
                || type == InventoryMovementType.ENTRY
                || type == InventoryMovementType.KIT_ASSEMBLY_PRODUCTION)
                && direction != InventoryMovementDirection.INCREASE
                || (type == InventoryMovementType.WORK_ORDER_CONSUMPTION
                || type == InventoryMovementType.KIT_ASSEMBLY_CONSUMPTION)
                && direction != InventoryMovementDirection.DECREASE) {
            throw new IllegalArgumentException("La dirección no coincide con el tipo de movimiento.");
        }
        if ((type == InventoryMovementType.REVERSAL) != (reversalOfMovementId != null)) {
            throw new IllegalArgumentException("La referencia de reversión no coincide con el tipo.");
        }
        if ((type == InventoryMovementType.WORK_ORDER_CONSUMPTION) != (workOrderId != null)) {
            throw new IllegalArgumentException("La OT solo corresponde a consumos.");
        }
    }

    public static InventoryMovement create(InventoryItem item, UnitOfMeasure unit,
                                           InventoryMovementType type, InventoryMovementDirection direction,
                                           BigDecimal quantity, BigDecimal unitCostSnapshot, String requestId,
                                           String actor, String reason, String referenceType, String referenceId,
                                           String workOrderId, UUID reversalOfMovementId) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(unit);
        if (!unit.id().equals(item.unitId())) throw new IllegalArgumentException("La unidad no corresponde al artículo.");
        BigDecimal exactQuantity = InventoryQuantity.exact(quantity, 3, "quantity");
        BigDecimal after = direction == InventoryMovementDirection.INCREASE
                ? item.stockCurrent().add(exactQuantity) : item.stockCurrent().subtract(exactQuantity);
        return new InventoryMovement(UUID.randomUUID(), item.id(), type, direction, exactQuantity,
                item.stockCurrent(), after, unitCostSnapshot, requestId,
                Instant.now().truncatedTo(ChronoUnit.MICROS), actor, reason, referenceType,
                referenceId, workOrderId, reversalOfMovementId, item.sku(), item.name(),
                unit.name(), unit.symbol());
    }

    public static InventoryMovement restore(UUID id, UUID itemId, InventoryMovementType type,
                                            InventoryMovementDirection direction, BigDecimal quantity,
                                            BigDecimal stockBefore, BigDecimal stockAfter,
                                            BigDecimal unitCostSnapshot, String requestId, Instant occurredAt,
                                            String actor, String reason, String referenceType, String referenceId,
                                            String workOrderId, UUID reversalOfMovementId, String skuSnapshot,
                                            String itemNameSnapshot, String unitNameSnapshot, String unitSymbolSnapshot) {
        return new InventoryMovement(id, itemId, type, direction, quantity, stockBefore, stockAfter,
                unitCostSnapshot, requestId, occurredAt, actor, reason, referenceType, referenceId,
                workOrderId, reversalOfMovementId, skuSnapshot, itemNameSnapshot,
                unitNameSnapshot, unitSymbolSnapshot);
    }
}