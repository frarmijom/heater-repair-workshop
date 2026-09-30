package com.heaterworkshop.domain.entity;

import com.heaterworkshop.domain.valueobject.EquipmentId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WorkOrderEquipmentTest {
    private static final EquipmentId ID =
            new EquipmentId(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

    @Test
    void createsAndNormalizesEquipment() {
        WorkOrderEquipment equipment = new WorkOrderEquipment(
                ID, "  Junkers  ", "  WR10  ", "  10 L  ", "  SN-123  ",
                "  Cliente entrega equipo completo  ", 1);
        assertEquals(ID, equipment.id());
        assertEquals("Junkers", equipment.brand());
        assertEquals("WR10", equipment.model());
        assertEquals("10 L", equipment.capacity());
        assertEquals("SN-123", equipment.serialNumber());
        assertEquals("Cliente entrega equipo completo", equipment.notes());
        assertEquals(1, equipment.position());
    }

    @Test
    void normalizesMissingOptionalFieldsToNull() {
        WorkOrderEquipment equipment = new WorkOrderEquipment(ID, "Junkers", "WR10", "   ", null, "\t", 1);
        assertNull(equipment.capacity());
        assertNull(equipment.serialNumber());
        assertNull(equipment.notes());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void rejectsMissingBrand(String brand) {
        assertThrows(IllegalArgumentException.class, () ->
                new WorkOrderEquipment(ID, brand, "WR10", null, null, null, 1));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void rejectsMissingModel(String model) {
        assertThrows(IllegalArgumentException.class, () ->
                new WorkOrderEquipment(ID, "Junkers", model, null, null, null, 1));
    }

    @Test
    void rejectsMissingId() {
        assertThrows(NullPointerException.class, () ->
                new WorkOrderEquipment(null, "Junkers", "WR10", null, null, null, 1));
    }

    @Test
    void rejectsInvalidPosition() {
        assertThrows(IllegalArgumentException.class, () ->
                new WorkOrderEquipment(ID, "Junkers", "WR10", null, null, null, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new WorkOrderEquipment(ID, "Junkers", "WR10", null, null, null, -1));
    }
}
