package com.heaterworkshop.application.usecase;

import com.heaterworkshop.application.port.CustomerNotifier;
import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.entity.RepairStatus;
import com.heaterworkshop.domain.repository.RepairOrderRepository;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RepairUseCasesTest {

    @Test
    void startsAndPersistsARepair() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = newOrder();
        StartRepairUseCase useCase = new StartRepairUseCase(repository);

        useCase.execute(order, new Diagnosis("Damaged ignition sensor"));

        assertEquals(RepairStatus.IN_PROGRESS, order.status());
        verify(repository).save(order);
    }

    @Test
    void completesPersistsAndNotifies() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        CustomerNotifier notifier = mock(CustomerNotifier.class);
        RepairOrder order = newOrder();
        order.start(new Diagnosis("Damaged ignition sensor"));
        CompleteRepairUseCase useCase = new CompleteRepairUseCase(repository, notifier);

        useCase.execute(order);

        assertEquals(RepairStatus.COMPLETED, order.status());
        verify(repository).save(order);
        verify(notifier).notify(
                new CustomerContact("+56911112222"),
                "Repair order ORDER-001 has been completed."
        );
    }

    private RepairOrder newOrder() {
        return new RepairOrder(new RepairOrderId("ORDER-001"), new CustomerContact("+56911112222"));
    }
}
