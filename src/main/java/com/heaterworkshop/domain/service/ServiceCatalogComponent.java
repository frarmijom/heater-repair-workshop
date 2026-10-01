package com.heaterworkshop.domain.service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record ServiceCatalogComponent(UUID serviceId, UUID inventoryItemId, BigDecimal quantity) {
    public ServiceCatalogComponent {
        Objects.requireNonNull(serviceId);
        Objects.requireNonNull(inventoryItemId);
        Objects.requireNonNull(quantity);
        if (quantity.scale() > 3) {
            throw new IllegalArgumentException("quantity admite máximo 3 decimales.");
        }
        quantity = quantity.setScale(3);
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity debe ser mayor que cero.");
        }
    }
}
