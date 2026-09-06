package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.RepairStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "repair_orders")
public class JpaRepairOrderEntity {
    @Id
    @Column(name = "order_id", nullable = false, length = 64)
    private String id;
    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;
    @Column(name = "customer_contact", nullable = false, length = 16)
    private String customerContact;
    @Column(name = "heater_brand", nullable = false, length = 120)
    private String heaterBrand;
    @Column(name = "heater_model", nullable = false, length = 120)
    private String heaterModel;
    @Column(name = "reported_issue", nullable = false, length = 2000)
    private String reportedIssue;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RepairStatus status;
    @Column(length = 1000)
    private String diagnosis;
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    protected JpaRepairOrderEntity() { }

    public JpaRepairOrderEntity(String id, String customerName, String customerContact,
                                String heaterBrand, String heaterModel, String reportedIssue,
                                RepairStatus status, String diagnosis, Instant receivedAt,
                                Instant completedAt) {
        this.id = id;
        this.customerName = customerName;
        this.customerContact = customerContact;
        this.heaterBrand = heaterBrand;
        this.heaterModel = heaterModel;
        this.reportedIssue = reportedIssue;
        this.status = status;
        this.diagnosis = diagnosis;
        this.receivedAt = receivedAt;
        this.completedAt = completedAt;
    }

    public String getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getCustomerContact() { return customerContact; }
    public String getHeaterBrand() { return heaterBrand; }
    public String getHeaterModel() { return heaterModel; }
    public String getReportedIssue() { return reportedIssue; }
    public RepairStatus getStatus() { return status; }
    public String getDiagnosis() { return diagnosis; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
