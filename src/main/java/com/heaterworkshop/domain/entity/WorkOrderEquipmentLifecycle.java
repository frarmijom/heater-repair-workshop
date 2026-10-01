package com.heaterworkshop.domain.entity;

import com.heaterworkshop.domain.exception.InvalidWorkOrderStateException;
import com.heaterworkshop.domain.valueobject.Diagnosis;

import java.time.Instant;
import java.util.Objects;

/**
 * V2 operational lifecycle for one equipment inside a work order.
 * It is intentionally independent from the V1 WorkOrder lifecycle.
 */
public final class WorkOrderEquipmentLifecycle {
    private final EquipmentIntakeRoute intakeRoute;
    private final Instant receivedAt;
    private WorkOrderStatus status;
    private String reportedIssue;
    private Diagnosis diagnosis;
    private CustomerDecision customerDecision;
    private Instant completedAt;

    private WorkOrderEquipmentLifecycle(EquipmentIntakeRoute intakeRoute, String reportedIssue,
                                        Instant receivedAt) {
        this.intakeRoute = Objects.requireNonNull(intakeRoute, "Equipment intake route is required.");
        this.receivedAt = Objects.requireNonNull(receivedAt, "Equipment received timestamp is required.");
        this.reportedIssue = normalizeOptional(reportedIssue);
        if (intakeRoute == EquipmentIntakeRoute.DIAGNOSIS_REQUIRED && this.reportedIssue == null) {
            throw new IllegalArgumentException("Reported issue is required when diagnosis is required.");
        }
        this.status = WorkOrderStatus.RECEIVED;
    }

    public static WorkOrderEquipmentLifecycle create(EquipmentIntakeRoute intakeRoute,
                                                      String reportedIssue,
                                                      Instant receivedAt) {
        return new WorkOrderEquipmentLifecycle(intakeRoute, reportedIssue, receivedAt);
    }

    public void beginDiagnosis() {
        requireRoute(EquipmentIntakeRoute.DIAGNOSIS_REQUIRED);
        requireStatus(WorkOrderStatus.RECEIVED);
        status = WorkOrderStatus.DIAGNOSIS;
    }

    public void recordDiagnosis(Diagnosis diagnosis) {
        requireRoute(EquipmentIntakeRoute.DIAGNOSIS_REQUIRED);
        requireStatus(WorkOrderStatus.DIAGNOSIS);
        this.diagnosis = Objects.requireNonNull(diagnosis, "Diagnosis is required.");
    }

    public void completeDiagnosis() {
        requireRoute(EquipmentIntakeRoute.DIAGNOSIS_REQUIRED);
        requireStatus(WorkOrderStatus.DIAGNOSIS);
        if (diagnosis == null) throw new IllegalArgumentException("Record a diagnosis before completing diagnosis.");
        status = WorkOrderStatus.WAITING_CUSTOMER;
    }

    public void approve(boolean partsAvailable) {
        requireRoute(EquipmentIntakeRoute.DIAGNOSIS_REQUIRED);
        requireStatus(WorkOrderStatus.WAITING_CUSTOMER);
        customerDecision = CustomerDecision.APPROVED;
        status = partsAvailable ? WorkOrderStatus.IN_PROGRESS : WorkOrderStatus.WAITING_PARTS;
    }

    public void reject() {
        requireRoute(EquipmentIntakeRoute.DIAGNOSIS_REQUIRED);
        requireStatus(WorkOrderStatus.WAITING_CUSTOMER);
        customerDecision = CustomerDecision.REJECTED;
        status = WorkOrderStatus.NOT_APPROVED;
    }

    public void waitForParts() {
        requireStatus(WorkOrderStatus.RECEIVED);
        status = WorkOrderStatus.WAITING_PARTS;
    }

    public void startWork() {
        boolean directStart = intakeRoute == EquipmentIntakeRoute.DIRECT_SERVICE
                && status == WorkOrderStatus.RECEIVED;
        boolean approvedStart = status == WorkOrderStatus.WAITING_PARTS
                && (intakeRoute == EquipmentIntakeRoute.DIRECT_SERVICE
                || customerDecision == CustomerDecision.APPROVED);
        if (!directStart && !approvedStart) {
            throw new InvalidWorkOrderStateException("Work cannot start before its prerequisites are met.");
        }
        status = WorkOrderStatus.IN_PROGRESS;
    }

    public void complete(Instant completedAt) {
        requireStatus(WorkOrderStatus.IN_PROGRESS);
        Instant timestamp = Objects.requireNonNull(completedAt, "Completion timestamp is required.");
        if (timestamp.isBefore(receivedAt)) {
            throw new IllegalArgumentException("Completion timestamp cannot be before reception.");
        }
        this.completedAt = timestamp;
        status = WorkOrderStatus.COMPLETED;
    }

    public EquipmentIntakeRoute intakeRoute() { return intakeRoute; }
    public String reportedIssue() { return reportedIssue; }
    public Instant receivedAt() { return receivedAt; }
    public WorkOrderStatus status() { return status; }
    public Diagnosis diagnosis() { return diagnosis; }
    public CustomerDecision customerDecision() { return customerDecision; }
    public Instant completedAt() { return completedAt; }

    private void requireRoute(EquipmentIntakeRoute expected) {
        if (intakeRoute != expected) {
            throw new InvalidWorkOrderStateException("This action is not valid for the equipment intake route.");
        }
    }

    private void requireStatus(WorkOrderStatus expected) {
        if (status != expected) {
            throw new InvalidWorkOrderStateException("This action is not valid in the current equipment state.");
        }
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
