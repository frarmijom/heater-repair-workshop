package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.exception.RepairOrderNotFoundException;
import com.heaterworkshop.domain.repository.RepairOrderRepository;
import com.heaterworkshop.domain.valueobject.RepairOrderId;

public final class GetRepairOrderUseCase {
    private final RepairOrderRepository repository;
    public GetRepairOrderUseCase(RepairOrderRepository repository) { this.repository = repository; }
    public RepairOrder execute(RepairOrderId id) {
        return repository.findById(id).orElseThrow(() -> new RepairOrderNotFoundException(id.value()));
    }
}
