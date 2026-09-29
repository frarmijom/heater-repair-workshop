package com.heaterworkshop.domain.exception;

public class InvalidWorkOrderStateException extends RuntimeException {

    public InvalidWorkOrderStateException(String message) {
        super(message);
    }
}
