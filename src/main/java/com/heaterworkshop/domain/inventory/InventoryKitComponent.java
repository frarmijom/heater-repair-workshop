package com.heaterworkshop.domain.inventory;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record InventoryKitComponent(UUID kitItemId, UUID componentItemId, BigDecimal quantity) {
    public InventoryKitComponent {
        Objects.requireNonNull(kitItemId); Objects.requireNonNull(componentItemId);
        if (kitItemId.equals(componentItemId)) throw new IllegalArgumentException("Un kit no puede contenerse a sí mismo.");
        quantity = InventoryQuantity.exact(quantity, 3, "quantity");
        if (quantity.signum() <= 0) throw new IllegalArgumentException("quantity debe ser mayor que cero.");
    }
}
