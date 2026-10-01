package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.service.ServiceCatalogComponent;
import com.heaterworkshop.domain.service.ServiceCatalogComponentRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public class JpaServiceCatalogComponentRepositoryAdapter implements ServiceCatalogComponentRepository {
    private final SpringDataServiceCatalogComponentRepository repository;

    public JpaServiceCatalogComponentRepositoryAdapter(
            SpringDataServiceCatalogComponentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceCatalogComponent> findByServiceId(UUID serviceId) {
        return repository.findByServiceId(serviceId).stream()
                .map(JpaServiceCatalogComponentEntity::toDomain)
                .toList();
    }

    @Override
    public void replace(UUID serviceId, List<ServiceCatalogComponent> components) {
        repository.deleteByServiceId(serviceId);
        repository.flush();
        repository.saveAllAndFlush(
                components.stream().map(JpaServiceCatalogComponentEntity::new).toList());
    }
}
