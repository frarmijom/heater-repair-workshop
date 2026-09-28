package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.repository.WorkOrderRepository;

import java.util.List;

public final class ListWorkOrdersUseCase {
    private final WorkOrderRepository repository;

    public ListWorkOrdersUseCase(WorkOrderRepository repository) {
        this.repository = repository;
    }

    public List<WorkOrder> execute() {
        return repository.findAllByReceivedAtDescending();
    }
}
