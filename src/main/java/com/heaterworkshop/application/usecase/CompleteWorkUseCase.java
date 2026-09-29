package com.heaterworkshop.application.usecase;

import com.heaterworkshop.application.port.CustomerNotifier;
import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import com.heaterworkshop.domain.exception.WorkOrderNotFoundException;

public final class CompleteWorkUseCase {

    private final WorkOrderRepository repository;
    private final CustomerNotifier notifier;

    public CompleteWorkUseCase(WorkOrderRepository repository, CustomerNotifier notifier) {
        this.repository = repository;
        this.notifier = notifier;
    }

    public WorkOrder execute(WorkOrderId id) {
        WorkOrder order = repository.findById(id)
                .orElseThrow(() -> new WorkOrderNotFoundException(id.value()));
        order.complete();
        repository.save(order);
        notifier.notify(
                order.customerContact(),
                "Work order " + order.id().value() + " has been completed."
        );
        return order;
    }
}
