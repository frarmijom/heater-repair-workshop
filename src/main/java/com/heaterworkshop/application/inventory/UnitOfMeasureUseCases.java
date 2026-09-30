package com.heaterworkshop.application.inventory;
import com.heaterworkshop.domain.inventory.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

public class UnitOfMeasureUseCases {
    private final UnitOfMeasureRepository repository;
    private final InventoryMovementRepository movements;
    public UnitOfMeasureUseCases(UnitOfMeasureRepository repository, InventoryMovementRepository movements) {
        this.repository=repository; this.movements=movements;
    }
    public List<UnitOfMeasure> list() { return repository.findAll(); }
    public UnitOfMeasure create(String name, String symbol, boolean allowsDecimal) { return repository.create(UnitOfMeasure.create(name, symbol, allowsDecimal)); }
    @Transactional
    public UnitOfMeasure edit(UUID id, long expectedVersion, String name, String symbol, Boolean allowsDecimal, Boolean active) {
        var old=repository.findByIdForUpdate(id).orElseThrow(CatalogNotFoundException::new);
        if (old.version()!=expectedVersion) throw new CatalogConflictException("El registro cambió. Recarga antes de editar.");
        if (allowsDecimal != null && allowsDecimal != old.allowsDecimal() && movements.existsByUnitId(id))
            throw new CatalogConflictException("No se puede reinterpretar una unidad con movimientos históricos.");
        return repository.update(old.edit(name == null ? old.name() : name, symbol == null ? old.symbol() : symbol, allowsDecimal == null ? old.allowsDecimal() : allowsDecimal, active == null ? old.active() : active));
    }
}
