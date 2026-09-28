package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.entity.ServiceType;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryWorkOrderRepositoryTest {

    @ParameterizedTest
    @EnumSource(ServiceType.class)
    void savesAndFindsAnOrder(ServiceType type) {
        InMemoryWorkOrderRepository repository = new InMemoryWorkOrderRepository();
        WorkOrder order = new WorkOrder(
                new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440001"),
                "Maria Gonzalez", new CustomerContact("+56911112222"), "Bosch",
                "Therm 5700", type, type == ServiceType.REPAIR ? "Turns off" : null, Instant.parse("2026-09-03T18:30:00Z")
        );

        repository.save(order);

        assertSame(order, repository.findById(order.id()).orElseThrow());
        assertEquals(type, repository.findById(order.id()).orElseThrow().serviceType());
        assertEquals(type == ServiceType.REPAIR ? "Turns off" : "",
                repository.findAllByReceivedAtDescending().get(0).reportedIssue());
    }

    @Test
    void returnsEmptyWhenOrderDoesNotExist() {
        InMemoryWorkOrderRepository repository = new InMemoryWorkOrderRepository();

        WorkOrderId missingId = new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440404");
        assertTrue(repository.findById(missingId).isEmpty());
        assertEquals(0, repository.findById(missingId).stream().count());
    }

    @Test
    void listsOrdersNewestFirst() {
        InMemoryWorkOrderRepository repository = new InMemoryWorkOrderRepository();
        WorkOrder older = order("ORDER-550E8400-E29B-41D4-A716-446655440001", "2026-09-03T18:00:00Z");
        WorkOrder newer = order("ORDER-550E8400-E29B-41D4-A716-446655440002", "2026-09-03T19:00:00Z");
        repository.save(older);
        repository.save(newer);

        assertEquals(java.util.List.of(newer, older), repository.findAllByReceivedAtDescending());
    }

    private WorkOrder order(String id, String receivedAt) {
        return new WorkOrder(new WorkOrderId(id), "Maria Gonzalez",
                new CustomerContact("+56911112222"), "Bosch", "Therm 5700",
                ServiceType.REPAIR, "Turns off", Instant.parse(receivedAt));
    }
}
