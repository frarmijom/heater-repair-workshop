package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.repository.RepairOrderRepository;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.RepairOrderId;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

public final class CreateRepairOrderUseCase {
    private final RepairOrderRepository repository;
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;

    public CreateRepairOrderUseCase(RepairOrderRepository repository) {
        this(repository, Clock.systemUTC(), UUID::randomUUID);
    }

    CreateRepairOrderUseCase(RepairOrderRepository repository, Clock clock, Supplier<UUID> uuidSupplier) {
        this.repository = repository;
        this.clock = clock;
        this.uuidSupplier = uuidSupplier;
    }

    public RepairOrder execute(String customerName, CustomerContact contact, String heaterBrand,
                               String heaterModel, String reportedIssue) {
        String id = "ORDER-" + uuidSupplier.get().toString().toUpperCase(Locale.ROOT);
        RepairOrder order = new RepairOrder(new RepairOrderId(id), customerName, contact,
                heaterBrand, heaterModel, reportedIssue, Instant.now(clock));
        repository.save(order);
        return order;
    }
}
