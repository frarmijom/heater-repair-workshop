package com.heaterworkshop.domain.inventory;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record InventoryItem(UUID id, String sku, String name, String description,
                           UUID categoryId, UUID unitId, BigDecimal stockCurrent,
                           BigDecimal stockMinimum, BigDecimal referenceUnitCost,
                           boolean active, long version, Instant createdAt, Instant updatedAt) {
    public InventoryItem {
        Objects.requireNonNull(id);
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(unitId);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        sku = normalizeSku(sku);
        name = CatalogText.required(name, 160);
        description = description == null ? null : CatalogText.required(description, 1000);
        stockCurrent = InventoryQuantity.nonNegative(stockCurrent, 3, "stockCurrent");
        stockMinimum = InventoryQuantity.nonNegative(stockMinimum, 3, "stockMinimum");
        referenceUnitCost = InventoryQuantity.nonNegative(referenceUnitCost, 4, "referenceUnitCost");
        if (version < 0 || updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("Versión o fecha inválida.");
    }

    public static InventoryItem create(String sku, String name, String description,
                                       UUID categoryId, UUID unitId,
                                       BigDecimal stockMinimum, BigDecimal referenceUnitCost) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return new InventoryItem(UUID.randomUUID(), sku, name, description, categoryId, unitId,
                BigDecimal.ZERO.setScale(3), stockMinimum, referenceUnitCost, true, 0, now, now);
    }

    public static InventoryItem restore(UUID id, String sku, String name, String description,
                                        UUID categoryId, UUID unitId, BigDecimal stockCurrent,
                                        BigDecimal stockMinimum, BigDecimal referenceUnitCost,
                                        boolean active, long version, Instant createdAt, Instant updatedAt) {
        return new InventoryItem(id, sku, name, description, categoryId, unitId, stockCurrent,
                stockMinimum, referenceUnitCost, active, version, createdAt, updatedAt);
    }

    public boolean lowStock() {
        return stockCurrent.compareTo(stockMinimum) < 0;
    }

    public InventoryItem applyMovement(InventoryMovement movement, boolean allowsDecimal) {
        Objects.requireNonNull(movement);
        if (!id.equals(movement.itemId()) || stockCurrent.compareTo(movement.stockBefore()) != 0) {
            throw new IllegalArgumentException("El movimiento no corresponde al stock actual del artículo.");
        }
        if (!active && movement.type() != InventoryMovementType.ADJUSTMENT
                && movement.type() != InventoryMovementType.REVERSAL) {
            throw new IllegalArgumentException("No se puede operar sobre un artículo inactivo.");
        }
        if (!allowsDecimal && movement.quantity().stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("La unidad no admite cantidades fraccionarias.");
        }
        return InventoryItem.restore(id, sku, name, description, categoryId, unitId,
                movement.stockAfter(), stockMinimum, referenceUnitCost, active,
                Math.addExact(version, 1), createdAt, Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    private static String normalizeSku(String value) {
        String normalized = Normalizer.normalize(CatalogText.required(value, 64), Normalizer.Form.NFC)
                .toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9][A-Z0-9._/-]{0,63}")) {
            throw new IllegalArgumentException("SKU inválido.");
        }
        return normalized;
    }
}