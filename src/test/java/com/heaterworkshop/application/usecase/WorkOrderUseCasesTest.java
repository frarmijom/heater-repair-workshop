package com.heaterworkshop.application.usecase;

import com.heaterworkshop.application.port.CustomerNotifier;
import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.entity.WorkOrderStatus;
import com.heaterworkshop.domain.entity.ServiceType;
import com.heaterworkshop.domain.exception.WorkOrderNotFoundException;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkOrderUseCasesTest {

    @Test
    void startsAndPersistsMaintenanceWithoutDiagnosis() {
        WorkOrderRepository repository = mock(WorkOrderRepository.class);
        WorkOrder order = newOrder();
        StartWorkUseCase useCase = new StartWorkUseCase(repository);
        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        WorkOrder updated = useCase.execute(order.id());

        assertEquals(WorkOrderStatus.IN_PROGRESS, order.status());
        assertEquals(order, updated);
        verify(repository).save(order);
    }

    @Test
    void completesPersistsAndNotifies() {
        WorkOrderRepository repository = mock(WorkOrderRepository.class);
        CustomerNotifier notifier = mock(CustomerNotifier.class);
        WorkOrder order = newOrder();
        order.startWork();
        CompleteWorkUseCase useCase = new CompleteWorkUseCase(repository, notifier);
        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        WorkOrder updated = useCase.execute(order.id());

        assertEquals(WorkOrderStatus.COMPLETED, order.status());
        assertEquals(order, updated);
        verify(repository).save(order);
        verify(notifier).notify(
                new CustomerContact("+56911112222"),
                "Work order ORDER-550E8400-E29B-41D4-A716-446655440001 has been completed."
        );
    }

    @Test
    void cannotStartAnUnknownOrder() {
        WorkOrderRepository repository = mock(WorkOrderRepository.class);
        StartWorkUseCase useCase = new StartWorkUseCase(repository);
        WorkOrderId id = new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440404");

        assertThrows(WorkOrderNotFoundException.class,
                () -> useCase.execute(id));
    }

    @Test
    void cannotCompleteAnUnknownOrder() {
        WorkOrderRepository repository = mock(WorkOrderRepository.class);
        CompleteWorkUseCase useCase = new CompleteWorkUseCase(repository, mock(CustomerNotifier.class));
        WorkOrderId id = new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440404");

        assertThrows(WorkOrderNotFoundException.class, () -> useCase.execute(id));
    }

    private WorkOrder newOrder() {
        return new WorkOrder(new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440001"),
                "Maria Gonzalez", new CustomerContact("+56911112222"), "Bosch",
                "Therm 5700", ServiceType.MAINTENANCE, "Turns off", Instant.parse("2026-09-03T18:30:00Z"));
    }
}
