package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.repository.RepairOrderRepository;

import java.util.List;

public final class ListRepairOrdersUseCase {
    private final RepairOrderRepository repository;

    public ListRepairOrdersUseCase(RepairOrderRepository repository) {
        this.repository = repository;
    }

    public List<RepairOrder> execute() {
        return repository.findAllByReceivedAtDescending();
    }
}
