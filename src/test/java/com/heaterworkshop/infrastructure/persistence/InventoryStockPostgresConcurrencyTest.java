package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.application.inventory.RecordInventoryMovementUseCase;
import com.heaterworkshop.domain.inventory.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@EnabledIfEnvironmentVariable(named = "I2_POSTGRES_TEST", matches = "true")
class InventoryStockPostgresConcurrencyTest {
    @Autowired private InventoryCategoryRepository categories;
    @Autowired private UnitOfMeasureRepository units;
    @Autowired private InventoryItemRepository items;
    @Autowired private InventoryMovementRepository movements;
    @Autowired private RecordInventoryMovementUseCase recordMovement;
    @Autowired private com.heaterworkshop.application.inventory.UnitOfMeasureUseCases editUnits;

    @Test void concurrentConsumersSerializeAndPersistOneMovement() throws Exception {
        InventoryCategory category = categories.create(InventoryCategory.create("Race " + UUID.randomUUID()));
        UnitOfMeasure unit = units.create(UnitOfMeasure.create("Unit " + UUID.randomUUID(), "u" + UUID.randomUUID().toString().substring(0, 6), false));
        InventoryItem item = items.create(InventoryItem.create("RACE-" + UUID.randomUUID(), "Race item", null,
                category.id(), unit.id(), BigDecimal.ZERO, BigDecimal.ZERO));
        String initialRequest = "initial-" + UUID.randomUUID();
        InventoryMovement initial = recordMovement.record(item.id(), InventoryMovementType.INITIAL_ENTRY,
            InventoryMovementDirection.INCREASE, BigDecimal.ONE, initialRequest, "postgres-test", "fixture",
            null, null, null, null);
        assertEquals(initial.id(), recordMovement.record(item.id(), InventoryMovementType.INITIAL_ENTRY,
            InventoryMovementDirection.INCREASE, new BigDecimal("1.000"), initialRequest, "postgres-test",
            "fixture", null, null, null, null).id());
        assertThrows(CatalogConflictException.class, () -> recordMovement.record(item.id(),
            InventoryMovementType.INITIAL_ENTRY, InventoryMovementDirection.INCREASE,
            new BigDecimal("2.000"), initialRequest, "postgres-test", "fixture",
            null, null, null, null));
        assertThrows(CatalogConflictException.class, () -> editUnits.edit(unit.id(), unit.version(), unit.name(),
            unit.symbol(), true, unit.active()));

        CyclicBarrier start = new CyclicBarrier(2);
        String requestA = "race-a-" + UUID.randomUUID();
        String requestB = "race-b-" + UUID.randomUUID();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> consume(item.id(), start, requestA));
            var second = executor.submit(() -> consume(item.id(), start, requestB));
            assertNotEquals(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertEquals(0, items.findById(item.id()).orElseThrow().stockCurrent().compareTo(BigDecimal.ZERO));
        assertEquals(2, movements.countByItemId(item.id()));
        InventoryMovement consumed = movements.findByRequestId(requestA).orElseGet(() ->
            movements.findByRequestId(requestB).orElseThrow());
        InventoryMovement reversal = recordMovement.record(item.id(), InventoryMovementType.REVERSAL,
            InventoryMovementDirection.INCREASE, BigDecimal.ONE, "reverse-" + UUID.randomUUID(),
            "postgres-test", "compensation", null, null, null, consumed.id());
        assertEquals(consumed.unitCostSnapshot(), reversal.unitCostSnapshot());
        assertEquals(0, items.findById(item.id()).orElseThrow().stockCurrent().compareTo(BigDecimal.ONE));
        assertThrows(CatalogConflictException.class, () -> recordMovement.record(item.id(),
            InventoryMovementType.REVERSAL, InventoryMovementDirection.INCREASE, BigDecimal.ONE,
            "reverse-again-" + UUID.randomUUID(), "postgres-test", "duplicate", null, null,
            null, consumed.id()));
        assertEquals(3, movements.countByItemId(item.id()));
    }

    private boolean consume(UUID itemId, CyclicBarrier start, String requestId) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        try {
            recordMovement.record(itemId, InventoryMovementType.WORK_ORDER_CONSUMPTION,
                    InventoryMovementDirection.DECREASE, BigDecimal.ONE, requestId, "postgres-test",
                    "concurrent test", null, null,
                    "ORDER-550E8400-E29B-41D4-A716-446655440001", null);
            return true;
        } catch (IllegalArgumentException expectedWhenStockIsExhausted) {
            return false;
        }
    }
}