package com.heaterworkshop.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class JpaWorkOrderEquipmentServiceId implements Serializable {

    private String workOrderId;
    private UUID equipmentId;
    private UUID serviceId;

    public JpaWorkOrderEquipmentServiceId() {}

    public JpaWorkOrderEquipmentServiceId(
            String workOrderId,
            UUID equipmentId,
            UUID serviceId) {
        this.workOrderId = workOrderId;
        this.equipmentId = equipmentId;
        this.serviceId = serviceId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof JpaWorkOrderEquipmentServiceId that)) return false;
        return Objects.equals(workOrderId, that.workOrderId)
                && Objects.equals(equipmentId, that.equipmentId)
                && Objects.equals(serviceId, that.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workOrderId, equipmentId, serviceId);
    }
}
