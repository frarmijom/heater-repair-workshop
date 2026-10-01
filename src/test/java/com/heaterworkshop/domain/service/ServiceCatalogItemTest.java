package com.heaterworkshop.domain.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ServiceCatalogItemTest {
    @Test void createsNormalizedActiveService() {
        var service = ServiceCatalogItem.create(" mant-001 ", "  Mantención   completa ", " Limpieza general ", new BigDecimal("45000"));
        assertEquals("MANT-001", service.code());
        assertEquals("Mantención completa", service.name());
        assertEquals("Limpieza general", service.description());
        assertEquals(new BigDecimal("45000.0000"), service.price());
        assertTrue(service.active());
        assertEquals(0, service.version());
    }

    @Test void editsWithoutChangingIdentityOrCreationTime() {
        var service = ServiceCatalogItem.create("MANT-001", "Mantención", null, new BigDecimal("30000"));
        var edited = service.edit("MANT-002", "Mantención completa", "Incluye limpieza", new BigDecimal("45000"), false);
        assertEquals(service.id(), edited.id());
        assertEquals(service.createdAt(), edited.createdAt());
        assertFalse(edited.active());
        assertEquals("MANT-002", edited.code());
    }

    @Test void rejectsInvalidCodeNameAndPrice() {
        assertThrows(IllegalArgumentException.class, () -> ServiceCatalogItem.create("mal código", "Servicio", null, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> ServiceCatalogItem.create("SERV-1", "   ", null, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> ServiceCatalogItem.create("SERV-1", "Servicio", null, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> ServiceCatalogItem.create("SERV-1", "Servicio", null, new BigDecimal("1.00001")));
    }
}
