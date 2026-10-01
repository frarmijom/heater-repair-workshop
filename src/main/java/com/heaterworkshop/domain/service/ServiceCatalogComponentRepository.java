package com.heaterworkshop.domain.service;

import java.util.List;
import java.util.UUID;

public interface ServiceCatalogComponentRepository {
    List<ServiceCatalogComponent> findByServiceId(UUID serviceId);
    void replace(UUID serviceId, List<ServiceCatalogComponent> components);
}
