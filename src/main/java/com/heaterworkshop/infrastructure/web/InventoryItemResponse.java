package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.domain.inventory.InventoryCategory;
import com.heaterworkshop.domain.inventory.InventoryItem;
import com.heaterworkshop.domain.inventory.InventoryItemType;
import com.heaterworkshop.domain.inventory.UnitOfMeasure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryItemResponse(UUID id, String sku, String name, String description, InventoryItemType itemType,
                                    Category category, Unit unit, BigDecimal stockCurrent,
                                    BigDecimal stockMinimum, BigDecimal referenceUnitCost,
                                    boolean lowStock, boolean active, boolean hasMovements,
                                    long version, Instant createdAt, Instant updatedAt) {
    public record Category(UUID id, String name) {}
    public record Unit(UUID id, String name, String symbol, boolean allowsDecimal) {}

    public static InventoryItemResponse from(InventoryItem item, InventoryCategory category,
                                             UnitOfMeasure unit, boolean hasMovements) {
        return new InventoryItemResponse(item.id(), item.sku(), item.name(), item.description(), item.itemType(),
                new Category(category.id(), category.name()),
                new Unit(unit.id(), unit.name(), unit.symbol(), unit.allowsDecimal()),
                item.stockCurrent(), item.stockMinimum(), item.referenceUnitCost(),
                item.lowStock(), item.active(), hasMovements, item.version(), item.createdAt(), item.updatedAt());
    }
}