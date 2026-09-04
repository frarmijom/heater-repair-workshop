package com.heaterworkshop.application.usecase;

import com.heaterworkshop.application.port.CustomerNotifier;
import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.entity.RepairStatus;
import com.heaterworkshop.domain.repository.RepairOrderRepository;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import com.heaterworkshop.domain.exception.RepairOrderNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepairUseCasesTest {

    @Test
    void startsAndPersistsARepair() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = newOrder();
        StartRepairUseCase useCase = new StartRepairUseCase(repository);
        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        RepairOrder updated = useCase.execute(order.id(), new Diagnosis("Damaged ignition sensor"));

        assertEquals(RepairStatus.IN_PROGRESS, order.status());
        assertEquals(order, updated);
        verify(repository).save(order);
    }

    @Test
    void completesPersistsAndNotifies() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        CustomerNotifier notifier = mock(CustomerNotifier.class);
        RepairOrder order = newOrder();
        order.start(new Diagnosis("Damaged ignition sensor"));
        CompleteRepairUseCase useCase = new CompleteRepairUseCase(repository, notifier);
        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        RepairOrder updated = useCase.execute(order.id());

        assertEquals(RepairStatus.COMPLETED, order.status());
        assertEquals(order, updated);
        verify(repository).save(order);
        verify(notifier).notify(
                new CustomerContact("+56911112222"),
                "Repair order ORDER-550E8400-E29B-41D4-A716-446655440001 has been completed."
        );
    }

    @Test
    void cannotStartAnUnknownOrder() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        StartRepairUseCase useCase = new StartRepairUseCase(repository);
        RepairOrderId id = new RepairOrderId("ORDER-550E8400-E29B-41D4-A716-446655440404");

        assertThrows(RepairOrderNotFoundException.class,
                () -> useCase.execute(id, new Diagnosis("Damaged sensor")));
    }

    @Test
    void cannotCompleteAnUnknownOrder() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        CompleteRepairUseCase useCase = new CompleteRepairUseCase(repository, mock(CustomerNotifier.class));
        RepairOrderId id = new RepairOrderId("ORDER-550E8400-E29B-41D4-A716-446655440404");

        assertThrows(RepairOrderNotFoundException.class, () -> useCase.execute(id));
    }

    private RepairOrder newOrder() {
        return new RepairOrder(new RepairOrderId("ORDER-550E8400-E29B-41D4-A716-446655440001"),
                "Maria Gonzalez", new CustomerContact("+56911112222"), "Bosch",
                "Therm 5700", "Turns off", Instant.parse("2026-09-03T18:30:00Z"));
    }
}
