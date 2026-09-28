package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.exception.WorkOrderNotFoundException;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import java.util.function.Consumer;

public final class WorkOrderWorkflowUseCase {
    private final WorkOrderRepository repository;
    public WorkOrderWorkflowUseCase(WorkOrderRepository repository) { this.repository = repository; }
    public WorkOrder beginDiagnosis(WorkOrderId id) { return change(id, WorkOrder::beginDiagnosis); }
    public WorkOrder recordDiagnosis(WorkOrderId id, Diagnosis diagnosis) { return change(id, order -> order.recordDiagnosis(diagnosis)); }
    public WorkOrder completeDiagnosis(WorkOrderId id) { return change(id, WorkOrder::completeDiagnosis); }
    public WorkOrder approve(WorkOrderId id, boolean partsAvailable) { return change(id, order -> order.approveRepair(partsAvailable)); }
    public WorkOrder reject(WorkOrderId id) { return change(id, WorkOrder::rejectRepair); }
    public WorkOrder waitForParts(WorkOrderId id) { return change(id, WorkOrder::waitForParts); }
    private WorkOrder change(WorkOrderId id, Consumer<WorkOrder> action) {
        WorkOrder order = repository.findById(id).orElseThrow(() -> new WorkOrderNotFoundException(id.value()));
        action.accept(order);
        repository.save(order);
        return order;
    }
}
