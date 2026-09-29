package com.heaterworkshop.application.inventory;
import com.heaterworkshop.domain.inventory.*;
import java.util.*;

public final class UnitOfMeasureUseCases {
    private final UnitOfMeasureRepository repository;
    public UnitOfMeasureUseCases(UnitOfMeasureRepository repository) { this.repository=repository; }
    public List<UnitOfMeasure> list() { return repository.findAll(); }
    public UnitOfMeasure create(String name, String symbol, boolean allowsDecimal) { return repository.create(UnitOfMeasure.create(name, symbol, allowsDecimal)); }
    public UnitOfMeasure edit(UUID id, long expectedVersion, String name, String symbol, Boolean allowsDecimal, Boolean active) {
        var old=repository.findById(id).orElseThrow(CatalogNotFoundException::new);
        if (old.version()!=expectedVersion) throw new CatalogConflictException("El registro cambió. Recarga antes de editar.");
        // I1 has no InventoryItem. When it exists, enforce the assigned-item guard
        // for allowsDecimal here, within the shared assignment/update transaction.
        return repository.update(old.edit(name == null ? old.name() : name, symbol == null ? old.symbol() : symbol, allowsDecimal == null ? old.allowsDecimal() : allowsDecimal, active == null ? old.active() : active));
    }
}
