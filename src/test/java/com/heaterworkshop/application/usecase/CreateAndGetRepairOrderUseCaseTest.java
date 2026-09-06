package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.exception.RepairOrderNotFoundException;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import com.heaterworkshop.infrastructure.persistence.InMemoryRepairOrderRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateAndGetRepairOrderUseCaseTest {
    @Test
    void createsPersistsAndRetrievesAnOrder() {
        InMemoryRepairOrderRepository repository = new InMemoryRepairOrderRepository();
        Instant receivedAt = Instant.parse("2026-09-03T18:30:00Z");
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        CreateRepairOrderUseCase create = new CreateRepairOrderUseCase(repository,
                Clock.fixed(receivedAt, ZoneOffset.UTC), () -> uuid);
        GetRepairOrderUseCase get = new GetRepairOrderUseCase(repository);

        RepairOrder created = create.execute("Maria Gonzalez", new CustomerContact("+56911112222"),
                "Bosch", "Therm 5700", "Turns off");

        assertEquals("ORDER-550E8400-E29B-41D4-A716-446655440000", created.id().value());
        assertEquals(receivedAt, created.receivedAt());
        assertSame(created, get.execute(created.id()));
    }

    @Test
    void rejectsAnUnknownOrder() {
        GetRepairOrderUseCase get = new GetRepairOrderUseCase(new InMemoryRepairOrderRepository());

        assertThrows(RepairOrderNotFoundException.class, () -> get.execute(
                new RepairOrderId("ORDER-550E8400-E29B-41D4-A716-446655440404")));
    }
}
