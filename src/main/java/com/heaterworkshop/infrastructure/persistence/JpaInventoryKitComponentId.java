package com.heaterworkshop.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class JpaInventoryKitComponentId implements Serializable {
    private UUID kitItemId;
    private UUID componentItemId;
    protected JpaInventoryKitComponentId() {}
    public JpaInventoryKitComponentId(UUID kitItemId, UUID componentItemId) { this.kitItemId=kitItemId; this.componentItemId=componentItemId; }
    @Override public boolean equals(Object o) { return o instanceof JpaInventoryKitComponentId other && Objects.equals(kitItemId, other.kitItemId) && Objects.equals(componentItemId, other.componentItemId); }
    @Override public int hashCode() { return Objects.hash(kitItemId, componentItemId); }
}
