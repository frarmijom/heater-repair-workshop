package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import com.heaterworkshop.domain.valueobject.WorkOrderId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Comparator;

public final class InMemoryWorkOrderRepository implements WorkOrderRepository {

    private final Map<WorkOrderId, WorkOrder> orders = new HashMap<>();

    @Override
    public void save(WorkOrder order) {
        orders.put(order.id(), order);
    }

    @Override
    public Optional<WorkOrder> findById(WorkOrderId id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public List<WorkOrder> findAllByReceivedAtDescending() {
        return orders.values().stream()
                .sorted(Comparator.comparing(WorkOrder::receivedAt).reversed())
                .toList();
    }
}
