package com.heaterworkshop.domain.inventory;

import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository {
    Optional<InventoryItem> findById(UUID id);
    Optional<InventoryItem> findByIdForUpdate(UUID id);
    InventoryItem create(InventoryItem item);
    InventoryItem saveStock(InventoryItem item);
}