package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.application.inventory.InventoryItemUseCases;
import com.heaterworkshop.application.inventory.UnitOfMeasureUseCases;
import com.heaterworkshop.domain.inventory.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@EnabledIfEnvironmentVariable(named = "I3_POSTGRES_TEST", matches = "true")
class InventoryItemsPostgresIntegrationTest {
    @Autowired private InventoryItemUseCases items;
    @Autowired private InventoryItemRepository itemRepository;
    @Autowired private InventoryItemCreationRequestRepository requests;
    @Autowired private InventoryMovementRepository movements;
    @Autowired private InventoryCategoryRepository categories;
    @Autowired private UnitOfMeasureRepository units;
    @Autowired private UnitOfMeasureUseCases editUnits;
    @Autowired private JdbcTemplate jdbc;

    @Test void concurrentCreateReplayAndInitialMovementAreIdempotent() throws Exception {
        InventoryCategory category = categories.create(InventoryCategory.create("I3 " + UUID.randomUUID()));
        UnitOfMeasure unit = units.create(UnitOfMeasure.create("I3 unit " + UUID.randomUUID(),
                "i" + UUID.randomUUID().toString().substring(0, 6), true));
        String requestId = "i3-concurrent-" + UUID.randomUUID();
        CyclicBarrier start = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        InventoryItem first;
        InventoryItem second;
        try {
            var one = executor.submit(() -> createTogether(start, category, unit, requestId));
            var two = executor.submit(() -> createTogether(start, category, unit, requestId));
            first = one.get(30, TimeUnit.SECONDS);
            second = two.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertEquals(first.id(), second.id());
        assertEquals(0, first.stockCurrent().compareTo(new BigDecimal("10.000")));
        assertEquals(1, movements.countByItemId(first.id()));
        assertEquals(1, itemRepository.findAll().stream().filter(item -> item.id().equals(first.id())).count());
        InventoryMovement movement = movements.findByRequestId(requestId).orElseThrow();
        assertEquals(InventoryMovementType.INITIAL_ENTRY, movement.type());
        assertEquals(new BigDecimal("10.000"), movement.quantity());
        assertEquals("i3-test@example.test", movement.actor());
        assertEquals(first.id(), requests.findByRequestId(requestId).orElseThrow().itemId());
        assertThrows(CatalogConflictException.class, () -> editUnits.edit(unit.id(), unit.version(),
                unit.name(), unit.symbol(), false, unit.active()));
    }

    @Test void zeroStockCanChangeUnitAndMovementFailureRollsBackTheWholeCreate() {
        InventoryCategory category = categories.create(InventoryCategory.create("I3 rollback " + UUID.randomUUID()));
        UnitOfMeasure firstUnit = units.create(UnitOfMeasure.create("Unit A " + UUID.randomUUID(),
                "a" + UUID.randomUUID().toString().substring(0, 6), false));
        UnitOfMeasure secondUnit = units.create(UnitOfMeasure.create("Unit B " + UUID.randomUUID(),
                "b" + UUID.randomUUID().toString().substring(0, 6), true));
        InventoryItem zero = items.create("I3-ZERO-" + UUID.randomUUID(), "Zero item", null,
                category.id(), firstUnit.id(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "i3-zero-" + UUID.randomUUID(), "i3-test@example.test");
        InventoryItem changed = items.edit(zero.id(), zero.version(), null, null, zero.description(), null,
                secondUnit.id(), null, null, null);
        assertEquals(secondUnit.id(), changed.unitId());
        assertEquals(0, movements.countByItemId(zero.id()));

        String failRequest = "i3-fail-" + UUID.randomUUID();
        jdbc.execute("CREATE FUNCTION fail_test_initial_entry() RETURNS trigger LANGUAGE plpgsql AS $$ " +
                "BEGIN RAISE EXCEPTION 'forced initial movement failure'; END; $$");
        jdbc.execute("CREATE TRIGGER fail_test_initial_entry BEFORE INSERT ON inventory_movements " +
                "FOR EACH ROW WHEN (NEW.request_id = '" + failRequest + "') EXECUTE FUNCTION fail_test_initial_entry()");
        try {
            assertThrows(RuntimeException.class, () -> items.create("I3-FAIL-" + UUID.randomUUID(), "Fail item", null,
                    category.id(), secondUnit.id(), BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE,
                    failRequest, "i3-test@example.test"));
        } finally {
            jdbc.execute("DROP TRIGGER fail_test_initial_entry ON inventory_movements");
            jdbc.execute("DROP FUNCTION fail_test_initial_entry()");
        }
        assertTrue(requests.findByRequestId(failRequest).isEmpty());
        assertTrue(itemRepository.findAll().stream().noneMatch(item -> item.sku().startsWith("I3-FAIL-")));
        assertTrue(movements.findByRequestId(failRequest).isEmpty());
    }

    private InventoryItem createTogether(CyclicBarrier start, InventoryCategory category,
                                         UnitOfMeasure unit, String requestId) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        return items.create("I3-RACE-" + requestId, "Concurrent item", null, category.id(), unit.id(),
                BigDecimal.ZERO, new BigDecimal("2.1250"), new BigDecimal("10.000"),
                requestId, "i3-test@example.test");
    }
}