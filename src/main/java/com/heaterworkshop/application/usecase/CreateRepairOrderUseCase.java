package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.repository.RepairOrderRepository;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.RepairOrderId;

public final class CreateRepairOrderUseCase {
    private final RepairOrderRepository repository;
    public CreateRepairOrderUseCase(RepairOrderRepository repository) { this.repository = repository; }
    public RepairOrder execute(RepairOrderId id, CustomerContact contact) {
        RepairOrder order = new RepairOrder(id, contact);
        repository.save(order);
        return order;
    }
}
