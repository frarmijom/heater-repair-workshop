package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.repository.RepairOrderRepository;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import com.heaterworkshop.domain.exception.RepairOrderNotFoundException;

public final class StartRepairUseCase {

    private final RepairOrderRepository repository;

    public StartRepairUseCase(RepairOrderRepository repository) {
        this.repository = repository;
    }

    public RepairOrder execute(RepairOrderId id, Diagnosis diagnosis) {
        RepairOrder order = repository.findById(id)
                .orElseThrow(() -> new RepairOrderNotFoundException(id.value()));
        order.start(diagnosis);
        repository.save(order);
        return order;
    }
}
