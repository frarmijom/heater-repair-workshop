package com.heaterworkshop.domain.entity;

import com.heaterworkshop.domain.exception.InvalidWorkOrderStateException;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.WorkOrderId;

import java.time.Instant;
import java.util.Objects;

public final class WorkOrder {

    private final WorkOrderId id;
    private final String customerName;
    private final CustomerContact customerContact;
    private final String heaterBrand;
    private final String heaterModel;
    private final ServiceType serviceType;
    private final String reportedIssue;
    private final Instant receivedAt;
    private WorkOrderStatus status;
    private Diagnosis diagnosis;
    private Instant completedAt;
    private LifecycleVersion lifecycleVersion = LifecycleVersion.V1;
    private WorkOrderStatus legacyStatus;
    private CustomerDecision customerDecision;

    public WorkOrder(WorkOrderId id, String customerName, CustomerContact customerContact,
                       String heaterBrand, String heaterModel, ServiceType serviceType, String reportedIssue,
                       Instant receivedAt) {
        this.id = Objects.requireNonNull(id, "Work order id is required.");
        this.customerName = requiredText(customerName, "Customer name");
        this.customerContact = Objects.requireNonNull(customerContact, "Customer contact is required.");
        this.heaterBrand = requiredText(heaterBrand, "Heater brand");
        this.heaterModel = requiredText(heaterModel, "Heater model");
        this.serviceType = Objects.requireNonNull(serviceType, "Service type is required.");
        this.reportedIssue = serviceType == ServiceType.REPAIR
                ? requiredText(reportedIssue, "Reported issue")
                : (reportedIssue == null || reportedIssue.isBlank() ? "" : reportedIssue.trim());
        this.receivedAt = Objects.requireNonNull(receivedAt, "Received timestamp is required.");
        this.status = WorkOrderStatus.RECEIVED;
    }

    public static WorkOrder restore(WorkOrderId id, String customerName,
                                      CustomerContact customerContact, String heaterBrand,
                                      String heaterModel, ServiceType serviceType, String reportedIssue,
                                      WorkOrderStatus status, Diagnosis diagnosis,
                                      Instant receivedAt, Instant completedAt, LifecycleVersion lifecycleVersion,
                                      WorkOrderStatus legacyStatus, CustomerDecision customerDecision) {
        WorkOrder order = new WorkOrder(id, customerName, customerContact, heaterBrand,
                heaterModel, serviceType, reportedIssue, receivedAt);
        order.status = Objects.requireNonNull(status, "Work order status is required.");
        order.diagnosis = diagnosis;
        order.completedAt = completedAt;
        order.lifecycleVersion = Objects.requireNonNull(lifecycleVersion, "Lifecycle version is required.");
        order.legacyStatus = legacyStatus;
        order.customerDecision = customerDecision;
        order.validateRestoredState();
        return order;
    }

    private boolean legacyWorkAlreadyStarted() {
        return lifecycleVersion == LifecycleVersion.LEGACY
                && (legacyStatus == WorkOrderStatus.IN_PROGRESS || legacyStatus == WorkOrderStatus.COMPLETED);
    }

    private void validateRestoredState() {
        if (lifecycleVersion == LifecycleVersion.V1 && legacyStatus != null
                || lifecycleVersion == LifecycleVersion.LEGACY && legacyStatus != WorkOrderStatus.RECEIVED
                && legacyStatus != WorkOrderStatus.IN_PROGRESS && legacyStatus != WorkOrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Invalid lifecycle provenance.");
        }
        if (legacyWorkAlreadyStarted()) {
            if (customerDecision != null || diagnosis == null
                    || (legacyStatus == WorkOrderStatus.COMPLETED && status != WorkOrderStatus.COMPLETED)
                    || (status != WorkOrderStatus.IN_PROGRESS && status != WorkOrderStatus.COMPLETED)
                    || (status == WorkOrderStatus.COMPLETED) != (completedAt != null)) {
                throw new IllegalArgumentException("Invalid legacy work state.");
            }
            // Preserve historical timestamps exactly, including existing inconsistencies.
            return;
        }
        if ((status == WorkOrderStatus.COMPLETED) != (completedAt != null)
                || completedAt != null && completedAt.isBefore(receivedAt)) {
            throw new IllegalArgumentException("Invalid completion timestamp.");
        }
        if (serviceType == ServiceType.MAINTENANCE) {
            if (diagnosis != null || customerDecision != null
                    || status == WorkOrderStatus.DIAGNOSIS || status == WorkOrderStatus.WAITING_CUSTOMER
                    || status == WorkOrderStatus.NOT_APPROVED) {
                throw new IllegalArgumentException("Maintenance cannot use the repair approval workflow.");
            }
            return;
        }
        boolean needsDiagnosis = status != WorkOrderStatus.RECEIVED && status != WorkOrderStatus.DIAGNOSIS;
        if (status == WorkOrderStatus.RECEIVED && diagnosis != null || needsDiagnosis && diagnosis == null) {
            throw new IllegalArgumentException("Invalid repair diagnosis state.");
        }
        CustomerDecision required = switch (status) {
            case WAITING_PARTS, IN_PROGRESS, COMPLETED -> CustomerDecision.APPROVED;
            case NOT_APPROVED -> CustomerDecision.REJECTED;
            default -> null;
        };
        if (customerDecision != required) throw new IllegalArgumentException("Invalid customer decision for state.");
    }

    private static String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    private void requireStatus(WorkOrderStatus expected) {
        if (status != expected) throw new InvalidWorkOrderStateException("This action is not valid in the current work order state.");
    }

    private void requireRepair() {
        if (serviceType != ServiceType.REPAIR) throw new InvalidWorkOrderStateException("This action requires a repair service.");
    }

    public void beginDiagnosis() {
        requireRepair();
        requireStatus(WorkOrderStatus.RECEIVED);
        status = WorkOrderStatus.DIAGNOSIS;
    }

    public void recordDiagnosis(Diagnosis diagnosis) {
        requireRepair();
        requireStatus(WorkOrderStatus.DIAGNOSIS);
        this.diagnosis = Objects.requireNonNull(diagnosis, "Diagnosis is required.");
    }

    public void completeDiagnosis() {
        requireRepair();
        requireStatus(WorkOrderStatus.DIAGNOSIS);
        if (diagnosis == null) throw new IllegalArgumentException("Record a diagnosis before completing diagnosis.");
        status = WorkOrderStatus.WAITING_CUSTOMER;
    }

    public void approveRepair(boolean partsAvailable) {
        requireRepair();
        requireStatus(WorkOrderStatus.WAITING_CUSTOMER);
        customerDecision = CustomerDecision.APPROVED;
        status = partsAvailable ? WorkOrderStatus.IN_PROGRESS : WorkOrderStatus.WAITING_PARTS;
    }

    public void rejectRepair() {
        requireRepair();
        requireStatus(WorkOrderStatus.WAITING_CUSTOMER);
        customerDecision = CustomerDecision.REJECTED;
        status = WorkOrderStatus.NOT_APPROVED;
    }

    public void waitForParts() {
        if (serviceType != ServiceType.MAINTENANCE)
            throw new InvalidWorkOrderStateException("For repairs, record parts availability with customer approval.");
        requireStatus(WorkOrderStatus.RECEIVED);
        status = WorkOrderStatus.WAITING_PARTS;
    }

    public void startWork() {
        boolean allowed = serviceType == ServiceType.MAINTENANCE && status == WorkOrderStatus.RECEIVED
                || status == WorkOrderStatus.WAITING_PARTS
                && (serviceType == ServiceType.MAINTENANCE || customerDecision == CustomerDecision.APPROVED);
        if (!allowed) throw new InvalidWorkOrderStateException("Work cannot start before its prerequisites are met.");
        status = WorkOrderStatus.IN_PROGRESS;
    }

    public void complete() {
        complete(Instant.now());
    }

    public void complete(Instant completedAt) {
        if (status != WorkOrderStatus.IN_PROGRESS) {
            throw new InvalidWorkOrderStateException("Only work in progress can be completed.");
        }

        Instant completionTimestamp = Objects.requireNonNull(completedAt, "Completion timestamp is required.");
        if (completionTimestamp.isBefore(receivedAt)) {
            throw new IllegalArgumentException("Completion timestamp cannot be before reception.");
        }
        this.status = WorkOrderStatus.COMPLETED;
        this.completedAt = completionTimestamp;
    }

    public LifecycleVersion lifecycleVersion() { return lifecycleVersion; }

    public WorkOrderStatus legacyStatus() { return legacyStatus; }

    public CustomerDecision customerDecision() { return customerDecision; }

    public WorkOrderId id() {
        return id;
    }

    public CustomerContact customerContact() {
        return customerContact;
    }

    public String customerName() { return customerName; }

    public String heaterBrand() { return heaterBrand; }

    public String heaterModel() { return heaterModel; }

    public ServiceType serviceType() { return serviceType; }

    public String reportedIssue() { return reportedIssue; }

    public Instant receivedAt() { return receivedAt; }

    public WorkOrderStatus status() {
        return status;
    }

    public Diagnosis diagnosis() {
        return diagnosis;
    }

    public Instant completedAt() { return completedAt; }
}
