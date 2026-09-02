package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.RepairStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "repair_orders")
public class JpaRepairOrderEntity {
    @Id
    @Column(name = "order_id", nullable = false, length = 64)
    private String id;
    @Column(name = "customer_contact", nullable = false, length = 16)
    private String customerContact;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RepairStatus status;
    @Column(length = 1000)
    private String diagnosis;

    protected JpaRepairOrderEntity() { }

    public JpaRepairOrderEntity(String id, String customerContact, RepairStatus status, String diagnosis) {
        this.id = id;
        this.customerContact = customerContact;
        this.status = status;
        this.diagnosis = diagnosis;
    }

    public String getId() { return id; }
    public String getCustomerContact() { return customerContact; }
    public RepairStatus getStatus() { return status; }
    public String getDiagnosis() { return diagnosis; }
}
