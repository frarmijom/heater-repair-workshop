package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.repository.RepairOrderRepository;
import com.heaterworkshop.domain.valueobject.RepairOrderId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryRepairOrderRepository implements RepairOrderRepository {

    private final Map<RepairOrderId, RepairOrder> orders = new HashMap<>();

    @Override
    public void save(RepairOrder order) {
        orders.put(order.id(), order);
    }

    @Override
    public Optional<RepairOrder> findById(RepairOrderId id) {
        return Optional.ofNullable(orders.get(id));
    }
}
