package com.heaterworkshop.domain.repository;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.valueobject.WorkOrderId;

import java.util.Optional;
import java.util.List;

public interface WorkOrderRepository {
    void save(WorkOrder order);

    Optional<WorkOrder> findById(WorkOrderId id);

    List<WorkOrder> findAllByReceivedAtDescending();
}
