package com.heaterworkshop.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Repair Order")
class RepairOrderTest {

    @Test
    @DisplayName("Should initialize a repair order as received and without diagnosis")
    void shouldInitializeRepairOrderAsReceivedAndWithoutDiagnosis() {
        // Arrange
        String orderId = "ORDER-001";
        String customerContact = "+56911112222";

        // Act
        RepairOrder order = new RepairOrder(orderId, customerContact);

        // Assert
        assertEquals(orderId, order.getOrderId());
        assertEquals(customerContact, order.getCustomerContact());
        assertEquals(RepairStatus.RECEIVED, order.getStatus());
        assertNull(order.getDiagnosis());
    }
}
