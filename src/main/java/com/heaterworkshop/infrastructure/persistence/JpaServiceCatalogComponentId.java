package com.heaterworkshop.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class JpaServiceCatalogComponentId implements Serializable {
    private UUID serviceId;
    private UUID inventoryItemId;

    public JpaServiceCatalogComponentId() {}

    public JpaServiceCatalogComponentId(UUID serviceId, UUID inventoryItemId) {
        this.serviceId = serviceId;
        this.inventoryItemId = inventoryItemId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof JpaServiceCatalogComponentId that)) return false;
        return Objects.equals(serviceId, that.serviceId)
                && Objects.equals(inventoryItemId, that.inventoryItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, inventoryItemId);
    }
}
