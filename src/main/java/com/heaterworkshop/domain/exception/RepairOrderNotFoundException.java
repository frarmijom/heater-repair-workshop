package com.heaterworkshop.domain.exception;

public class RepairOrderNotFoundException extends RuntimeException {
    public RepairOrderNotFoundException(String id) {
        super("Repair order " + id + " was not found.");
    }
}
