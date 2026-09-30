package com.heaterworkshop.domain.inventory;

import java.util.List;
import java.util.UUID;

public interface InventoryKitComponentRepository {
    List<InventoryKitComponent> findByKitItemId(UUID kitItemId);
    void replace(UUID kitItemId, List<InventoryKitComponent> components);
}
