package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.application.usecase.*;
import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.entity.WorkOrderEquipment;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/work-orders")
@Tag(name = "Work orders", description = "Workshop service lifecycle")
public class WorkOrderController {
    private final CreateWorkOrderUseCase create;
    private final ListWorkOrdersUseCase list;
    private final GetWorkOrderUseCase get;
    private final StartWorkUseCase start;
    private final CompleteWorkUseCase complete;
    private final WorkOrderWorkflowUseCase workflow;

    public WorkOrderController(CreateWorkOrderUseCase create, ListWorkOrdersUseCase list,
                                 GetWorkOrderUseCase get,
                                 StartWorkUseCase start, CompleteWorkUseCase complete, WorkOrderWorkflowUseCase workflow) {
        this.create = create; this.list = list; this.get = get; this.start = start; this.complete = complete; this.workflow = workflow;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a received work order")
    public WorkOrderResponse create(@Valid @RequestBody CreateWorkOrderRequest request) {
        if (request.equipments() != null && !request.equipments().isEmpty()) {
            return WorkOrderResponse.from(create.execute(request.customerName(), new CustomerContact(request.customerContact()),
                    toEquipments(request.equipments()), request.serviceType(), request.reportedIssue()));
        }
        return WorkOrderResponse.from(create.execute(request.customerName(), new CustomerContact(request.customerContact()),
                request.heaterBrand(), request.heaterModel(), request.serviceType(), request.reportedIssue()));
    }

    private List<WorkOrderEquipment> toEquipments(List<WorkOrderEquipmentRequest> requests) {
        return requests.stream().map(request -> new WorkOrderEquipment(new EquipmentId(UUID.randomUUID()),
                request.brand(), request.model(), request.capacity(), request.serialNumber(), request.notes(), request.position())).toList();
    }

    @GetMapping
    @Operation(summary = "List work orders newest first")
    public List<WorkOrderResponse> list() {
        return list.execute().stream().map(WorkOrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a work order")
    public WorkOrderResponse get(@PathVariable String id) {
        return WorkOrderResponse.from(get.execute(new WorkOrderId(id)));
    }

    @PatchMapping("/{id}/diagnosis/begin")
    public WorkOrderResponse beginDiagnosis(@PathVariable String id) {
        return WorkOrderResponse.from(workflow.beginDiagnosis(new WorkOrderId(id)));
    }

    @PatchMapping("/{id}/diagnosis")
    public WorkOrderResponse recordDiagnosis(@PathVariable String id, @Valid @RequestBody RecordDiagnosisRequest request) {
        return WorkOrderResponse.from(workflow.recordDiagnosis(new WorkOrderId(id), new Diagnosis(request.diagnosis())));
    }

    @PatchMapping("/{id}/diagnosis/complete")
    public WorkOrderResponse completeDiagnosis(@PathVariable String id) {
        return WorkOrderResponse.from(workflow.completeDiagnosis(new WorkOrderId(id)));
    }

    @PatchMapping("/{id}/approve")
    public WorkOrderResponse approve(@PathVariable String id, @Valid @RequestBody ApproveWorkOrderRequest request) {
        return WorkOrderResponse.from(workflow.approve(new WorkOrderId(id), request.partsAvailable()));
    }

    @PatchMapping("/{id}/reject")
    public WorkOrderResponse reject(@PathVariable String id) {
        return WorkOrderResponse.from(workflow.reject(new WorkOrderId(id)));
    }

    @PatchMapping("/{id}/waiting-parts")
    public WorkOrderResponse waitForParts(@PathVariable String id) {
        return WorkOrderResponse.from(workflow.waitForParts(new WorkOrderId(id)));
    }

    @PatchMapping("/{id}/start")
    public WorkOrderResponse start(@PathVariable String id) {
        return WorkOrderResponse.from(start.execute(new WorkOrderId(id)));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Complete a repair in progress")
    public WorkOrderResponse complete(@PathVariable String id) {
        return WorkOrderResponse.from(complete.execute(new WorkOrderId(id)));
    }
}
