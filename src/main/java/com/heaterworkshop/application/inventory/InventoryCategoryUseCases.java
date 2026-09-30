package com.heaterworkshop.application.inventory;
import com.heaterworkshop.domain.inventory.*;
import java.util.*;

public final class InventoryCategoryUseCases {
    private final InventoryCategoryRepository repository;
    public InventoryCategoryUseCases(InventoryCategoryRepository repository) { this.repository=repository; }
    public List<InventoryCategory> list() { return repository.findAll(); }
    public InventoryCategory create(String name) { return repository.create(InventoryCategory.create(name)); }
    public InventoryCategory edit(UUID id, long expectedVersion, String name, Boolean active) {
        var old=repository.findById(id).orElseThrow(CatalogNotFoundException::new);
        if (old.version()!=expectedVersion) throw new CatalogConflictException("El registro cambió. Recarga antes de editar.");
        return repository.update(old.edit(name == null ? old.name() : name, active == null ? old.active() : active));
    }
}
