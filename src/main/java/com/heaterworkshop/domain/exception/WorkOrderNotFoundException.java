package com.heaterworkshop.domain.exception;

public class WorkOrderNotFoundException extends RuntimeException {
    public WorkOrderNotFoundException(String id) {
        super("Work order " + id + " was not found.");
    }
}
