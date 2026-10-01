package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.domain.service.ServiceCatalogItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ServiceCatalogResponse(
        UUID id,
        String code,
        String name,
        String description,
        BigDecimal price,
        boolean active,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public static ServiceCatalogResponse from(ServiceCatalogItem item) {
        return new ServiceCatalogResponse(
                item.id(),
                item.code(),
                item.name(),
                item.description(),
                item.price(),
                item.active(),
                item.version(),
                item.createdAt(),
                item.updatedAt());
    }
}
