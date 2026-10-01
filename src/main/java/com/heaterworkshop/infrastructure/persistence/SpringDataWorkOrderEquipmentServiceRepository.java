package com.heaterworkshop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataWorkOrderEquipmentServiceRepository
        extends JpaRepository<
                JpaWorkOrderEquipmentServiceEntity,
                JpaWorkOrderEquipmentServiceId> {

    List<JpaWorkOrderEquipmentServiceEntity>
    findByWorkOrderIdAndEquipmentId(String workOrderId, UUID equipmentId);

    void deleteByWorkOrderIdAndEquipmentId(String workOrderId, UUID equipmentId);
}
