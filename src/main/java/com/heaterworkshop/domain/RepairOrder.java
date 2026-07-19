package com.heaterworkshop.domain;

public class RepairOrder {

    private final String orderId;
    private final String customerContact;
    private RepairStatus status = RepairStatus.RECEIVED;
    private String diagnosis;

    public RepairOrder(String orderId, String customerContact) {
        this.orderId = orderId;
        this.customerContact = customerContact;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerContact() {
        return customerContact;
    }

    public RepairStatus getStatus() {
        return status;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    void start(String diagnosis) {
        this.diagnosis = diagnosis;
        this.status = RepairStatus.IN_PROGRESS;
    }

    void complete() {
        this.status = RepairStatus.COMPLETED;
    }
}
