package com.heaterworkshop.domain;

import com.heaterworkshop.domain.exception.InvalidRepairStateException;

public class RepairWorkflow {

    private final RepairValidator validator;

    public RepairWorkflow(RepairValidator validator) {
        this.validator = validator;
    }

    public void startRepair(RepairOrder order, String diagnosis) {
        if (order.getStatus() != RepairStatus.RECEIVED) {
            throw new InvalidRepairStateException("Only received orders can be started.");
        }
        validator.validateDiagnosis(diagnosis);
        order.start(diagnosis);
    }

    public void completeRepair(RepairOrder order) {
        if (order.getStatus() != RepairStatus.IN_PROGRESS) {
            throw new InvalidRepairStateException("Only repairs in progress can be completed.");
        }
        order.complete();
    }
}
