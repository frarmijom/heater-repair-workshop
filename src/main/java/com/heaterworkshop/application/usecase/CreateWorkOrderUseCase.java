package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.entity.WorkOrderEquipment;
import com.heaterworkshop.domain.entity.WorkOrderEquipmentLifecycle;
import com.heaterworkshop.domain.entity.EquipmentIntakeRoute;
import com.heaterworkshop.domain.entity.EquipmentType;
import com.heaterworkshop.domain.entity.ServiceType;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

public final class CreateWorkOrderUseCase {
    private final WorkOrderRepository repository;
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;

    public CreateWorkOrderUseCase(WorkOrderRepository repository) {
        this(repository, Clock.systemUTC(), UUID::randomUUID);
    }

    CreateWorkOrderUseCase(WorkOrderRepository repository, Clock clock, Supplier<UUID> uuidSupplier) {
        this.repository = repository;
        this.clock = clock;
        this.uuidSupplier = uuidSupplier;
    }

    public WorkOrder execute(String customerName, CustomerContact contact, List<WorkOrderEquipment> equipments,
                             ServiceType serviceType, String reportedIssue) {
        String id = "ORDER-" + uuidSupplier.get().toString().toUpperCase(Locale.ROOT);
        WorkOrder order = new WorkOrder(new WorkOrderId(id), customerName, contact,
                equipments, serviceType, reportedIssue, Instant.now(clock));
        repository.save(order);
        return order;
    }

    public WorkOrder executeV2(String customerName, CustomerContact contact,
                               List<EquipmentV2Input> equipmentInputs) {
        Instant receivedAt = Instant.now(clock);

        List<WorkOrderEquipment> equipments = equipmentInputs.stream()
                .map(input -> new WorkOrderEquipment(
                        new EquipmentId(UUID.randomUUID()),
                        input.type(),
                        input.brand(),
                        input.model(),
                        input.capacity(),
                        input.serialNumber(),
                        input.notes(),
                        input.position(),
                        WorkOrderEquipmentLifecycle.create(
                                input.intakeRoute(),
                                input.reportedIssue(),
                                receivedAt)))
                .toList();

        String id = "ORDER-" + uuidSupplier.get().toString().toUpperCase(Locale.ROOT);
        WorkOrder order = WorkOrder.createV2(
                new WorkOrderId(id),
                customerName,
                contact,
                equipments,
                receivedAt);
        repository.save(order);
        return order;
    }

    public record EquipmentV2Input(
            EquipmentType type,
            String brand,
            String model,
            String capacity,
            String serialNumber,
            String notes,
            int position,
            EquipmentIntakeRoute intakeRoute,
            String reportedIssue) {
    }

    /** Transitional overload for callers that still submit one legacy heater. */
    public WorkOrder execute(String customerName, CustomerContact contact, String heaterBrand,
                               String heaterModel, ServiceType serviceType, String reportedIssue) {
        String id = "ORDER-" + uuidSupplier.get().toString().toUpperCase(Locale.ROOT);
        WorkOrder order = new WorkOrder(new WorkOrderId(id), customerName, contact,
                heaterBrand, heaterModel, serviceType, reportedIssue, Instant.now(clock));
        repository.save(order);
        return order;
    }
}
