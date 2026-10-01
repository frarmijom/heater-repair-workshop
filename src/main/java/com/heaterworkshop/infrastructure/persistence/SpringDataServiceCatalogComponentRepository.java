package com.heaterworkshop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataServiceCatalogComponentRepository
        extends JpaRepository<JpaServiceCatalogComponentEntity, JpaServiceCatalogComponentId> {
    List<JpaServiceCatalogComponentEntity> findByServiceId(UUID serviceId);
    void deleteByServiceId(UUID serviceId);
}
