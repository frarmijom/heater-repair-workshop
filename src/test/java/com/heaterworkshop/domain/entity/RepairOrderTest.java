package com.heaterworkshop.domain.entity;

import com.heaterworkshop.domain.exception.InvalidRepairStateException;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepairOrderTest {

    private RepairOrder order;

    @BeforeEach
    void setUp() {
        order = new RepairOrder(new RepairOrderId("ORDER-001"), new CustomerContact("+56911112222"));
    }

    @Test
    void startsAsReceivedWithoutDiagnosis() {
        assertEquals(new RepairOrderId("ORDER-001"), order.id());
        assertEquals(new CustomerContact("+56911112222"), order.customerContact());
        assertEquals(RepairStatus.RECEIVED, order.status());
        assertNull(order.diagnosis());
    }

    @Test
    void startsAndCompletesARepair() {
        Diagnosis diagnosis = new Diagnosis("Damaged ignition sensor");

        order.start(diagnosis);
        assertEquals(RepairStatus.IN_PROGRESS, order.status());
        assertEquals(diagnosis, order.diagnosis());

        order.complete();
        assertEquals(RepairStatus.COMPLETED, order.status());
    }

    @Test
    void cannotStartTwice() {
        order.start(new Diagnosis("Damaged ignition sensor"));

        InvalidRepairStateException exception = assertThrows(
                InvalidRepairStateException.class,
                () -> order.start(new Diagnosis("Blocked water valve"))
        );
        assertEquals("Only received orders can be started.", exception.getMessage());
    }

    @Test
    void cannotCompleteBeforeStarting() {
        InvalidRepairStateException exception = assertThrows(
                InvalidRepairStateException.class,
                order::complete
        );
        assertEquals("Only repairs in progress can be completed.", exception.getMessage());
    }
}
