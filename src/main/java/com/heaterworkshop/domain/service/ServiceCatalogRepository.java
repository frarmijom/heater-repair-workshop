package com.heaterworkshop.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceCatalogRepository {
    List<ServiceCatalogItem> findAll();
    Optional<ServiceCatalogItem> findById(UUID id);
    Optional<ServiceCatalogItem> findByIdForUpdate(UUID id);
    Optional<ServiceCatalogItem> findByCode(String code);
    ServiceCatalogItem create(ServiceCatalogItem item);
    ServiceCatalogItem save(ServiceCatalogItem item, long expectedVersion);
}
