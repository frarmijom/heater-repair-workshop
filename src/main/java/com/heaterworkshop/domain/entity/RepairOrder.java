package com.heaterworkshop.domain.entity;

import com.heaterworkshop.domain.exception.InvalidRepairStateException;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.RepairOrderId;

import java.time.Instant;
import java.util.Objects;

public final class RepairOrder {

    private final RepairOrderId id;
    private final String customerName;
    private final CustomerContact customerContact;
    private final String heaterBrand;
    private final String heaterModel;
    private final String reportedIssue;
    private final Instant receivedAt;
    private RepairStatus status;
    private Diagnosis diagnosis;
    private Instant completedAt;

    /**
     * Compatibility constructor for the milestone 4 application layer. It will
     * be removed when the create use case is migrated to the shared contract.
     */
    public RepairOrder(RepairOrderId id, CustomerContact customerContact) {
        this(id, "Pending customer name", customerContact, "Pending brand",
                "Pending model", "Pending reported issue", Instant.now());
    }

    public RepairOrder(RepairOrderId id, String customerName, CustomerContact customerContact,
                       String heaterBrand, String heaterModel, String reportedIssue,
                       Instant receivedAt) {
        this.id = Objects.requireNonNull(id, "Repair order id is required.");
        this.customerName = requiredText(customerName, "Customer name");
        this.customerContact = Objects.requireNonNull(customerContact, "Customer contact is required.");
        this.heaterBrand = requiredText(heaterBrand, "Heater brand");
        this.heaterModel = requiredText(heaterModel, "Heater model");
        this.reportedIssue = requiredText(reportedIssue, "Reported issue");
        this.receivedAt = Objects.requireNonNull(receivedAt, "Received timestamp is required.");
        this.status = RepairStatus.RECEIVED;
    }

    public static RepairOrder restore(RepairOrderId id, String customerName,
                                      CustomerContact customerContact, String heaterBrand,
                                      String heaterModel, String reportedIssue,
                                      RepairStatus status, Diagnosis diagnosis,
                                      Instant receivedAt, Instant completedAt) {
        RepairOrder order = new RepairOrder(id, customerName, customerContact, heaterBrand,
                heaterModel, reportedIssue, receivedAt);
        order.status = Objects.requireNonNull(status, "Repair status is required.");
        order.diagnosis = diagnosis;
        order.completedAt = completedAt;
        order.validateRestoredState();
        return order;
    }

    /**
     * Compatibility restoration method for records created before the Unit 6
     * schema is introduced.
     */
    public static RepairOrder restore(RepairOrderId id, CustomerContact customerContact,
                                      RepairStatus status, Diagnosis diagnosis) {
        RepairOrder order = new RepairOrder(id, customerContact);
        order.status = status;
        order.diagnosis = diagnosis;
        return order;
    }

    private void validateRestoredState() {
        if (status == RepairStatus.RECEIVED && (diagnosis != null || completedAt != null)) {
            throw new IllegalArgumentException("A received order cannot have diagnosis or completion timestamp.");
        }
        if (status == RepairStatus.IN_PROGRESS && (diagnosis == null || completedAt != null)) {
            throw new IllegalArgumentException("An in-progress order requires diagnosis and cannot be completed.");
        }
        if (status == RepairStatus.COMPLETED && (diagnosis == null || completedAt == null)) {
            throw new IllegalArgumentException("A completed order requires diagnosis and completion timestamp.");
        }
    }

    private static String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    public void start(Diagnosis diagnosis) {
        if (status != RepairStatus.RECEIVED) {
            throw new InvalidRepairStateException("Only received orders can be started.");
        }

        this.diagnosis = Objects.requireNonNull(diagnosis, "Diagnosis is required to start a repair.");
        this.status = RepairStatus.IN_PROGRESS;
    }

    public void complete() {
        complete(Instant.now());
    }

    public void complete(Instant completedAt) {
        if (status != RepairStatus.IN_PROGRESS) {
            throw new InvalidRepairStateException("Only repairs in progress can be completed.");
        }

        Instant completionTimestamp = Objects.requireNonNull(completedAt, "Completion timestamp is required.");
        if (completionTimestamp.isBefore(receivedAt)) {
            throw new IllegalArgumentException("Completion timestamp cannot be before reception.");
        }
        this.status = RepairStatus.COMPLETED;
        this.completedAt = completionTimestamp;
    }

    public RepairOrderId id() {
        return id;
    }

    public CustomerContact customerContact() {
        return customerContact;
    }

    public String customerName() { return customerName; }

    public String heaterBrand() { return heaterBrand; }

    public String heaterModel() { return heaterModel; }

    public String reportedIssue() { return reportedIssue; }

    public Instant receivedAt() { return receivedAt; }

    public RepairStatus status() {
        return status;
    }

    public Diagnosis diagnosis() {
        return diagnosis;
    }

    public Instant completedAt() { return completedAt; }
}
