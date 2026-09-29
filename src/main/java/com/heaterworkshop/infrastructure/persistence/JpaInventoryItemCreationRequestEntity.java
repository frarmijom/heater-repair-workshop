package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.inventory.InventoryItemCreationRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_item_creation_requests")
public class JpaInventoryItemCreationRequestEntity {
    @Id @Column(name = "request_id", length = 128) private String requestId;
    @Column(name = "item_id", nullable = false, unique = true) private UUID itemId;
    @Column(name = "payload_hash", nullable = false, length = 64) private String payloadHash;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected JpaInventoryItemCreationRequestEntity() {}

    public InventoryItemCreationRequest toDomain() {
        return new InventoryItemCreationRequest(requestId, itemId, payloadHash, createdAt);
    }
}