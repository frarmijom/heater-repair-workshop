package com.heaterworkshop.domain.inventory;

import java.util.Optional;
import java.util.UUID;

public interface InventoryMovementRepository {
    Optional<InventoryMovement> findByRequestId(String requestId);
    Optional<InventoryMovement> findById(UUID id);
    Optional<InventoryMovement> findByReversalOfMovementId(UUID id);
    InventoryMovement append(InventoryMovement movement);
    boolean existsByUnitId(UUID unitId);
    long countByItemId(UUID itemId);
}