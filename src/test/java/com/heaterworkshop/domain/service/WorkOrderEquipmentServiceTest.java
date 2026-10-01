package com.heaterworkshop.domain.service;

import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkOrderEquipmentServiceTest {

    @Test
    void createsEquipmentServiceAssignment() {
        WorkOrderId workOrderId =
                new WorkOrderId("ORDER-123E4567-E89B-12D3-A456-426614174000");
        EquipmentId equipmentId =
                new EquipmentId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
        UUID serviceId =
                UUID.fromString("123e4567-e89b-12d3-a456-426614174002");

        WorkOrderEquipmentService assignment =
                new WorkOrderEquipmentService(workOrderId, equipmentId, serviceId);

        assertEquals(workOrderId, assignment.workOrderId());
        assertEquals(equipmentId, assignment.equipmentId());
        assertEquals(serviceId, assignment.serviceId());
    }

    @Test
    void rejectsMissingWorkOrderId() {
        assertThrows(
                NullPointerException.class,
                () -> new WorkOrderEquipmentService(
                        null,
                        new EquipmentId(UUID.randomUUID()),
                        UUID.randomUUID()));
    }

    @Test
    void rejectsMissingEquipmentId() {
        assertThrows(
                NullPointerException.class,
                () -> new WorkOrderEquipmentService(
                        new WorkOrderId("ORDER-123E4567-E89B-12D3-A456-426614174000"),
                        null,
                        UUID.randomUUID()));
    }

    @Test
    void rejectsMissingServiceId() {
        assertThrows(
                NullPointerException.class,
                () -> new WorkOrderEquipmentService(
                        new WorkOrderId("ORDER-123E4567-E89B-12D3-A456-426614174000"),
                        new EquipmentId(UUID.randomUUID()),
                        null));
    }
}
