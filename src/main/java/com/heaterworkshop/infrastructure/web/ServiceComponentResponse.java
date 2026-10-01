package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.domain.service.ServiceCatalogComponent;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceComponentResponse(
        UUID inventoryItemId,
        BigDecimal quantity) {

    public static ServiceComponentResponse from(
            ServiceCatalogComponent component) {
        return new ServiceComponentResponse(
                component.inventoryItemId(),
                component.quantity());
    }
}
