package com.heaterworkshop.domain.inventory;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface InventoryItemRepository {
    List<InventoryItem> findAll();
    Optional<InventoryItem> findById(UUID id);
    Optional<InventoryItem> findByIdForUpdate(UUID id);
    InventoryItem create(InventoryItem item);
    InventoryItem saveStock(InventoryItem item);
    InventoryItem saveMetadata(InventoryItem item, long expectedVersion);
}