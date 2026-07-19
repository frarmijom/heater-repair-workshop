package com.heaterworkshop.domain.exception;

public class InvalidRepairStateException extends RuntimeException {

    public InvalidRepairStateException(String message) {
        super(message);
    }
}
