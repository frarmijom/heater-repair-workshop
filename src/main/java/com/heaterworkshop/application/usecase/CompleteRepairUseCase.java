package com.heaterworkshop.application.usecase;

import com.heaterworkshop.application.port.CustomerNotifier;
import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.repository.RepairOrderRepository;

public final class CompleteRepairUseCase {

    private final RepairOrderRepository repository;
    private final CustomerNotifier notifier;

    public CompleteRepairUseCase(RepairOrderRepository repository, CustomerNotifier notifier) {
        this.repository = repository;
        this.notifier = notifier;
    }

    public void execute(RepairOrder order) {
        order.complete();
        repository.save(order);
        notifier.notify(
                order.customerContact(),
                "Repair order " + order.id().value() + " has been completed."
        );
    }
}
