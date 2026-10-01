package com.heaterworkshop.application.service;

import com.heaterworkshop.domain.inventory.CatalogNotFoundException;
import com.heaterworkshop.domain.inventory.InventoryItem;
import com.heaterworkshop.domain.inventory.InventoryItemRepository;
import com.heaterworkshop.domain.inventory.InventoryItemType;
import com.heaterworkshop.domain.service.ServiceCatalogComponent;
import com.heaterworkshop.domain.service.ServiceCatalogComponentRepository;
import com.heaterworkshop.domain.service.ServiceCatalogItem;
import com.heaterworkshop.domain.service.ServiceCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceCatalogCompositionUseCasesTest {
    private UUID serviceId;
    private UUID standardId;
    private UUID kitId;
    private FakeServiceRepository services;
    private FakeComponentRepository components;
    private FakeInventoryRepository items;
    private ServiceCatalogCompositionUseCases useCases;

    @BeforeEach
    void setUp() {
        serviceId = UUID.randomUUID();
        standardId = UUID.randomUUID();
        kitId = UUID.randomUUID();

        services = new FakeServiceRepository();
        components = new FakeComponentRepository();
        items = new FakeInventoryRepository();

        services.values.put(serviceId, service(serviceId, true));
        items.values.put(standardId, item(standardId, InventoryItemType.STANDARD, true));
        items.values.put(kitId, item(kitId, InventoryItemType.KIT, true));

        useCases = new ServiceCatalogCompositionUseCases(services, components, items);
    }

    @Test
    void replacesCompositionWithStandardAndKitItems() {
        List<ServiceCatalogComponent> result = useCases.replaceComposition(serviceId, List.of(
                new ServiceCatalogCompositionUseCases.ComponentInput(standardId, new BigDecimal("2")),
                new ServiceCatalogCompositionUseCases.ComponentInput(kitId, new BigDecimal("1"))
        ));

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("2.000"), result.get(0).quantity());
        assertEquals(new BigDecimal("1.000"), result.get(1).quantity());
        assertEquals(2, components.values.size());
    }

    @Test
    void returnsCurrentComposition() {
        components.values = List.of(
                new ServiceCatalogComponent(serviceId, standardId, BigDecimal.ONE));

        assertEquals(components.values, useCases.getComposition(serviceId));
    }

    @Test
    void rejectsMissingOrInactiveService() {
        assertThrows(CatalogNotFoundException.class,
                () -> useCases.getComposition(UUID.randomUUID()));

        services.values.put(serviceId, service(serviceId, false));
        assertThrows(IllegalArgumentException.class,
                () -> useCases.replaceComposition(serviceId, List.of()));
    }

    @Test
    void rejectsMissingOrInactiveInventoryItem() {
        UUID missing = UUID.randomUUID();
        assertThrows(CatalogNotFoundException.class,
                () -> useCases.replaceComposition(serviceId, List.of(
                        new ServiceCatalogCompositionUseCases.ComponentInput(missing, BigDecimal.ONE))));

        items.values.put(standardId, item(standardId, InventoryItemType.STANDARD, false));
        assertThrows(IllegalArgumentException.class,
                () -> useCases.replaceComposition(serviceId, List.of(
                        new ServiceCatalogCompositionUseCases.ComponentInput(standardId, BigDecimal.ONE))));
    }

    @Test
    void rejectsDuplicateInventoryItem() {
        assertThrows(IllegalArgumentException.class,
                () -> useCases.replaceComposition(serviceId, List.of(
                        new ServiceCatalogCompositionUseCases.ComponentInput(standardId, BigDecimal.ONE),
                        new ServiceCatalogCompositionUseCases.ComponentInput(standardId, new BigDecimal("2")))));
    }

    @Test
    void rejectsInvalidQuantityBeforePersisting() {
        assertThrows(IllegalArgumentException.class,
                () -> useCases.replaceComposition(serviceId, List.of(
                        new ServiceCatalogCompositionUseCases.ComponentInput(standardId, BigDecimal.ZERO))));
        assertTrue(components.values.isEmpty());
    }

    @Test
    void allowsClearingComposition() {
        components.values = new ArrayList<>(List.of(
                new ServiceCatalogComponent(serviceId, standardId, BigDecimal.ONE)));

        List<ServiceCatalogComponent> result = useCases.replaceComposition(serviceId, List.of());

        assertTrue(result.isEmpty());
        assertTrue(components.values.isEmpty());
    }

    private static ServiceCatalogItem service(UUID id, boolean active) {
        Instant now = Instant.parse("2026-10-01T12:00:00Z");
        return ServiceCatalogItem.restore(
                id, "SERV-001", "Mantención", null,
                new BigDecimal("45000"), active, 0, now, now);
    }

    private static InventoryItem item(UUID id, InventoryItemType type, boolean active) {
        Instant now = Instant.parse("2026-10-01T12:00:00Z");
        return InventoryItem.restore(
                id, "ITEM-" + id.toString().substring(0, 8).toUpperCase(),
                "Artículo", null, UUID.randomUUID(), UUID.randomUUID(), type,
                new BigDecimal("10.000"), BigDecimal.ZERO,
                new BigDecimal("1000.0000"), active, 0, now, now);
    }

    private static final class FakeServiceRepository implements ServiceCatalogRepository {
        private final Map<UUID, ServiceCatalogItem> values = new HashMap<>();
        public List<ServiceCatalogItem> findAll() { return new ArrayList<>(values.values()); }
        public Optional<ServiceCatalogItem> findById(UUID id) { return Optional.ofNullable(values.get(id)); }
        public Optional<ServiceCatalogItem> findByIdForUpdate(UUID id) { return findById(id); }
        public Optional<ServiceCatalogItem> findByCode(String code) {
            return values.values().stream().filter(v -> v.code().equalsIgnoreCase(code)).findFirst();
        }
        public ServiceCatalogItem create(ServiceCatalogItem item) { values.put(item.id(), item); return item; }
        public ServiceCatalogItem save(ServiceCatalogItem item, long expectedVersion) {
            values.put(item.id(), item); return item;
        }
    }

    private static final class FakeComponentRepository implements ServiceCatalogComponentRepository {
        private List<ServiceCatalogComponent> values = new ArrayList<>();

        @Override
        public List<ServiceCatalogComponent> findByServiceId(UUID serviceId) {
            return values.stream().filter(value -> value.serviceId().equals(serviceId)).toList();
        }

        @Override
        public void replace(UUID serviceId, List<ServiceCatalogComponent> components) {
            values = new ArrayList<>(components);
        }
    }

    private static final class FakeInventoryRepository implements InventoryItemRepository {
        private final Map<UUID, InventoryItem> values = new HashMap<>();
        public List<InventoryItem> findAll() { return new ArrayList<>(values.values()); }
        public Optional<InventoryItem> findById(UUID id) { return Optional.ofNullable(values.get(id)); }
        public Optional<InventoryItem> findByIdForUpdate(UUID id) { return findById(id); }
        public InventoryItem create(InventoryItem item) { values.put(item.id(), item); return item; }
        public InventoryItem saveStock(InventoryItem item) { values.put(item.id(), item); return item; }
        public InventoryItem saveMetadata(InventoryItem item, long expectedVersion) {
            values.put(item.id(), item); return item;
        }
    }

}
