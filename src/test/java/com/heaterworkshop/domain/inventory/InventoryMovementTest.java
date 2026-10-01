package com.heaterworkshop.domain.inventory;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryMovementTest {
    @Test void storesImmutableSnapshotsAndExplicitAdjustmentDirection() {
        UnitOfMeasure unit = UnitOfMeasure.create("Metro", "m", true);
        InventoryItem item = item(unit, new BigDecimal("4.500"));
        InventoryMovement movement = movement(item, unit, InventoryMovementType.ADJUSTMENT,
                InventoryMovementDirection.DECREASE, new BigDecimal("1.250"), null);

        assertEquals(new BigDecimal("4.500"), movement.stockBefore());
        assertEquals(new BigDecimal("3.250"), movement.stockAfter());
        assertEquals("M", movement.skuSnapshot());
        assertEquals("Filtro", movement.itemNameSnapshot());
        assertEquals("Metro", movement.unitNameSnapshot());
        assertEquals("m", movement.unitSymbolSnapshot());
        assertEquals(new BigDecimal("3.0000"), movement.unitCostSnapshot());
        assertEquals(InventoryMovementType.ADJUSTMENT, movement.type());
        assertThrows(IllegalArgumentException.class, () -> movement(item, unit,
                InventoryMovementType.ENTRY, InventoryMovementDirection.DECREASE,
                BigDecimal.ONE, null));
        assertThrows(IllegalArgumentException.class, () -> movement(item, unit,
                InventoryMovementType.ADJUSTMENT, InventoryMovementDirection.DECREASE,
                new BigDecimal("5.000"), null));
    }

    @Test void requiresPositiveExactQuantityAndCompleteReversalReference() {
        UnitOfMeasure unit = UnitOfMeasure.create("Unidad", "un", false);
        InventoryItem item = item(unit, BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> movement(item, unit,
                InventoryMovementType.ENTRY, InventoryMovementDirection.INCREASE, BigDecimal.ZERO, null));
        assertThrows(IllegalArgumentException.class, () -> movement(item, unit,
                InventoryMovementType.ENTRY, InventoryMovementDirection.INCREASE,
                new BigDecimal("1.0001"), null));
        assertThrows(IllegalArgumentException.class, () -> movement(item, unit,
                InventoryMovementType.REVERSAL, InventoryMovementDirection.DECREASE,
                BigDecimal.ONE, null));
    }

    @Test void exposesI5ReversibilityPolicy() {
        assertFalse(InventoryMovementType.INITIAL_ENTRY.isReversible());
        assertTrue(InventoryMovementType.ENTRY.isReversible());
        assertTrue(InventoryMovementType.ADJUSTMENT.isReversible());
        assertFalse(InventoryMovementType.WORK_ORDER_CONSUMPTION.isReversible());
        assertFalse(InventoryMovementType.KIT_ASSEMBLY_CONSUMPTION.isReversible());
        assertFalse(InventoryMovementType.KIT_ASSEMBLY_PRODUCTION.isReversible());
        assertFalse(InventoryMovementType.REVERSAL.isReversible());
    }

    @Test void wholeUnitRejectsFractionButDecimalUnitAcceptsIt() {
        UnitOfMeasure whole = UnitOfMeasure.create("Unidad", "un", false);
        UnitOfMeasure fractional = UnitOfMeasure.create("Metro", "m", true);
        InventoryItem wholeItem = item(whole, BigDecimal.ZERO);
        InventoryItem fractionalItem = item(fractional, BigDecimal.ZERO);
        InventoryMovement fractionalEntry = movement(fractionalItem, fractional,
                InventoryMovementType.ENTRY, InventoryMovementDirection.INCREASE,
                new BigDecimal("1.500"), null);

        assertThrows(IllegalArgumentException.class, () -> wholeItem.applyMovement(
                movement(wholeItem, whole, InventoryMovementType.ENTRY,
                        InventoryMovementDirection.INCREASE, new BigDecimal("1.500"), null), false));
        assertEquals(new BigDecimal("1.500"), fractionalItem.applyMovement(fractionalEntry, true).stockCurrent());
    }

    private InventoryMovement movement(InventoryItem item, UnitOfMeasure unit, InventoryMovementType type,
                                      InventoryMovementDirection direction, BigDecimal quantity,
                                      UUID reversalOf) {
        return InventoryMovement.create(item, unit, type, direction, quantity,
                new BigDecimal("3.0000"), UUID.randomUUID().toString(), "technician-1", "test reason",
                "TEST", UUID.randomUUID().toString(), null, reversalOf);
    }

    private InventoryItem item(UnitOfMeasure unit, BigDecimal stock) {
        Instant now = Instant.now();
        return InventoryItem.restore(UUID.randomUUID(), "m", "Filtro", null, UUID.randomUUID(), unit.id(),
                stock, BigDecimal.ZERO.setScale(3), new BigDecimal("3.0000"), true, 0, now, now);
    }
}