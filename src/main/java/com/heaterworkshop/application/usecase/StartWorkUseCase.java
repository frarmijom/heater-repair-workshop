package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import com.heaterworkshop.domain.exception.WorkOrderNotFoundException;

public final class StartWorkUseCase {

    private final WorkOrderRepository repository;

    public StartWorkUseCase(WorkOrderRepository repository) {
        this.repository = repository;
    }

    public WorkOrder execute(WorkOrderId id) {
        WorkOrder order = repository.findById(id)
                .orElseThrow(() -> new WorkOrderNotFoundException(id.value()));
        order.startWork();
        repository.save(order);
        return order;
    }
}
