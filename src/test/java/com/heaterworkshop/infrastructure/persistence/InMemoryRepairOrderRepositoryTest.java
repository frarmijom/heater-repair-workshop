package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRepairOrderRepositoryTest {

    @Test
    void savesAndFindsAnOrder() {
        InMemoryRepairOrderRepository repository = new InMemoryRepairOrderRepository();
        RepairOrder order = new RepairOrder(
                new RepairOrderId("ORDER-550E8400-E29B-41D4-A716-446655440001"),
                "Maria Gonzalez", new CustomerContact("+56911112222"), "Bosch",
                "Therm 5700", "Turns off", Instant.parse("2026-09-03T18:30:00Z")
        );

        repository.save(order);

        assertSame(order, repository.findById(order.id()).orElseThrow());
    }

    @Test
    void returnsEmptyWhenOrderDoesNotExist() {
        InMemoryRepairOrderRepository repository = new InMemoryRepairOrderRepository();

        RepairOrderId missingId = new RepairOrderId("ORDER-550E8400-E29B-41D4-A716-446655440404");
        assertTrue(repository.findById(missingId).isEmpty());
        assertEquals(0, repository.findById(missingId).stream().count());
    }

    @Test
    void listsOrdersNewestFirst() {
        InMemoryRepairOrderRepository repository = new InMemoryRepairOrderRepository();
        RepairOrder older = order("ORDER-550E8400-E29B-41D4-A716-446655440001", "2026-09-03T18:00:00Z");
        RepairOrder newer = order("ORDER-550E8400-E29B-41D4-A716-446655440002", "2026-09-03T19:00:00Z");
        repository.save(older);
        repository.save(newer);

        assertEquals(java.util.List.of(newer, older), repository.findAllByReceivedAtDescending());
    }

    private RepairOrder order(String id, String receivedAt) {
        return new RepairOrder(new RepairOrderId(id), "Maria Gonzalez",
                new CustomerContact("+56911112222"), "Bosch", "Therm 5700",
                "Turns off", Instant.parse(receivedAt));
    }
}
