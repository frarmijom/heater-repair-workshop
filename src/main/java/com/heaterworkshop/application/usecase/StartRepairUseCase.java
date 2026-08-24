package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.repository.RepairOrderRepository;
import com.heaterworkshop.domain.valueobject.Diagnosis;

public final class StartRepairUseCase {

    private final RepairOrderRepository repository;

    public StartRepairUseCase(RepairOrderRepository repository) {
        this.repository = repository;
    }

    public void execute(RepairOrder order, Diagnosis diagnosis) {
        order.start(diagnosis);
        repository.save(order);
    }
}
