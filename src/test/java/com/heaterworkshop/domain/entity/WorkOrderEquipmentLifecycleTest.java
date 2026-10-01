package com.heaterworkshop.domain.entity;

import com.heaterworkshop.domain.exception.InvalidWorkOrderStateException;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class WorkOrderEquipmentLifecycleTest {
    private static final Instant RECEIVED = Instant.parse("2026-10-01T20:00:00Z");

    @Test
    void directServiceStartsWithoutDiagnosis() {
        var lifecycle = WorkOrderEquipmentLifecycle.create(
                EquipmentIntakeRoute.DIRECT_SERVICE, null, RECEIVED);

        lifecycle.startWork();

        assertEquals(WorkOrderStatus.IN_PROGRESS, lifecycle.status());
        assertNull(lifecycle.diagnosis());
        assertNull(lifecycle.customerDecision());
    }

    @Test
    void diagnosisRequiredFollowsDiagnosisAndApprovalFlow() {
        var lifecycle = WorkOrderEquipmentLifecycle.create(
                EquipmentIntakeRoute.DIAGNOSIS_REQUIRED, "No enciende", RECEIVED);

        lifecycle.beginDiagnosis();
        lifecycle.recordDiagnosis(new Diagnosis("Replace sensor"));
        lifecycle.completeDiagnosis();
        lifecycle.approve(true);

        assertEquals(WorkOrderStatus.IN_PROGRESS, lifecycle.status());
        assertEquals(CustomerDecision.APPROVED, lifecycle.customerDecision());
    }

    @Test
    void diagnosisRequiredCanBeRejected() {
        var lifecycle = WorkOrderEquipmentLifecycle.create(
                EquipmentIntakeRoute.DIAGNOSIS_REQUIRED, "No enciende", RECEIVED);

        lifecycle.beginDiagnosis();
        lifecycle.recordDiagnosis(new Diagnosis("Replace sensor"));
        lifecycle.completeDiagnosis();
        lifecycle.reject();

        assertEquals(WorkOrderStatus.NOT_APPROVED, lifecycle.status());
        assertEquals(CustomerDecision.REJECTED, lifecycle.customerDecision());
    }

    @Test
    void directServiceCannotEnterDiagnosisFlow() {
        var lifecycle = WorkOrderEquipmentLifecycle.create(
                EquipmentIntakeRoute.DIRECT_SERVICE, null, RECEIVED);

        assertThrows(InvalidWorkOrderStateException.class, lifecycle::beginDiagnosis);
    }

    @Test
    void diagnosisRequiredCannotStartBeforeApproval() {
        var lifecycle = WorkOrderEquipmentLifecycle.create(
                EquipmentIntakeRoute.DIAGNOSIS_REQUIRED, "No enciende", RECEIVED);

        assertThrows(InvalidWorkOrderStateException.class, lifecycle::startWork);
        lifecycle.beginDiagnosis();
        assertThrows(InvalidWorkOrderStateException.class, () -> lifecycle.startWork());
    }

    @Test
    void completedRequiresInProgressAndValidTimestamp() {
        var lifecycle = WorkOrderEquipmentLifecycle.create(
                EquipmentIntakeRoute.DIRECT_SERVICE, null, RECEIVED);

        assertThrows(InvalidWorkOrderStateException.class, () -> lifecycle.complete(RECEIVED.plusSeconds(1)));
        lifecycle.startWork();
        lifecycle.complete(RECEIVED.plusSeconds(60));

        assertEquals(WorkOrderStatus.COMPLETED, lifecycle.status());
        assertEquals(RECEIVED.plusSeconds(60), lifecycle.completedAt());
    }

    @Test
    void diagnosisRequiredNeedsReportedIssue() {
        assertThrows(IllegalArgumentException.class, () ->
                WorkOrderEquipmentLifecycle.create(EquipmentIntakeRoute.DIAGNOSIS_REQUIRED, "  ", RECEIVED));
    }

    @Test
    void equipmentDefaultsToCalefontForV1Compatibility() {
        var equipment = new WorkOrderEquipment(
                new com.heaterworkshop.domain.valueobject.EquipmentId(
                        java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440001")),
                "Junkers", "WR10", null, null, null, 1);

        assertEquals(EquipmentType.CALEFONT, equipment.type());
        assertNull(equipment.lifecycle());
    }
}
