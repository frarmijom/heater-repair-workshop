package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.entity.ServiceType;
import com.heaterworkshop.domain.exception.WorkOrderNotFoundException;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import com.heaterworkshop.infrastructure.persistence.InMemoryWorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateAndGetWorkOrderUseCaseTest {
    @ParameterizedTest
    @EnumSource(ServiceType.class)
    void createsPersistsAndRetrievesAnOrder(ServiceType type) {
        InMemoryWorkOrderRepository repository = new InMemoryWorkOrderRepository();
        Instant receivedAt = Instant.parse("2026-09-03T18:30:00Z");
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        CreateWorkOrderUseCase create = new CreateWorkOrderUseCase(repository,
                Clock.fixed(receivedAt, ZoneOffset.UTC), () -> uuid);
        GetWorkOrderUseCase get = new GetWorkOrderUseCase(repository);

        WorkOrder created = create.execute("Maria Gonzalez", new CustomerContact("+56911112222"),
                "Bosch", "Therm 5700", type, type == ServiceType.REPAIR ? "  Turns off  " : null);

        assertEquals("ORDER-550E8400-E29B-41D4-A716-446655440000", created.id().value());
        assertEquals(receivedAt, created.receivedAt());
        assertSame(created, get.execute(created.id()));
        WorkOrder retrieved = get.execute(created.id());
        assertEquals(type, retrieved.serviceType());
        assertEquals(type == ServiceType.REPAIR ? "Turns off" : "", retrieved.reportedIssue());
    }

    @Test
    void rejectsAnUnknownOrder() {
        GetWorkOrderUseCase get = new GetWorkOrderUseCase(new InMemoryWorkOrderRepository());

        assertThrows(WorkOrderNotFoundException.class, () -> get.execute(
                new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440404")));
    }
}
