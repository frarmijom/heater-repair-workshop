package com.heaterworkshop.application.inventory;

import com.heaterworkshop.domain.inventory.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InventoryAdjustmentReversalUseCasesTest {

    @Test
    void positiveAdjustmentIncreasesStock() {
        Fixture f = new Fixture(item(true, "10"));
        var useCases = new InventoryAdjustmentUseCases(f.recorder);

        InventoryMovement movement = useCases.adjust(f.item.id(), InventoryMovementDirection.INCREASE, bd("3"),
                "req-adjust-plus", "tester", "Conteo físico");

        assertEquals(InventoryMovementType.ADJUSTMENT, movement.type());
        assertEquals(InventoryMovementDirection.INCREASE, movement.direction());
        assertBd("10", movement.stockBefore());
        assertBd("13", movement.stockAfter());
        assertBd("13", f.items.current.stockCurrent());
        assertEquals(1, f.movements.appended.size());
    }

    @Test
    void negativeAdjustmentDecreasesStock() {
        Fixture f = new Fixture(item(true, "10"));
        var useCases = new InventoryAdjustmentUseCases(f.recorder);

        InventoryMovement movement = useCases.adjust(f.item.id(), InventoryMovementDirection.DECREASE, bd("4"),
                "req-adjust-minus", "tester", "Corrección de inventario");

        assertBd("6", movement.stockAfter());
        assertBd("6", f.items.current.stockCurrent());
    }

    @Test
    void negativeAdjustmentCannotLeaveNegativeStock() {
        Fixture f = new Fixture(item(true, "2"));
        var useCases = new InventoryAdjustmentUseCases(f.recorder);

        assertThrows(IllegalArgumentException.class, () -> useCases.adjust(
                f.item.id(), InventoryMovementDirection.DECREASE, bd("3"),
                "req-negative", "tester", "Conteo físico"));

        assertBd("2", f.items.current.stockCurrent());
        assertTrue(f.movements.appended.isEmpty());
    }

    @Test
    void inactiveItemRejectsAdjustment() {
        Fixture f = new Fixture(item(false, "10"));
        var useCases = new InventoryAdjustmentUseCases(f.recorder);

        assertThrows(CatalogConflictException.class, () -> useCases.adjust(
                f.item.id(), InventoryMovementDirection.INCREASE, bd("1"),
                "req-inactive", "tester", "Conteo físico"));

        assertTrue(f.movements.appended.isEmpty());
    }

    @Test
    void adjustmentRequiresReason() {
        Fixture f = new Fixture(item(true, "10"));
        var useCases = new InventoryAdjustmentUseCases(f.recorder);

        assertThrows(IllegalArgumentException.class, () -> useCases.adjust(
                f.item.id(), InventoryMovementDirection.INCREASE, bd("1"),
                "req-reason", "tester", "   "));
    }

    @Test
    void retryWithSameRequestIdIsIdempotent() {
        Fixture f = new Fixture(item(true, "10"));
        var useCases = new InventoryAdjustmentUseCases(f.recorder);

        InventoryMovement first = useCases.adjust(f.item.id(), InventoryMovementDirection.INCREASE, bd("2"),
                "req-idempotent", "tester", "Conteo físico");
        InventoryMovement second = useCases.adjust(f.item.id(), InventoryMovementDirection.INCREASE, bd("2"),
                "req-idempotent", "tester", "Conteo físico");

        assertEquals(first.id(), second.id());
        assertBd("12", f.items.current.stockCurrent());
        assertEquals(1, f.movements.appended.size());
    }

    @Test
    void reversesEntryUsingOppositeDirectionAndOriginalQuantity() {
        Fixture f = new Fixture(item(true, "15"));
        InventoryMovement original = movement(f, InventoryMovementType.ENTRY, InventoryMovementDirection.INCREASE,
                "5", "10", "15", "entry-1", null);
        f.movements.seed(original);

        var useCases = new InventoryReversalUseCases(f.movements, f.recorder);
        InventoryMovement reversal = useCases.reverse(
                original.id(), "req-reversal-entry", "tester", "Entrada incorrecta");

        assertEquals(InventoryMovementType.REVERSAL, reversal.type());
        assertEquals(InventoryMovementDirection.DECREASE, reversal.direction());
        assertEquals(0, original.quantity().compareTo(reversal.quantity()));
        assertEquals(original.id(), reversal.reversalOfMovementId());
        assertBd("10", f.items.current.stockCurrent());
    }

    @Test
    void reversesAdjustment() {
        Fixture f = new Fixture(item(true, "7"));
        InventoryMovement original = movement(f, InventoryMovementType.ADJUSTMENT, InventoryMovementDirection.DECREASE,
                "3", "10", "7", "adjust-1", null);
        f.movements.seed(original);

        var useCases = new InventoryReversalUseCases(f.movements, f.recorder);
        InventoryMovement reversal = useCases.reverse(
                original.id(), "req-reversal-adjust", "tester", "Ajuste equivocado");

        assertEquals(InventoryMovementDirection.INCREASE, reversal.direction());
        assertBd("10", reversal.stockAfter());
        assertBd("10", f.items.current.stockCurrent());
    }

    @Test
    void rejectsReversalOfNonReversibleMovement() {
        Fixture f = new Fixture(item(true, "8"));
        InventoryMovement original = movement(f, InventoryMovementType.WORK_ORDER_CONSUMPTION,
                InventoryMovementDirection.DECREASE, "2", "10", "8", "wo-1", null);
        f.movements.seed(original);

        var useCases = new InventoryReversalUseCases(f.movements, f.recorder);
        assertThrows(CatalogConflictException.class, () -> useCases.reverse(
                original.id(), "req-reversal-wo", "tester", "Intento inválido"));
    }

    @Test
    void rejectsSecondReversalOfSameMovement() {
        Fixture f = new Fixture(item(true, "15"));
        InventoryMovement original = movement(f, InventoryMovementType.ENTRY, InventoryMovementDirection.INCREASE,
                "5", "10", "15", "entry-2", null);
        f.movements.seed(original);

        var useCases = new InventoryReversalUseCases(f.movements, f.recorder);
        useCases.reverse(original.id(), "req-first-reversal", "tester", "Primera reversión");

        assertThrows(CatalogConflictException.class, () -> useCases.reverse(
                original.id(), "req-second-reversal", "tester", "Segunda reversión"));
    }

    private static BigDecimal bd(String value) { return new BigDecimal(value); }

    private static void assertBd(String expected, BigDecimal actual) {
        assertEquals(0, bd(expected).compareTo(actual));
    }

    private static InventoryItem item(boolean active, String stock) {
        Instant now = Instant.parse("2026-09-29T12:00:00Z");
        return InventoryItem.restore(UUID.randomUUID(), "SKU-I5", "Item I5", null,
                UUID.randomUUID(), UUID.randomUUID(), bd(stock), bd("0.000"), bd("5.0000"),
                active, 0, now, now);
    }

    private static InventoryMovement movement(Fixture f, InventoryMovementType type,
                                              InventoryMovementDirection direction, String quantity,
                                              String before, String after, String requestId, UUID reversalOf) {
        String workOrderId = type == InventoryMovementType.WORK_ORDER_CONSUMPTION ? "WO-TEST" : null;
        return InventoryMovement.restore(UUID.randomUUID(), f.item.id(), type, direction,
                bd(quantity), bd(before), bd(after), bd("5.0000"), requestId,
                Instant.parse("2026-09-29T12:00:00Z"), "tester", "seed", null, null,
                workOrderId, reversalOf, f.item.sku(), f.item.name(), f.unit.name(), f.unit.symbol());
    }

    private static final class Fixture {
        final InventoryItem item;
        final UnitOfMeasure unit;
        final FakeItemRepository items;
        final FakeMovementRepository movements = new FakeMovementRepository();
        final FakeUnitRepository units;
        final RecordInventoryMovementUseCase recorder;

        Fixture(InventoryItem item) {
            this.item = item;
            this.unit = new UnitOfMeasure(item.unitId(), "Unidad", "un", false, true, 0,
                    Instant.parse("2026-09-29T12:00:00Z"), Instant.parse("2026-09-29T12:00:00Z"));
            this.items = new FakeItemRepository(item);
            this.units = new FakeUnitRepository(unit);
            this.recorder = new RecordInventoryMovementUseCase(items, movements, units);
        }
    }

    private static final class FakeItemRepository implements InventoryItemRepository {
        InventoryItem current;
        FakeItemRepository(InventoryItem current) { this.current = current; }

        @Override public List<InventoryItem> findAll() { return List.of(current); }
        @Override public Optional<InventoryItem> findById(UUID id) {
            return current.id().equals(id) ? Optional.of(current) : Optional.empty();
        }
        @Override public Optional<InventoryItem> findByIdForUpdate(UUID id) { return findById(id); }
        @Override public InventoryItem create(InventoryItem item) { current = item; return item; }
        @Override public InventoryItem saveStock(InventoryItem item) { current = item; return item; }
        @Override public InventoryItem saveMetadata(InventoryItem item, long expectedVersion) {
            current = item; return item;
        }
    }

    private static final class FakeMovementRepository implements InventoryMovementRepository {
        final List<InventoryMovement> appended = new ArrayList<>();
        final Map<UUID, InventoryMovement> byId = new LinkedHashMap<>();
        final Map<String, InventoryMovement> byRequest = new HashMap<>();

        void seed(InventoryMovement movement) {
            byId.put(movement.id(), movement);
            byRequest.put(movement.requestId(), movement);
        }

        @Override public Optional<InventoryMovement> findByRequestId(String requestId) {
            return Optional.ofNullable(byRequest.get(requestId));
        }
        @Override public Optional<InventoryMovement> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }
        @Override public Optional<InventoryMovement> findByReversalOfMovementId(UUID id) {
            return byId.values().stream().filter(m -> id.equals(m.reversalOfMovementId())).findFirst();
        }
        @Override public InventoryMovement append(InventoryMovement movement) {
            appended.add(movement);
            seed(movement);
            return movement;
        }
        @Override public boolean existsByUnitId(UUID unitId) { return false; }
        @Override public long countByItemId(UUID itemId) {
            return byId.values().stream().filter(m -> itemId.equals(m.itemId())).count();
        }
        @Override public List<InventoryMovement> findByItemId(UUID itemId) {
            return byId.values().stream().filter(m -> itemId.equals(m.itemId())).toList();
        }
    }

    private static final class FakeUnitRepository implements UnitOfMeasureRepository {
        UnitOfMeasure current;
        FakeUnitRepository(UnitOfMeasure current) { this.current = current; }

        @Override public List<UnitOfMeasure> findAll() { return List.of(current); }
        @Override public Optional<UnitOfMeasure> findById(UUID id) {
            return current.id().equals(id) ? Optional.of(current) : Optional.empty();
        }
        @Override public Optional<UnitOfMeasure> findByIdForUpdate(UUID id) { return findById(id); }
        @Override public UnitOfMeasure create(UnitOfMeasure value) { current = value; return value; }
        @Override public UnitOfMeasure update(UnitOfMeasure value) { current = value; return value; }
    }
}
