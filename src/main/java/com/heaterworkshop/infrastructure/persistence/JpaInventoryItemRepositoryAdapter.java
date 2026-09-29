package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.inventory.InventoryItem;
import com.heaterworkshop.domain.inventory.InventoryItemRepository;
import com.heaterworkshop.domain.inventory.CatalogConflictException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public class JpaInventoryItemRepositoryAdapter implements InventoryItemRepository {
    private final SpringDataInventoryItemRepository repository;

    public JpaInventoryItemRepositoryAdapter(SpringDataInventoryItemRepository repository) {
        this.repository = repository;
    }

    @Override @Transactional(readOnly = true)
    public List<InventoryItem> findAll() {
        return repository.findAll().stream().map(JpaInventoryItemEntity::toDomain)
                .sorted(java.util.Comparator.comparing(InventoryItem::sku)).toList();
    }

    @Override @Transactional(readOnly = true)
    public Optional<InventoryItem> findById(UUID id) {
        return repository.findById(id).map(JpaInventoryItemEntity::toDomain);
    }

    @Override
    public Optional<InventoryItem> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(JpaInventoryItemEntity::toDomain);
    }

    @Override
    public InventoryItem create(InventoryItem item) {
        if (item.stockCurrent().signum() != 0 || item.version() != 0)
            throw new IllegalArgumentException("Un artículo nuevo debe comenzar con stock cero.");
        return repository.saveAndFlush(new JpaInventoryItemEntity(item)).toDomain();
    }

    @Override
    public InventoryItem saveStock(InventoryItem item) {
        JpaInventoryItemEntity entity = repository.findByIdForUpdate(item.id()).orElseThrow();
        if (entity.toDomain().version() + 1 != item.version()) {
            throw new CatalogConflictException("El artículo cambió durante la operación.");
        }
        entity.applyStock(item);
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    public InventoryItem saveMetadata(InventoryItem item, long expectedVersion) {
        JpaInventoryItemEntity entity = repository.findByIdForUpdate(item.id()).orElseThrow();
        if (entity.toDomain().version() != expectedVersion || item.version() != expectedVersion)
            throw new CatalogConflictException("El artículo cambió. Recarga antes de editar.");
        entity.applyMetadata(item);
        InventoryItem saved = repository.saveAndFlush(entity).toDomain();
        return saved;
    }
}