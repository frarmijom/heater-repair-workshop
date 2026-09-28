package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.entity.ServiceType;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.WorkOrderId;

import java.time.Clock;
import java.time.Instant;
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

    public WorkOrder execute(String customerName, CustomerContact contact, String heaterBrand,
                               String heaterModel, ServiceType serviceType, String reportedIssue) {
        String id = "ORDER-" + uuidSupplier.get().toString().toUpperCase(Locale.ROOT);
        WorkOrder order = new WorkOrder(new WorkOrderId(id), customerName, contact,
                heaterBrand, heaterModel, serviceType, reportedIssue, Instant.now(clock));
        repository.save(order);
        return order;
    }
}
