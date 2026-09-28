package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.exception.WorkOrderNotFoundException;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import com.heaterworkshop.domain.valueobject.WorkOrderId;

public final class GetWorkOrderUseCase {
    private final WorkOrderRepository repository;
    public GetWorkOrderUseCase(WorkOrderRepository repository) { this.repository = repository; }
    public WorkOrder execute(WorkOrderId id) {
        return repository.findById(id).orElseThrow(() -> new WorkOrderNotFoundException(id.value()));
    }
}
