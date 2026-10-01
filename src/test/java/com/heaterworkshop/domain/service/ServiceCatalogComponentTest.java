package com.heaterworkshop.domain.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceCatalogComponentTest {
    private final UUID serviceId = UUID.randomUUID();
    private final UUID inventoryItemId = UUID.randomUUID();

    @Test
    void acceptsPositiveQuantityAndNormalizesScale() {
        ServiceCatalogComponent component =
                new ServiceCatalogComponent(serviceId, inventoryItemId, new BigDecimal("2.5"));

        assertEquals(new BigDecimal("2.500"), component.quantity());
    }

    @Test
    void rejectsZeroAndNegativeQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceCatalogComponent(serviceId, inventoryItemId, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceCatalogComponent(serviceId, inventoryItemId, new BigDecimal("-1")));
    }

    @Test
    void rejectsMoreThanThreeDecimals() {
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceCatalogComponent(serviceId, inventoryItemId, new BigDecimal("1.0001")));
    }

    @Test
    void requiresIdentifiersAndQuantity() {
        assertThrows(NullPointerException.class,
                () -> new ServiceCatalogComponent(null, inventoryItemId, BigDecimal.ONE));
        assertThrows(NullPointerException.class,
                () -> new ServiceCatalogComponent(serviceId, null, BigDecimal.ONE));
        assertThrows(NullPointerException.class,
                () -> new ServiceCatalogComponent(serviceId, inventoryItemId, null));
    }
}
