package com.heaterworkshop.infrastructure.persistence;
import com.heaterworkshop.domain.inventory.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Repository @Transactional
public class JpaInventoryCategoryRepositoryAdapter implements InventoryCategoryRepository {
    private final SpringDataInventoryCategoryRepository repository;
    public JpaInventoryCategoryRepositoryAdapter(SpringDataInventoryCategoryRepository repository) { this.repository=repository; }
    @Transactional(readOnly=true)
    public List<InventoryCategory> findAll() { return repository.findAll().stream().map(JpaInventoryCategoryEntity::toDomain)
        .sorted(Comparator.comparing((InventoryCategory value) -> CatalogText.key(value.name())).thenComparing(InventoryCategory::id)).toList(); }
    @Transactional(readOnly=true)
    public Optional<InventoryCategory> findById(UUID id) { return repository.findById(id).map(JpaInventoryCategoryEntity::toDomain); }
    public InventoryCategory create(InventoryCategory value) { return repository.saveAndFlush(new JpaInventoryCategoryEntity(value)).toDomain(); }
    public InventoryCategory update(InventoryCategory value) {
        var entity=repository.findById(value.id()).orElseThrow(CatalogNotFoundException::new);
        if (entity.toDomain().version()!=value.version()) throw new CatalogConflictException("El registro cambió. Recarga antes de editar.");
        entity.apply(value);
        return repository.saveAndFlush(entity).toDomain();
    }
}
