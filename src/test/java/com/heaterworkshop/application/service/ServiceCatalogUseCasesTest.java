package com.heaterworkshop.application.service;

import com.heaterworkshop.domain.inventory.CatalogConflictException;
import com.heaterworkshop.domain.service.ServiceCatalogItem;
import com.heaterworkshop.domain.service.ServiceCatalogRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceCatalogUseCasesTest {
    @Test void createsListsAndEditsService() {
        var repo = new MemoryRepository();
        var useCases = new ServiceCatalogUseCases(repo);
        var created = useCases.create("mant-001", "Mantención", null, new BigDecimal("35000"));
        assertEquals("MANT-001", created.code());
        assertEquals(1, useCases.list().size());

        var edited = useCases.edit(created.id(), created.version(), null, "Mantención completa",
                "Limpieza y revisión", new BigDecimal("45000"), false);
        assertEquals("Mantención completa", edited.name());
        assertFalse(edited.active());
    }

    @Test void rejectsDuplicateCodeAndStaleVersion() {
        var repo = new MemoryRepository();
        var useCases = new ServiceCatalogUseCases(repo);
        var first = useCases.create("MANT-001", "Mantención", null, BigDecimal.TEN);
        assertThrows(CatalogConflictException.class,
                () -> useCases.create("mant-001", "Otra", null, BigDecimal.TEN));
        assertThrows(CatalogConflictException.class,
                () -> useCases.edit(first.id(), 99, null, "Cambio", null, null, null));
    }

    private static final class MemoryRepository implements ServiceCatalogRepository {
        private final List<ServiceCatalogItem> values = new ArrayList<>();
        public List<ServiceCatalogItem> findAll() { return List.copyOf(values); }
        public Optional<ServiceCatalogItem> findById(UUID id) { return values.stream().filter(v -> v.id().equals(id)).findFirst(); }
        public Optional<ServiceCatalogItem> findByIdForUpdate(UUID id) { return findById(id); }
        public Optional<ServiceCatalogItem> findByCode(String code) { return values.stream().filter(v -> v.code().equalsIgnoreCase(code)).findFirst(); }
        public ServiceCatalogItem create(ServiceCatalogItem item) { values.add(item); return item; }
        public ServiceCatalogItem save(ServiceCatalogItem item, long expectedVersion) {
            int index = -1;
            for (int i = 0; i < values.size(); i++) if (values.get(i).id().equals(item.id())) index = i;
            if (index < 0) throw new IllegalStateException();
            ServiceCatalogItem saved = ServiceCatalogItem.restore(item.id(), item.code(), item.name(), item.description(),
                    item.price(), item.active(), expectedVersion + 1, item.createdAt(), item.updatedAt());
            values.set(index, saved);
            return saved;
        }
    }
}
