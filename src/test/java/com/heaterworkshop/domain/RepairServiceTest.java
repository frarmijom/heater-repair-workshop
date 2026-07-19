package com.heaterworkshop.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Repair Service")
class RepairServiceTest {

    @Test
    @DisplayName("Should save and notify the customer when closing a repair order")
    void shouldSaveAndNotifyCustomerWhenClosingRepairOrder() {
        // Arrange
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        CustomerNotifier notifier = mock(CustomerNotifier.class);
        RepairWorkflow workflow = new RepairWorkflow(new RepairValidator());
        RepairOrder order = new RepairOrder("ORDER-001", "+56911112222");
        workflow.startRepair(order, "Damaged ignition sensor");
        RepairService service = new RepairService(workflow, repository, notifier);

        // Act
        service.closeOrder(order);

        // Assert
        assertEquals(RepairStatus.COMPLETED, order.getStatus());
        verify(repository, times(1)).save(order);
        verify(notifier, times(1)).notifyCustomer(
                "+56911112222",
                "Repair order ORDER-001 has been completed."
        );
    }
}
