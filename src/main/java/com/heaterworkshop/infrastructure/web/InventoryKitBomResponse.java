package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.application.inventory.InventoryKitUseCases;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InventoryKitBomResponse(UUID kitItemId, List<Component> components) {
    public record Component(UUID componentItemId, BigDecimal quantity) {}

    public static InventoryKitBomResponse from(InventoryKitUseCases.KitBom bom) {
        return new InventoryKitBomResponse(bom.kit().id(), bom.components().stream()
                .map(component -> new Component(component.componentItemId(), component.quantity())).toList());
    }
}
