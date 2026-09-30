package com.heaterworkshop.domain.inventory;

import java.util.Optional;
import java.util.UUID;

public interface InventoryItemCreationRequestRepository {
    boolean claim(String requestId, UUID itemId, String payloadHash);
    Optional<InventoryItemCreationRequest> findByRequestId(String requestId);
}