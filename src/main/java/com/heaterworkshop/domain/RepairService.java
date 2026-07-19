package com.heaterworkshop.domain;

public class RepairService {

    private final RepairWorkflow workflow;
    private final RepairOrderRepository repository;
    private final CustomerNotifier notifier;

    public RepairService(
            RepairWorkflow workflow,
            RepairOrderRepository repository,
            CustomerNotifier notifier) {
        this.workflow = workflow;
        this.repository = repository;
        this.notifier = notifier;
    }

    public void closeOrder(RepairOrder order) {
        workflow.completeRepair(order);
        repository.save(order);
        notifier.notifyCustomer(
                order.getCustomerContact(),
                "Repair order " + order.getOrderId() + " has been completed."
        );
    }
}
