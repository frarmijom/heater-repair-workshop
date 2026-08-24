package com.heaterworkshop.domain.entity;

import com.heaterworkshop.domain.exception.InvalidRepairStateException;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.RepairOrderId;

public final class RepairOrder {

    private final RepairOrderId id;
    private final CustomerContact customerContact;
    private RepairStatus status;
    private Diagnosis diagnosis;

    public RepairOrder(RepairOrderId id, CustomerContact customerContact) {
        this.id = id;
        this.customerContact = customerContact;
        this.status = RepairStatus.RECEIVED;
    }

    public void start(Diagnosis diagnosis) {
        if (status != RepairStatus.RECEIVED) {
            throw new InvalidRepairStateException("Only received orders can be started.");
        }

        this.diagnosis = diagnosis;
        this.status = RepairStatus.IN_PROGRESS;
    }

    public void complete() {
        if (status != RepairStatus.IN_PROGRESS) {
            throw new InvalidRepairStateException("Only repairs in progress can be completed.");
        }

        this.status = RepairStatus.COMPLETED;
    }

    public RepairOrderId id() {
        return id;
    }

    public CustomerContact customerContact() {
        return customerContact;
    }

    public RepairStatus status() {
        return status;
    }

    public Diagnosis diagnosis() {
        return diagnosis;
    }
}
