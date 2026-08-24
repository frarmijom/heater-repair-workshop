package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRepairOrderRepositoryTest {

    @Test
    void savesAndFindsAnOrder() {
        InMemoryRepairOrderRepository repository = new InMemoryRepairOrderRepository();
        RepairOrder order = new RepairOrder(
                new RepairOrderId("ORDER-001"),
                new CustomerContact("+56911112222")
        );

        repository.save(order);

        assertSame(order, repository.findById(order.id()).orElseThrow());
    }

    @Test
    void returnsEmptyWhenOrderDoesNotExist() {
        InMemoryRepairOrderRepository repository = new InMemoryRepairOrderRepository();

        assertTrue(repository.findById(new RepairOrderId("ORDER-404")).isEmpty());
        assertEquals(0, repository.findById(new RepairOrderId("ORDER-404")).stream().count());
    }
}
