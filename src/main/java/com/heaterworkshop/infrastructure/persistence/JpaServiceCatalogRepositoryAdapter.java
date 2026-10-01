package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.inventory.CatalogConflictException;
import com.heaterworkshop.domain.service.ServiceCatalogItem;
import com.heaterworkshop.domain.service.ServiceCatalogRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class JpaServiceCatalogRepositoryAdapter implements ServiceCatalogRepository {
    private final SpringDataServiceCatalogRepository repository;

    public JpaServiceCatalogRepositoryAdapter(SpringDataServiceCatalogRepository repository) {
        this.repository = repository;
    }

    @Override @Transactional(readOnly = true)
    public List<ServiceCatalogItem> findAll() {
        return repository.findAll().stream().map(JpaServiceCatalogItemEntity::toDomain)
                .sorted(Comparator.comparing(ServiceCatalogItem::code)).toList();
    }

    @Override @Transactional(readOnly = true)
    public Optional<ServiceCatalogItem> findById(UUID id) {
        return repository.findById(id).map(JpaServiceCatalogItemEntity::toDomain);
    }

    @Override
    public Optional<ServiceCatalogItem> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(JpaServiceCatalogItemEntity::toDomain);
    }

    @Override @Transactional(readOnly = true)
    public Optional<ServiceCatalogItem> findByCode(String code) {
        return repository.findByCodeNormalized(code).map(JpaServiceCatalogItemEntity::toDomain);
    }

    @Override
    public ServiceCatalogItem create(ServiceCatalogItem item) {
        if (item.version() != 0) throw new IllegalArgumentException("Un servicio nuevo debe comenzar en versión cero.");
        return repository.saveAndFlush(new JpaServiceCatalogItemEntity(item)).toDomain();
    }

    @Override
    public ServiceCatalogItem save(ServiceCatalogItem item, long expectedVersion) {
        JpaServiceCatalogItemEntity entity = repository.findByIdForUpdate(item.id()).orElseThrow();
        if (entity.toDomain().version() != expectedVersion || item.version() != expectedVersion) {
            throw new CatalogConflictException("El servicio cambió. Recarga antes de editar.");
        }
        entity.apply(item);
        return repository.saveAndFlush(entity).toDomain();
    }
}
