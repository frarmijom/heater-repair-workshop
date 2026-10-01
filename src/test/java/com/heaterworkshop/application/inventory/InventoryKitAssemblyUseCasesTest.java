package com.heaterworkshop.application.inventory;

import com.heaterworkshop.domain.inventory.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InventoryKitAssemblyUseCasesTest {

    @Test
    void assemblyConsumesBomAndProducesKit() {
        Fixture f = new Fixture("20", "15", "5");

        var result = f.useCases().assemble(
                f.kit.id(), bd("2"), "assembly-1", "tester", "Ensamblaje de prueba");

        assertBd("22", f.items.get(f.kit.id()).stockCurrent());
        assertBd("5", f.items.get(f.component.id()).stockCurrent());
        assertEquals(InventoryItemType.KIT, f.items.get(f.kit.id()).itemType());

        assertEquals(2, result.movements().size());

        InventoryMovement consumption = result.movements().stream()
                .filter(m -> m.type() == InventoryMovementType.KIT_ASSEMBLY_CONSUMPTION)
                .findFirst().orElseThrow();

        InventoryMovement production = result.movements().stream()
                .filter(m -> m.type() == InventoryMovementType.KIT_ASSEMBLY_PRODUCTION)
                .findFirst().orElseThrow();

        assertBd("10", consumption.quantity());
        assertBd("2", production.quantity());
        assertEquals(InventoryMovementDirection.DECREASE, consumption.direction());
        assertEquals(InventoryMovementDirection.INCREASE, production.direction());

        assertEquals("KIT_ASSEMBLY", consumption.referenceType());
        assertEquals("KIT_ASSEMBLY", production.referenceType());
        assertEquals(consumption.referenceId(), production.referenceId());
        assertEquals(result.assemblyId().toString(), production.referenceId());
    }

    @Test
    void insufficientStockRejectsBeforeAnyWrite() {
        Fixture f = new Fixture("20", "9", "5");

        assertThrows(CatalogConflictException.class, () -> f.useCases().assemble(
                f.kit.id(), bd("2"), "assembly-no-stock", "tester", "Sin stock"));

        assertBd("20", f.items.get(f.kit.id()).stockCurrent());
        assertBd("9", f.items.get(f.component.id()).stockCurrent());
        assertEquals(0, f.items.saveStockCalls);
        assertTrue(f.movements.appended.isEmpty());
    }

    @Test
    void standardItemCannotBeAssembled() {
        Fixture f = new Fixture("20", "15", "5");

        assertThrows(CatalogConflictException.class, () -> f.useCases().assemble(
                f.component.id(), BigDecimal.ONE,
                "assembly-standard", "tester", "No es kit"));

        assertEquals(0, f.items.saveStockCalls);
        assertTrue(f.movements.appended.isEmpty());
    }

    @Test
    void kitWithoutBomCannotBeAssembled() {
        Fixture f = new Fixture("20", "15", "5");
        f.components.lines = List.of();

        assertThrows(CatalogConflictException.class, () -> f.useCases().assemble(
                f.kit.id(), BigDecimal.ONE,
                "assembly-empty", "tester", "Sin BOM"));

        assertEquals(0, f.items.saveStockCalls);
        assertTrue(f.movements.appended.isEmpty());
    }

    @Test
    void wholeUnitKitRejectsFractionalAssembly() {
        Fixture f = new Fixture("20", "15", "5");

        assertThrows(IllegalArgumentException.class, () -> f.useCases().assemble(
                f.kit.id(), bd("1.5"),
                "assembly-fraction", "tester", "Cantidad inválida"));

        assertEquals(0, f.items.saveStockCalls);
        assertTrue(f.movements.appended.isEmpty());
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertBd(String expected, BigDecimal actual) {
        assertEquals(0, bd(expected).compareTo(actual));
    }

    private static InventoryItem item(String sku, InventoryItemType type,
                                      UUID unitId, String stock) {
        Instant now = Instant.parse("2026-10-01T12:00:00Z");
        return InventoryItem.restore(
                UUID.randomUUID(), sku, sku, null,
                UUID.randomUUID(), unitId, type,
                bd(stock), bd("0.000"), bd("5.0000"),
                true, 0, now, now);
    }

    private static final class Fixture {
        final UnitOfMeasure unit;
        final InventoryItem kit;
        final InventoryItem component;
        final FakeItemRepository items;
        final FakeKitComponentRepository components;
        final FakeMovementRepository movements;
        final FakeUnitRepository units;

        Fixture(String kitStock, String componentStock, String componentPerKit) {
            unit = UnitOfMeasure.create("Unidad", "un", false);
            kit = item("KIT-TEST", InventoryItemType.KIT, unit.id(), kitStock);
            component = item("COMP-TEST", InventoryItemType.STANDARD, unit.id(), componentStock);

            items = new FakeItemRepository(kit, component);
            components = new FakeKitComponentRepository(List.of(
                    new InventoryKitComponent(
                            kit.id(), component.id(), bd(componentPerKit))));
            movements = new FakeMovementRepository();
            units = new FakeUnitRepository(unit);
        }

        InventoryKitAssemblyUseCases useCases() {
            return new InventoryKitAssemblyUseCases(
                    items, components, movements, units);
        }
    }

    private static final class FakeItemRepository
            implements InventoryItemRepository {

        final Map<UUID, InventoryItem> values = new LinkedHashMap<>();
        int saveStockCalls;

        FakeItemRepository(InventoryItem... seed) {
            for (InventoryItem item : seed) {
                values.put(item.id(), item);
            }
        }

        InventoryItem get(UUID id) {
            return values.get(id);
        }

        @Override
        public List<InventoryItem> findAll() {
            return List.copyOf(values.values());
        }

        @Override
        public Optional<InventoryItem> findById(UUID id) {
            return Optional.ofNullable(values.get(id));
        }

        @Override
        public Optional<InventoryItem> findByIdForUpdate(UUID id) {
            return findById(id);
        }

        @Override
        public InventoryItem create(InventoryItem item) {
            values.put(item.id(), item);
            return item;
        }

        @Override
        public InventoryItem saveStock(InventoryItem item) {
            saveStockCalls++;
            values.put(item.id(), item);
            return item;
        }

        @Override
        public InventoryItem saveMetadata(
                InventoryItem item, long expectedVersion) {
            values.put(item.id(), item);
            return item;
        }
    }

    private static final class FakeKitComponentRepository
            implements InventoryKitComponentRepository {

        List<InventoryKitComponent> lines;

        FakeKitComponentRepository(List<InventoryKitComponent> lines) {
            this.lines = lines;
        }

        @Override
        public List<InventoryKitComponent> findByKitItemId(UUID kitItemId) {
            return lines.stream()
                    .filter(line -> line.kitItemId().equals(kitItemId))
                    .toList();
        }

        @Override
        public void replace(
                UUID kitItemId,
                List<InventoryKitComponent> components) {
            this.lines = List.copyOf(components);
        }
    }

    private static final class FakeMovementRepository
            implements InventoryMovementRepository {

        final List<InventoryMovement> appended = new ArrayList<>();

        @Override
        public Optional<InventoryMovement> findByRequestId(String requestId) {
            return appended.stream()
                    .filter(m -> requestId.equals(m.requestId()))
                    .findFirst();
        }

        @Override
        public Optional<InventoryMovement> findById(UUID id) {
            return appended.stream()
                    .filter(m -> id.equals(m.id()))
                    .findFirst();
        }

        @Override
        public Optional<InventoryMovement> findByReversalOfMovementId(UUID id) {
            return appended.stream()
                    .filter(m -> id.equals(m.reversalOfMovementId()))
                    .findFirst();
        }

        @Override
        public InventoryMovement append(InventoryMovement movement) {
            appended.add(movement);
            return movement;
        }

        @Override
        public boolean existsByUnitId(UUID unitId) {
            return false;
        }

        @Override
        public long countByItemId(UUID itemId) {
            return appended.stream()
                    .filter(m -> itemId.equals(m.itemId()))
                    .count();
        }

        @Override
        public List<InventoryMovement> findByItemId(UUID itemId) {
            return appended.stream()
                    .filter(m -> itemId.equals(m.itemId()))
                    .toList();
        }
    }

    private static final class FakeUnitRepository
            implements UnitOfMeasureRepository {

        final UnitOfMeasure unit;

        FakeUnitRepository(UnitOfMeasure unit) {
            this.unit = unit;
        }

        @Override
        public List<UnitOfMeasure> findAll() {
            return List.of(unit);
        }

        @Override
        public Optional<UnitOfMeasure> findById(UUID id) {
            return unit.id().equals(id)
                    ? Optional.of(unit)
                    : Optional.empty();
        }

        @Override
        public Optional<UnitOfMeasure> findByIdForUpdate(UUID id) {
            return findById(id);
        }

        @Override
        public UnitOfMeasure create(UnitOfMeasure value) {
            return value;
        }

        @Override
        public UnitOfMeasure update(UnitOfMeasure value) {
            return value;
        }
    }
}
