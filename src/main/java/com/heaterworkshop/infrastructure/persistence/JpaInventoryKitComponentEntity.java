package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.inventory.InventoryKitComponent;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name="inventory_kit_components")
@IdClass(JpaInventoryKitComponentId.class)
public class JpaInventoryKitComponentEntity {
    @Id @Column(name="kit_item_id", nullable=false) private UUID kitItemId;
    @Id @Column(name="component_item_id", nullable=false) private UUID componentItemId;
    @Column(nullable=false, precision=19, scale=3) private BigDecimal quantity;
    protected JpaInventoryKitComponentEntity() {}
    public JpaInventoryKitComponentEntity(InventoryKitComponent value) { kitItemId=value.kitItemId(); componentItemId=value.componentItemId(); quantity=value.quantity(); }
    public InventoryKitComponent toDomain() { return new InventoryKitComponent(kitItemId, componentItemId, quantity); }
}
