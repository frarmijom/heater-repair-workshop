package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import com.heaterworkshop.infrastructure.persistence.InMemoryRepairOrderRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class CreateAndGetRepairOrderUseCaseTest {
    @Test
    void createsPersistsAndRetrievesAnOrder() {
        InMemoryRepairOrderRepository repository = new InMemoryRepairOrderRepository();
        CreateRepairOrderUseCase create = new CreateRepairOrderUseCase(repository);
        GetRepairOrderUseCase get = new GetRepairOrderUseCase(repository);

        RepairOrder created = create.execute(new RepairOrderId("ORDER-100"), new CustomerContact("+56911112222"));

        assertSame(created, get.execute(new RepairOrderId("ORDER-100")));
    }
}
