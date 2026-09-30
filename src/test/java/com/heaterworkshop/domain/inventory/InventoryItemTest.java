package com.heaterworkshop.domain.inventory;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryItemTest {
    @Test void creationNormalizesSkuAndStartsWithoutStock() {
        InventoryItem item = create(" filter-01 ", new BigDecimal("2.500"), new BigDecimal("4.1250"));

        assertEquals("FILTER-01", item.sku());
        assertEquals(0, item.stockCurrent().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("2.500"), item.stockMinimum());
        assertEquals(new BigDecimal("4.1250"), item.referenceUnitCost());
        assertTrue(item.lowStock());
        assertEquals(InventoryItemType.STANDARD, item.itemType());
    }

    @Test void rejectsNegativeAndInexactValues() {
        assertThrows(IllegalArgumentException.class, () -> create("SKU-1", new BigDecimal("-1"), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> create("SKU-1", new BigDecimal("1.0001"), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> create("SKU-1", BigDecimal.ZERO, new BigDecimal("1.00000")));
    }

    @Test void stockChangesOnlyThroughAConsistentMovement() {
        UnitOfMeasure unit = UnitOfMeasure.create("Unidad", "un", false);
        InventoryItem item = create("SKU-1", BigDecimal.ZERO, BigDecimal.ZERO, unit.id());
        InventoryMovement movement = InventoryMovement.create(item, unit, InventoryMovementType.INITIAL_ENTRY,
                InventoryMovementDirection.INCREASE, new BigDecimal("1.000"), BigDecimal.ZERO,
            UUID.randomUUID().toString(), "operator", null, null, null, null, null);

        InventoryItem stocked = item.applyMovement(movement, false);
        assertEquals(new BigDecimal("1.000"), stocked.stockCurrent());
        assertEquals(1, stocked.version());
        assertFalse(stocked.lowStock());
        assertThrows(IllegalArgumentException.class, () -> stocked.applyMovement(movement, false));
        assertThrows(IllegalArgumentException.class, () -> item.applyMovement(
                InventoryMovement.create(item, unit, InventoryMovementType.INITIAL_ENTRY,
                        InventoryMovementDirection.INCREASE, new BigDecimal("1.500"), BigDecimal.ZERO,
                    UUID.randomUUID().toString(), "operator", null, null, null, null, null), false));
    }

    private InventoryItem create(String sku, BigDecimal minimum, BigDecimal cost) {
        return create(sku, minimum, cost, UUID.randomUUID());
    }

    private InventoryItem create(String sku, BigDecimal minimum, BigDecimal cost, UUID unitId) {
        Instant now = Instant.now();
        return InventoryItem.restore(UUID.randomUUID(), sku, "Filtro", null, UUID.randomUUID(), unitId,
                BigDecimal.ZERO.setScale(3), minimum, cost, true, 0, now, now);
    }
}