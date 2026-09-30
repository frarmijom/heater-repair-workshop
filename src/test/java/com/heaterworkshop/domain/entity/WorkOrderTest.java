package com.heaterworkshop.domain.entity;

import com.heaterworkshop.domain.exception.InvalidWorkOrderStateException;
import com.heaterworkshop.domain.valueobject.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class WorkOrderTest {
    private static final Instant RECEIVED = Instant.parse("2026-09-03T18:30:00Z");
    private static final WorkOrderId ID = new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440000");
    private static final Diagnosis DIAGNOSIS = new Diagnosis("  Replace sensor  ");
    private WorkOrder create(ServiceType type, String issue) {
        return new WorkOrder(ID, " Maria ", new CustomerContact("+56911112222"), " Bosch ", " Therm ", type, issue, RECEIVED);
    }
    private WorkOrder at(ServiceType type, WorkOrderStatus status) {
        WorkOrder order = create(type, " Issue ");
        if (status == WorkOrderStatus.RECEIVED) return order;
        if (type == ServiceType.REPAIR) {
            order.beginDiagnosis();
            order.recordDiagnosis(DIAGNOSIS);
            if (status == WorkOrderStatus.DIAGNOSIS) return order;
            order.completeDiagnosis();
            if (status == WorkOrderStatus.WAITING_CUSTOMER) return order;
            if (status == WorkOrderStatus.NOT_APPROVED) { order.rejectRepair(); return order; }
            order.approveRepair(status != WorkOrderStatus.WAITING_PARTS);
        } else if (status == WorkOrderStatus.WAITING_PARTS) order.waitForParts();
        else order.startWork();
        if (status == WorkOrderStatus.COMPLETED) order.complete(RECEIVED.plusSeconds(60));
        return order;
    }
    enum Action {
        BEGIN(WorkOrder::beginDiagnosis), RECORD(o -> o.recordDiagnosis(DIAGNOSIS)), FINISH_DIAGNOSIS(WorkOrder::completeDiagnosis),
        APPROVE(o -> o.approveRepair(true)), APPROVE_WAIT(o -> o.approveRepair(false)), REJECT(WorkOrder::rejectRepair),
        WAIT(WorkOrder::waitForParts), START(WorkOrder::startWork), COMPLETE(o -> o.complete(RECEIVED.plusSeconds(60)));
        final Consumer<WorkOrder> execute;
        Action(Consumer<WorkOrder> execute) { this.execute = execute; }
    }
    static Stream<Arguments> transitions() {
        return Stream.of(ServiceType.values()).flatMap(type -> Stream.of(WorkOrderStatus.values())
                .filter(status -> type == ServiceType.REPAIR || status != WorkOrderStatus.DIAGNOSIS
                        && status != WorkOrderStatus.WAITING_CUSTOMER && status != WorkOrderStatus.NOT_APPROVED)
                .flatMap(status -> Stream.of(Action.values()).map(action -> Arguments.of(type, status, action))));
    }
    @ParameterizedTest @MethodSource("transitions")
    void enforcesEveryTransition(ServiceType type, WorkOrderStatus status, Action action) {
        WorkOrder order = at(type, status);
        WorkOrderStatus target = switch(action) {
            case BEGIN -> type == ServiceType.REPAIR && status == WorkOrderStatus.RECEIVED ? WorkOrderStatus.DIAGNOSIS : null;
            case RECORD -> type == ServiceType.REPAIR && status == WorkOrderStatus.DIAGNOSIS ? status : null;
            case FINISH_DIAGNOSIS -> type == ServiceType.REPAIR && status == WorkOrderStatus.DIAGNOSIS ? WorkOrderStatus.WAITING_CUSTOMER : null;
            case APPROVE -> status == WorkOrderStatus.WAITING_CUSTOMER ? WorkOrderStatus.IN_PROGRESS : null;
            case APPROVE_WAIT -> status == WorkOrderStatus.WAITING_CUSTOMER ? WorkOrderStatus.WAITING_PARTS : null;
            case REJECT -> status == WorkOrderStatus.WAITING_CUSTOMER ? WorkOrderStatus.NOT_APPROVED : null;
            case WAIT -> type == ServiceType.MAINTENANCE && status == WorkOrderStatus.RECEIVED ? WorkOrderStatus.WAITING_PARTS : null;
            case START -> status == WorkOrderStatus.WAITING_PARTS || type == ServiceType.MAINTENANCE && status == WorkOrderStatus.RECEIVED ? WorkOrderStatus.IN_PROGRESS : null;
            case COMPLETE -> status == WorkOrderStatus.IN_PROGRESS ? WorkOrderStatus.COMPLETED : null;
        };
        if (target == null) {
            var decision = order.customerDecision(); var diagnosis = order.diagnosis(); var completed = order.completedAt();
            assertThrows(InvalidWorkOrderStateException.class, () -> action.execute.accept(order));
            assertEquals(status, order.status()); assertEquals(decision, order.customerDecision());
            assertEquals(diagnosis, order.diagnosis()); assertEquals(completed, order.completedAt());
        } else {
            action.execute.accept(order);
            assertEquals(target, order.status());
            WorkOrder restored = restore(order, order.lifecycleVersion(), order.legacyStatus(), order.customerDecision());
            assertEquals(target, restored.status());
            assertEquals(order.customerDecision(), restored.customerDecision());
        }
    }
    @ParameterizedTest @EnumSource(ServiceType.class)
    void completesAfterWaitingForParts(ServiceType type) {
        WorkOrder order = at(type, WorkOrderStatus.WAITING_PARTS);
        order.startWork(); order.complete(RECEIVED.plusSeconds(5));
        assertEquals(WorkOrderStatus.COMPLETED, order.status());
        assertEquals(type == ServiceType.REPAIR ? CustomerDecision.APPROVED : null, order.customerDecision());
        assertEquals(type == ServiceType.REPAIR ? DIAGNOSIS : null, order.diagnosis());
    }
    @Test void requiresDiagnosisBeforeCustomerDecision() {
        WorkOrder order = create(ServiceType.REPAIR, "Issue"); order.beginDiagnosis();
        assertThrows(IllegalArgumentException.class, order::completeDiagnosis);
        assertThrows(NullPointerException.class, () -> order.recordDiagnosis(null));
        assertEquals(WorkOrderStatus.DIAGNOSIS, order.status());
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"   ", "\t"})
    void rejectsMissingRepairIssue(String value) { assertThrows(IllegalArgumentException.class, () -> create(ServiceType.REPAIR, value)); }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"   ", "\t", "\u2003"})
    void normalizesMaintenanceAbsence(String value) { assertEquals("", create(ServiceType.MAINTENANCE, value).reportedIssue()); }
    @ParameterizedTest @EnumSource(ServiceType.class)
    void normalizesFieldsAndCreatesOnlyV1(ServiceType type) {
        WorkOrder order = create(type, " Issue ");
        assertEquals("Maria", order.customerName()); assertEquals("Bosch", order.heaterBrand());
        assertEquals("Therm", order.heaterModel()); assertEquals("Issue", order.reportedIssue());
        assertEquals(LifecycleVersion.V1, order.lifecycleVersion()); assertNull(order.legacyStatus()); assertNull(order.customerDecision());
        assertEquals(ID, order.id()); assertEquals(RECEIVED, order.receivedAt()); assertNull(order.completedAt());
    }
    @Test void rejectsNullTypeAndInvalidTime() {
        assertThrows(NullPointerException.class, () -> create(null,"Issue"));
        WorkOrder order = at(ServiceType.MAINTENANCE, WorkOrderStatus.IN_PROGRESS);
        assertThrows(NullPointerException.class, () -> order.complete(null));
        assertThrows(IllegalArgumentException.class, () -> order.complete(RECEIVED.minusSeconds(1)));
        assertEquals(WorkOrderStatus.IN_PROGRESS, order.status());
    }
    @ParameterizedTest @EnumSource(ServiceType.class)
    void preservesLegacyInProgressWithoutInventingApproval(ServiceType type) {
        WorkOrder legacy = WorkOrder.restore(ID,"Maria",new CustomerContact("+56911112222"),"Bosch","Therm",type,"Issue",
                WorkOrderStatus.IN_PROGRESS, DIAGNOSIS, RECEIVED,null,LifecycleVersion.LEGACY,WorkOrderStatus.IN_PROGRESS,null);
        legacy.complete(RECEIVED.plusSeconds(60));
        WorkOrder restored = restore(legacy,LifecycleVersion.LEGACY,WorkOrderStatus.IN_PROGRESS,null);
        assertEquals(WorkOrderStatus.COMPLETED, restored.status()); assertNull(restored.customerDecision());
        assertEquals(DIAGNOSIS, restored.diagnosis());
        assertThrows(InvalidWorkOrderStateException.class, restored::startWork);
        assertThrows(InvalidWorkOrderStateException.class, () -> restored.approveRepair(true));
    }
    @Test void legacyCompletedPreservesExistingDataAndIsTerminal() {
        WorkOrder legacy = WorkOrder.restore(ID,"Maria",new CustomerContact("+56911112222"),"Bosch","Therm",ServiceType.REPAIR,"Issue",
                WorkOrderStatus.COMPLETED,DIAGNOSIS,RECEIVED,RECEIVED.minusSeconds(1),LifecycleVersion.LEGACY,WorkOrderStatus.COMPLETED,null);
        assertEquals(RECEIVED.minusSeconds(1), legacy.completedAt());
        for(Action action:Action.values()) assertThrows(InvalidWorkOrderStateException.class, () -> action.execute.accept(legacy));
    }
    @Test void legacyReceivedCannotBorrowInProgressExemption() {
        WorkOrder order = create(ServiceType.REPAIR,"Issue");
        WorkOrder legacy = restore(order,LifecycleVersion.LEGACY,WorkOrderStatus.RECEIVED,null);
        assertThrows(InvalidWorkOrderStateException.class, legacy::startWork);
        legacy.beginDiagnosis(); legacy.recordDiagnosis(DIAGNOSIS); legacy.completeDiagnosis(); legacy.approveRepair(true); legacy.complete();
        assertEquals(CustomerDecision.APPROVED, legacy.customerDecision());
        assertEquals(WorkOrderStatus.RECEIVED, legacy.legacyStatus());
    }
    @Test void rejectsForgedProvenanceAndDecisionsOnRestore() {
        WorkOrder started = at(ServiceType.REPAIR,WorkOrderStatus.IN_PROGRESS);
        assertThrows(IllegalArgumentException.class, () -> restore(started,LifecycleVersion.V1,null,null));
        assertThrows(IllegalArgumentException.class, () -> restore(started,LifecycleVersion.LEGACY,WorkOrderStatus.RECEIVED,null));
        assertThrows(IllegalArgumentException.class, () -> restore(started,LifecycleVersion.V1,WorkOrderStatus.IN_PROGRESS,null));
        assertThrows(IllegalArgumentException.class, () -> restore(started,LifecycleVersion.LEGACY,null,null));
        assertThrows(IllegalArgumentException.class, () -> restore(started,LifecycleVersion.LEGACY,WorkOrderStatus.IN_PROGRESS,CustomerDecision.APPROVED));
        assertThrows(IllegalArgumentException.class, () -> restore(started,LifecycleVersion.LEGACY,WorkOrderStatus.COMPLETED,null));
        assertThrows(NullPointerException.class, () -> restore(started,null,null,null));
        WorkOrder received = create(ServiceType.REPAIR,"Issue");
        assertThrows(IllegalArgumentException.class, () -> restore(received,LifecycleVersion.V1,null,CustomerDecision.APPROVED));
        WorkOrder maintenance = at(ServiceType.MAINTENANCE,WorkOrderStatus.IN_PROGRESS);
        assertThrows(IllegalArgumentException.class, () -> restore(maintenance,LifecycleVersion.V1,null,CustomerDecision.APPROVED));
    }
    @Test void ownsMultipleEquipmentsInStablePositionOrder() {
        WorkOrderEquipment second = new WorkOrderEquipment(
                new EquipmentId(java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440002")),
                "Mademsa", "11L", null, null, null, 2);
        WorkOrderEquipment first = new WorkOrderEquipment(
                new EquipmentId(java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440001")),
                "Junkers", "WR10", "10 L", null, null, 1);

        WorkOrder order = new WorkOrder(ID, "Maria", new CustomerContact("+56911112222"),
                java.util.List.of(second, first), ServiceType.MAINTENANCE, null, RECEIVED);

        assertEquals(java.util.List.of(first, second), order.equipments());
        assertThrows(UnsupportedOperationException.class, () -> order.equipments().add(first));
    }

    @Test void rejectsMissingEmptyNullOrDuplicatePositionEquipments() {
        WorkOrderEquipment first = new WorkOrderEquipment(
                new EquipmentId(java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440001")),
                "Junkers", "WR10", null, null, null, 1);
        WorkOrderEquipment duplicate = new WorkOrderEquipment(
                new EquipmentId(java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440002")),
                "Mademsa", "11L", null, null, null, 1);

        assertThrows(NullPointerException.class, () -> new WorkOrder(ID, "Maria",
                new CustomerContact("+56911112222"), (java.util.List<WorkOrderEquipment>) null,
                ServiceType.MAINTENANCE, null, RECEIVED));
        assertThrows(IllegalArgumentException.class, () -> new WorkOrder(ID, "Maria",
                new CustomerContact("+56911112222"), java.util.List.of(),
                ServiceType.MAINTENANCE, null, RECEIVED));
        assertThrows(NullPointerException.class, () -> new WorkOrder(ID, "Maria",
                new CustomerContact("+56911112222"), java.util.Arrays.asList(first, null),
                ServiceType.MAINTENANCE, null, RECEIVED));
        assertThrows(IllegalArgumentException.class, () -> new WorkOrder(ID, "Maria",
                new CustomerContact("+56911112222"), java.util.List.of(first, duplicate),
                ServiceType.MAINTENANCE, null, RECEIVED));
    }

    @Test void restoresMultipleEquipmentsWithoutLosingTheirIdentity() {
        WorkOrderEquipment first = new WorkOrderEquipment(
                new EquipmentId(java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440001")),
                "Junkers", "WR10", null, "SN-1", null, 1);
        WorkOrderEquipment second = new WorkOrderEquipment(
                new EquipmentId(java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440002")),
                "Mademsa", "11L", null, "SN-2", null, 2);

        WorkOrder restored = WorkOrder.restore(ID, "Maria", new CustomerContact("+56911112222"),
                java.util.List.of(first, second), ServiceType.MAINTENANCE, "", WorkOrderStatus.RECEIVED,
                null, RECEIVED, null, LifecycleVersion.V1, null, null);

        assertEquals(java.util.List.of(first, second), restored.equipments());
    }

    private WorkOrder restore(WorkOrder order,LifecycleVersion version,WorkOrderStatus legacy,CustomerDecision decision) {
        return WorkOrder.restore(order.id(),order.customerName(),order.customerContact(),order.heaterBrand(),order.heaterModel(),
                order.serviceType(),order.reportedIssue(),order.status(),order.diagnosis(),order.receivedAt(),order.completedAt(),version,legacy,decision);
    }
}
