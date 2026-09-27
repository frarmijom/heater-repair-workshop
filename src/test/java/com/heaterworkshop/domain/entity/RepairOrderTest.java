package com.heaterworkshop.domain.entity;

import com.heaterworkshop.domain.exception.InvalidRepairStateException;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepairOrderTest {

    private static final String ID = "ORDER-550E8400-E29B-41D4-A716-446655440000";

    private RepairOrder order;

    @BeforeEach
    void setUp() {
        order = new RepairOrder(new RepairOrderId(ID), "Maria Gonzalez",
                new CustomerContact("+56911112222"), "Bosch", "Therm 5700",
                ServiceType.REPAIR, "Turns off", Instant.parse("2026-09-03T18:30:00Z"));
    }

    @Test
    void startsAsReceivedWithoutDiagnosis() {
        assertEquals(new RepairOrderId(ID), order.id());
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

    @Test
    void createsACompleteReceivedOrderAndTrimsTextFields() {
        Instant receivedAt = Instant.parse("2026-09-03T18:30:00Z");

        RepairOrder completeOrder = new RepairOrder(
                new RepairOrderId("ORDER-550E8400-E29B-41D4-A716-446655440002"), "  Maria Gonzalez  ",
                new CustomerContact("+56911112222"), "  Bosch ", " Therm 5700 ",
                ServiceType.REPAIR, "  Turns off after a few minutes.  ", receivedAt);

        assertEquals("Maria Gonzalez", completeOrder.customerName());
        assertEquals("Bosch", completeOrder.heaterBrand());
        assertEquals("Therm 5700", completeOrder.heaterModel());
        assertEquals("Turns off after a few minutes.", completeOrder.reportedIssue());
        assertEquals(receivedAt, completeOrder.receivedAt());
        assertNull(completeOrder.completedAt());
    }

    @Test
    void completionStoresTheProvidedTimestamp() {
        Instant receivedAt = Instant.parse("2026-09-03T18:30:00Z");
        Instant completedAt = Instant.parse("2026-09-03T19:30:00Z");
        RepairOrder completeOrder = new RepairOrder(
                new RepairOrderId("ORDER-550E8400-E29B-41D4-A716-446655440003"), "Maria Gonzalez",
                new CustomerContact("+56911112222"), "Bosch", "Therm 5700",
                ServiceType.REPAIR, "Turns off", receivedAt);

        completeOrder.start(new Diagnosis("Damaged ignition sensor"));
        completeOrder.complete(completedAt);

        assertEquals(completedAt, completeOrder.completedAt());
    }

    @Test
    void rejectsBlankRequiredText() {
        assertThrows(IllegalArgumentException.class, () -> new RepairOrder(
                new RepairOrderId("ORDER-550E8400-E29B-41D4-A716-446655440004"), " ", new CustomerContact("+56911112222"),
                "Bosch", "Therm 5700", ServiceType.REPAIR, "Turns off", Instant.now()));
    }

    @Test
    void rejectsACompletionTimestampBeforeReception() {
        order.start(new Diagnosis("Damaged sensor"));

        assertThrows(IllegalArgumentException.class,
                () -> order.complete(Instant.parse("2026-09-03T18:29:59Z")));
    }

    @Test
    void rejectsRestoredStatesWithInconsistentWorkflowData() {
        Instant receivedAt = Instant.parse("2026-09-03T18:30:00Z");

        assertThrows(IllegalArgumentException.class, () -> RepairOrder.restore(
                new RepairOrderId(ID), "Maria Gonzalez", new CustomerContact("+56911112222"),
                "Bosch", "Therm 5700", ServiceType.REPAIR, "Turns off", RepairStatus.RECEIVED,
                new Diagnosis("Unexpected diagnosis"), receivedAt, null));
        assertThrows(IllegalArgumentException.class, () -> RepairOrder.restore(
                new RepairOrderId(ID), "Maria Gonzalez", new CustomerContact("+56911112222"),
                "Bosch", "Therm 5700", ServiceType.REPAIR, "Turns off", RepairStatus.IN_PROGRESS,
                null, receivedAt, null));
        assertThrows(IllegalArgumentException.class, () -> RepairOrder.restore(
                new RepairOrderId(ID), "Maria Gonzalez", new CustomerContact("+56911112222"),
                "Bosch", "Therm 5700", ServiceType.REPAIR, "Turns off", RepairStatus.COMPLETED,
                new Diagnosis("Damaged sensor"), receivedAt, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void rejectsMissingRepairIssue(String issue) {
        assertThrows(IllegalArgumentException.class, () -> serviceOrder(ServiceType.REPAIR, issue));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\u2003"})
    void normalizesAbsentMaintenanceObservations(String issue) {
        assertEquals("", serviceOrder(ServiceType.MAINTENANCE, issue).reportedIssue());
    }

    @Test
    void rejectsNullServiceTypeInConstructionAndRestoration() {
        assertThrows(NullPointerException.class, () -> serviceOrder(null, "Issue"));
        assertThrows(NullPointerException.class, () -> RepairOrder.restore(new RepairOrderId(ID),
                "Maria", new CustomerContact("+56911112222"), "Bosch", "Therm", null,
                "Issue", RepairStatus.RECEIVED, null, Instant.now(), null));
    }

    @ParameterizedTest
    @EnumSource(ServiceType.class)
    void preservesLifecycleAndRestorationInvariantsForBothTypes(ServiceType type) {
        RepairOrder service = serviceOrder(type, "  Observations  ");
        assertEquals(type, service.serviceType());
        assertEquals("Observations", service.reportedIssue());
        assertEquals(RepairStatus.RECEIVED, service.status());
        assertThrows(InvalidRepairStateException.class, service::complete);
        assertThrows(NullPointerException.class, () -> service.start(null));
        assertEquals(RepairStatus.RECEIVED, service.status());
        service.start(new Diagnosis("Inspection completed"));
        assertEquals(RepairStatus.IN_PROGRESS, service.status());
        service.complete(service.receivedAt().plusSeconds(60));
        assertEquals(RepairStatus.COMPLETED, service.status());
        assertEquals(type, service.serviceType());
        assertThrows(InvalidRepairStateException.class, service::complete);
        for (RepairStatus status : RepairStatus.values()) {
            assertThrows(IllegalArgumentException.class, () -> RepairOrder.restore(service.id(),
                    service.customerName(), service.customerContact(), service.heaterBrand(),
                    service.heaterModel(), type, service.reportedIssue(), status,
                    status == RepairStatus.RECEIVED ? service.diagnosis() : null,
                    service.receivedAt(), null));
        }
    }

    private RepairOrder serviceOrder(ServiceType type, String issue) {
        return new RepairOrder(new RepairOrderId(ID), "Maria", new CustomerContact("+56911112222"),
                "Bosch", "Therm", type, issue, Instant.parse("2026-09-03T18:30:00Z"));
    }
}
