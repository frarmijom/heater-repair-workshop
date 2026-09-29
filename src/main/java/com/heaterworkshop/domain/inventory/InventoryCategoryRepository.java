package com.heaterworkshop.domain.inventory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface InventoryCategoryRepository {
    List<InventoryCategory> findAll();
    Optional<InventoryCategory> findById(UUID id);
    InventoryCategory create(InventoryCategory value);
    InventoryCategory update(InventoryCategory value);
}
