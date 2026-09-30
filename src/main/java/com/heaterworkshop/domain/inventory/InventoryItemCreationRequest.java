package com.heaterworkshop.domain.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemCreationRequest(String requestId, UUID itemId, String payloadHash, Instant createdAt) {}