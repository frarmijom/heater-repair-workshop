package com.heaterworkshop.infrastructure.persistence;
import com.heaterworkshop.domain.inventory.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Repository @Transactional
public class JpaUnitOfMeasureRepositoryAdapter implements UnitOfMeasureRepository {
    private final SpringDataUnitOfMeasureRepository repository;
    public JpaUnitOfMeasureRepositoryAdapter(SpringDataUnitOfMeasureRepository repository) { this.repository=repository; }
    @Transactional(readOnly=true)
    public List<UnitOfMeasure> findAll() { return repository.findAll().stream().map(JpaUnitOfMeasureEntity::toDomain)
        .sorted(Comparator.comparing((UnitOfMeasure value) -> CatalogText.key(value.name())).thenComparing(UnitOfMeasure::id)).toList(); }
    @Transactional(readOnly=true)
    public Optional<UnitOfMeasure> findById(UUID id) { return repository.findById(id).map(JpaUnitOfMeasureEntity::toDomain); }
    public Optional<UnitOfMeasure> findByIdForUpdate(UUID id) { return repository.findByIdForUpdate(id).map(JpaUnitOfMeasureEntity::toDomain); }
    public UnitOfMeasure create(UnitOfMeasure value) { return repository.saveAndFlush(new JpaUnitOfMeasureEntity(value)).toDomain(); }
    public UnitOfMeasure update(UnitOfMeasure value) {
        var entity=repository.findById(value.id()).orElseThrow(CatalogNotFoundException::new);
        if (entity.toDomain().version()!=value.version()) throw new CatalogConflictException("El registro cambió. Recarga antes de editar.");
        entity.apply(value);
        return repository.saveAndFlush(entity).toDomain();
    }
}
