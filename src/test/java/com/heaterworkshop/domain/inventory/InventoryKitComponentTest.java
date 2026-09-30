package com.heaterworkshop.domain.inventory;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class InventoryKitComponentTest {
    @Test void acceptsPositiveComponentQuantity() {
        var component=new InventoryKitComponent(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("2.000"));
        assertEquals(new BigDecimal("2.000"), component.quantity());
    }
    @Test void rejectsSelfReferenceZeroNegativeAndExcessScale() {
        UUID id=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new InventoryKitComponent(id,id,BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new InventoryKitComponent(UUID.randomUUID(),UUID.randomUUID(),BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new InventoryKitComponent(UUID.randomUUID(),UUID.randomUUID(),new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> new InventoryKitComponent(UUID.randomUUID(),UUID.randomUUID(),new BigDecimal("1.0001")));
    }
}
