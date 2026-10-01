package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.service.WorkOrderEquipmentService;
import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "work_order_equipment_services")
@IdClass(JpaWorkOrderEquipmentServiceId.class)
public class JpaWorkOrderEquipmentServiceEntity {

    @Id
    @Column(name = "work_order_id", nullable = false, length = 64)
    private String workOrderId;

    @Id
    @Column(name = "equipment_id", nullable = false)
    private UUID equipmentId;

    @Id
    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    protected JpaWorkOrderEquipmentServiceEntity() {}

    public JpaWorkOrderEquipmentServiceEntity(
            WorkOrderEquipmentService value) {
        workOrderId = value.workOrderId().value();
        equipmentId = value.equipmentId().value();
        serviceId = value.serviceId();
    }

    public WorkOrderEquipmentService toDomain() {
        return new WorkOrderEquipmentService(
                new WorkOrderId(workOrderId),
                new EquipmentId(equipmentId),
                serviceId);
    }
}
