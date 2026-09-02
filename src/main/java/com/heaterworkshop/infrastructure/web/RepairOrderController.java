package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.application.usecase.*;
import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repair-orders")
@Tag(name = "Repair orders", description = "Heater repair lifecycle")
public class RepairOrderController {
    private final CreateRepairOrderUseCase create;
    private final GetRepairOrderUseCase get;
    private final StartRepairUseCase start;
    private final CompleteRepairUseCase complete;

    public RepairOrderController(CreateRepairOrderUseCase create, GetRepairOrderUseCase get,
                                 StartRepairUseCase start, CompleteRepairUseCase complete) {
        this.create = create; this.get = get; this.start = start; this.complete = complete;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a received repair order")
    public RepairOrderResponse create(@Valid @RequestBody CreateRepairOrderRequest request) {
        return RepairOrderResponse.from(create.execute(new RepairOrderId(request.id()),
                new CustomerContact(request.customerContact())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a repair order")
    public RepairOrderResponse get(@PathVariable String id) {
        return RepairOrderResponse.from(get.execute(new RepairOrderId(id)));
    }

    @PatchMapping("/{id}/start")
    @Operation(summary = "Start a received repair order")
    public RepairOrderResponse start(@PathVariable String id, @Valid @RequestBody StartRepairRequest request) {
        RepairOrder order = get.execute(new RepairOrderId(id));
        start.execute(order, new Diagnosis(request.diagnosis()));
        return RepairOrderResponse.from(order);
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Complete a repair in progress")
    public RepairOrderResponse complete(@PathVariable String id) {
        RepairOrder order = get.execute(new RepairOrderId(id));
        complete.execute(order);
        return RepairOrderResponse.from(order);
    }
}
