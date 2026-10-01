package com.heaterworkshop.domain.service;

import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;

import java.util.List;

public interface WorkOrderEquipmentServiceRepository {

    List<WorkOrderEquipmentService> findByWorkOrderIdAndEquipmentId(
            WorkOrderId workOrderId,
            EquipmentId equipmentId);

    void replace(
            WorkOrderId workOrderId,
            EquipmentId equipmentId,
            List<WorkOrderEquipmentService> services);
}
