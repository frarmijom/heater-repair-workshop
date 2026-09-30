package com.heaterworkshop.domain.inventory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface UnitOfMeasureRepository {
    List<UnitOfMeasure> findAll();
    Optional<UnitOfMeasure> findById(UUID id);
    Optional<UnitOfMeasure> findByIdForUpdate(UUID id);
    UnitOfMeasure create(UnitOfMeasure value);
    UnitOfMeasure update(UnitOfMeasure value);
}
