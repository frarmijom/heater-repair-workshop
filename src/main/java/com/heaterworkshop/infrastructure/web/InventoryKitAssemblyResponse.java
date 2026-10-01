package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.application.inventory.InventoryKitAssemblyUseCases;
import java.util.List;
import java.util.UUID;

public record InventoryKitAssemblyResponse(
        UUID assemblyId,
        InventoryItemResponse kit,
        List<InventoryMovementResponse> movements) {

    public static InventoryKitAssemblyResponse from(
            InventoryKitAssemblyUseCases.AssemblyResult result,
            InventoryItemResponse kit) {
        return new InventoryKitAssemblyResponse(
                result.assemblyId(),
                kit,
                result.movements().stream()
                        .map(InventoryMovementResponse::from)
                        .toList());
    }
}
