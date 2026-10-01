package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.CustomerDecision;
import com.heaterworkshop.domain.entity.EquipmentIntakeRoute;
import com.heaterworkshop.domain.entity.EquipmentType;
import com.heaterworkshop.domain.entity.WorkOrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "work_order_equipments")
public class JpaWorkOrderEquipmentEntity {
    @Id
    @Column(name = "equipment_id", nullable = false)
    private UUID id;

    @Column(name = "brand", nullable = false, length = 120)
    private String brand;

    @Column(name = "model", nullable = false, length = 120)
    private String model;

    @Column(name = "capacity", length = 80)
    private String capacity;

    @Column(name = "serial_number", length = 120)
    private String serialNumber;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "position", nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type", length = 32)
    private EquipmentType equipmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "intake_route", length = 32)
    private EquipmentIntakeRoute intakeRoute;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", length = 32)
    private WorkOrderStatus lifecycleStatus;

    @Column(name = "reported_issue", length = 2000)
    private String reportedIssue;

    @Column(name = "diagnosis", length = 1000)
    private String diagnosis;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_decision", length = 16)
    private CustomerDecision customerDecision;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected JpaWorkOrderEquipmentEntity() { }

    // Compatibility constructor for LEGACY/V1 equipment.
    public JpaWorkOrderEquipmentEntity(UUID id, String brand, String model, String capacity,
                                       String serialNumber, String notes, int position) {
        this(id, brand, model, capacity, serialNumber, notes, position,
                null, null, null, null, null, null, null, null);
    }

    // Complete persistence representation for equipment lifecycle V2.
    public JpaWorkOrderEquipmentEntity(
            UUID id,
            String brand,
            String model,
            String capacity,
            String serialNumber,
            String notes,
            int position,
            EquipmentType equipmentType,
            EquipmentIntakeRoute intakeRoute,
            WorkOrderStatus lifecycleStatus,
            String reportedIssue,
            String diagnosis,
            CustomerDecision customerDecision,
            Instant receivedAt,
            Instant completedAt) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.capacity = capacity;
        this.serialNumber = serialNumber;
        this.notes = notes;
        this.position = position;
        this.equipmentType = equipmentType;
        this.intakeRoute = intakeRoute;
        this.lifecycleStatus = lifecycleStatus;
        this.reportedIssue = reportedIssue;
        this.diagnosis = diagnosis;
        this.customerDecision = customerDecision;
        this.receivedAt = receivedAt;
        this.completedAt = completedAt;
    }

    public UUID getId() { return id; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getCapacity() { return capacity; }
    public String getSerialNumber() { return serialNumber; }
    public String getNotes() { return notes; }
    public int getPosition() { return position; }

    public EquipmentType getEquipmentType() { return equipmentType; }
    public EquipmentIntakeRoute getIntakeRoute() { return intakeRoute; }
    public WorkOrderStatus getLifecycleStatus() { return lifecycleStatus; }
    public String getReportedIssue() { return reportedIssue; }
    public String getDiagnosis() { return diagnosis; }
    public CustomerDecision getCustomerDecision() { return customerDecision; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
