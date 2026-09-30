package com.heaterworkshop.domain.entity;

import com.heaterworkshop.domain.exception.InvalidWorkOrderStateException;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class WorkOrder {

    private final WorkOrderId id;
    private final String customerName;
    private final CustomerContact customerContact;
    private final List<WorkOrderEquipment> equipments;
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
                       List<WorkOrderEquipment> equipments, ServiceType serviceType, String reportedIssue,
                       Instant receivedAt) {
        this.id = Objects.requireNonNull(id, "Work order id is required.");
        this.customerName = requiredText(customerName, "Customer name");
        this.customerContact = Objects.requireNonNull(customerContact, "Customer contact is required.");
        this.equipments = validateEquipments(equipments);
        this.serviceType = Objects.requireNonNull(serviceType, "Service type is required.");
        this.reportedIssue = serviceType == ServiceType.REPAIR
                ? requiredText(reportedIssue, "Reported issue")
                : (reportedIssue == null || reportedIssue.isBlank() ? "" : reportedIssue.trim());
        this.receivedAt = Objects.requireNonNull(receivedAt, "Received timestamp is required.");
        this.status = WorkOrderStatus.RECEIVED;
    }

    /**
     * Transitional compatibility constructor for the single-equipment API/persistence model.
     * It will be removed once I1 migrates all callers to the equipment collection.
     */
    public WorkOrder(WorkOrderId id, String customerName, CustomerContact customerContact,
                     String heaterBrand, String heaterModel, ServiceType serviceType, String reportedIssue,
                     Instant receivedAt) {
        this(id, customerName, customerContact,
                List.of(legacyEquipment(id, heaterBrand, heaterModel)),
                serviceType, reportedIssue, receivedAt);
    }

    public static WorkOrder restore(WorkOrderId id, String customerName,
                                    CustomerContact customerContact, List<WorkOrderEquipment> equipments,
                                    ServiceType serviceType, String reportedIssue,
                                    WorkOrderStatus status, Diagnosis diagnosis,
                                    Instant receivedAt, Instant completedAt, LifecycleVersion lifecycleVersion,
                                    WorkOrderStatus legacyStatus, CustomerDecision customerDecision) {
        WorkOrder order = new WorkOrder(id, customerName, customerContact, equipments,
                serviceType, reportedIssue, receivedAt);
        order.status = Objects.requireNonNull(status, "Work order status is required.");
        order.diagnosis = diagnosis;
        order.completedAt = completedAt;
        order.lifecycleVersion = Objects.requireNonNull(lifecycleVersion, "Lifecycle version is required.");
        order.legacyStatus = legacyStatus;
        order.customerDecision = customerDecision;
        order.validateRestoredState();
        return order;
    }

    /** Transitional restore overload for rows that still store heater_brand/heater_model on work_orders. */
    public static WorkOrder restore(WorkOrderId id, String customerName,
                                    CustomerContact customerContact, String heaterBrand,
                                    String heaterModel, ServiceType serviceType, String reportedIssue,
                                    WorkOrderStatus status, Diagnosis diagnosis,
                                    Instant receivedAt, Instant completedAt, LifecycleVersion lifecycleVersion,
                                    WorkOrderStatus legacyStatus, CustomerDecision customerDecision) {
        return restore(id, customerName, customerContact,
                List.of(legacyEquipment(id, heaterBrand, heaterModel)),
                serviceType, reportedIssue, status, diagnosis, receivedAt, completedAt,
                lifecycleVersion, legacyStatus, customerDecision);
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

    private static List<WorkOrderEquipment> validateEquipments(List<WorkOrderEquipment> equipments) {
        Objects.requireNonNull(equipments, "Work order equipments are required.");
        if (equipments.isEmpty()) {
            throw new IllegalArgumentException("Work order must contain at least one equipment.");
        }
        List<WorkOrderEquipment> copy = new ArrayList<>(equipments.size());
        Set<Integer> positions = new HashSet<>();
        for (WorkOrderEquipment equipment : equipments) {
            WorkOrderEquipment value = Objects.requireNonNull(equipment, "Work order equipment is required.");
            if (!positions.add(value.position())) {
                throw new IllegalArgumentException("Equipment positions must be unique within a work order.");
            }
            copy.add(value);
        }
        copy.sort(Comparator.comparingInt(WorkOrderEquipment::position));
        return List.copyOf(copy);
    }

    private static WorkOrderEquipment legacyEquipment(WorkOrderId workOrderId, String brand, String model) {
        Objects.requireNonNull(workOrderId, "Work order id is required.");
        UUID equipmentUuid = UUID.nameUUIDFromBytes(
                ("work-order-equipment:" + workOrderId.value()).getBytes(StandardCharsets.UTF_8));
        return new WorkOrderEquipment(new EquipmentId(equipmentUuid), brand, model, null, null, null, 1);
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

    public List<WorkOrderEquipment> equipments() { return equipments; }

    /** Transitional projection for callers that still assume a single equipment. */
    public String heaterBrand() { return equipments.get(0).brand(); }

    /** Transitional projection for callers that still assume a single equipment. */
    public String heaterModel() { return equipments.get(0).model(); }

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
