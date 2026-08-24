package com.heaterworkshop.domain.repository;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.valueobject.RepairOrderId;

import java.util.Optional;

public interface RepairOrderRepository {
    void save(RepairOrder order);

    Optional<RepairOrder> findById(RepairOrderId id);
}
