package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.WorkOrderStatus;
import com.heaterworkshop.domain.entity.ServiceType;
import com.heaterworkshop.domain.entity.LifecycleVersion;
import com.heaterworkshop.domain.entity.CustomerDecision;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_orders")
public class JpaWorkOrderEntity {
    @Id
    @Column(name = "order_id", nullable = false, length = 64)
    private String id;
    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;
    @Column(name = "customer_contact", nullable = false, length = 16)
    private String customerContact;
    @Column(name = "heater_brand", length = 120)
    private String heaterBrand;
    @Column(name = "heater_model", length = 120)
    private String heaterModel;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "work_order_id", nullable = false)
    @OrderBy("position ASC")
    private List<JpaWorkOrderEquipmentEntity> equipments = new ArrayList<>();
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", length = 32)
    private ServiceType serviceType;
    @Column(name = "reported_issue", length = 2000)
    private String reportedIssue;
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private WorkOrderStatus status;
    @Column(length = 1000)
    private String diagnosis;
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_version", nullable = false, length = 16)
    private LifecycleVersion lifecycleVersion;
    @Enumerated(EnumType.STRING)
    @Column(name = "legacy_status", length = 32)
    private WorkOrderStatus legacyStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_decision", length = 16)
    private CustomerDecision customerDecision;

    protected JpaWorkOrderEntity() { }

    public JpaWorkOrderEntity(String id, String customerName, String customerContact,
                                String heaterBrand, String heaterModel, ServiceType serviceType, String reportedIssue,
                                WorkOrderStatus status, String diagnosis, Instant receivedAt,
                                Instant completedAt, LifecycleVersion lifecycleVersion,
                                WorkOrderStatus legacyStatus, CustomerDecision customerDecision,
                                List<JpaWorkOrderEquipmentEntity> equipments) {
        this.id = id;
        this.customerName = customerName;
        this.customerContact = customerContact;
        this.heaterBrand = heaterBrand;
        this.heaterModel = heaterModel;
        this.serviceType = serviceType;
        this.reportedIssue = reportedIssue;
        this.status = status;
        this.diagnosis = diagnosis;
        this.receivedAt = receivedAt;
        this.completedAt = completedAt;
        this.lifecycleVersion = lifecycleVersion;
        this.legacyStatus = legacyStatus;
        this.customerDecision = customerDecision;
        this.equipments = new ArrayList<>(equipments);
    }

    public LifecycleVersion getLifecycleVersion() { return lifecycleVersion; }
    public WorkOrderStatus getLegacyStatus() { return legacyStatus; }
    public CustomerDecision getCustomerDecision() { return customerDecision; }
    public String getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getCustomerContact() { return customerContact; }
    public String getHeaterBrand() { return heaterBrand; }
    public String getHeaterModel() { return heaterModel; }
    public List<JpaWorkOrderEquipmentEntity> getEquipments() { return List.copyOf(equipments); }
    public ServiceType getServiceType() { return serviceType; }
    public String getReportedIssue() { return reportedIssue; }
    public WorkOrderStatus getStatus() { return status; }
    public String getDiagnosis() { return diagnosis; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
