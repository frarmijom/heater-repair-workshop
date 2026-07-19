package com.heaterworkshop.domain;

import com.heaterworkshop.domain.exception.InvalidRepairStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Repair Workflow")
class RepairWorkflowTest {

    @Test
    @DisplayName("Should start a received repair with a valid diagnosis")
    void shouldStartReceivedRepairWithValidDiagnosis() {
        // Arrange
        RepairOrder order = new RepairOrder("ORDER-001", "+56911112222");
        RepairWorkflow workflow = new RepairWorkflow(new RepairValidator());

        // Act
        workflow.startRepair(order, "Damaged ignition sensor");

        // Assert
        assertEquals(RepairStatus.IN_PROGRESS, order.getStatus());
        assertEquals("Damaged ignition sensor", order.getDiagnosis());
    }

    @Test
    @DisplayName("Should reject starting an order that is already in progress")
    void shouldRejectStartingOrderAlreadyInProgress() {
        // Arrange
        RepairOrder order = new RepairOrder("ORDER-001", "+56911112222");
        RepairWorkflow workflow = new RepairWorkflow(new RepairValidator());
        workflow.startRepair(order, "Damaged ignition sensor");

        // Act
        InvalidRepairStateException exception = assertThrows(
                InvalidRepairStateException.class,
                () -> workflow.startRepair(order, "Blocked water valve")
        );

        // Assert
        assertEquals("Only received orders can be started.", exception.getMessage());
    }

    @Test
    @DisplayName("Should complete a repair that is in progress")
    void shouldCompleteRepairInProgress() {
        // Arrange
        RepairOrder order = new RepairOrder("ORDER-001", "+56911112222");
        RepairWorkflow workflow = new RepairWorkflow(new RepairValidator());
        workflow.startRepair(order, "Damaged ignition sensor");

        // Act
        workflow.completeRepair(order);

        // Assert
        assertEquals(RepairStatus.COMPLETED, order.getStatus());
    }

    @Test
    @DisplayName("Should reject completing a repair that has not started")
    void shouldRejectCompletingRepairThatHasNotStarted() {
        // Arrange
        RepairOrder order = new RepairOrder("ORDER-001", "+56911112222");
        RepairWorkflow workflow = new RepairWorkflow(new RepairValidator());

        // Act
        InvalidRepairStateException exception = assertThrows(
                InvalidRepairStateException.class,
                () -> workflow.completeRepair(order)
        );

        // Assert
        assertEquals("Only repairs in progress can be completed.", exception.getMessage());
    }
}
