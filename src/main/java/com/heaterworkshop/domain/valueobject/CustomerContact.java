package com.heaterworkshop.domain.valueobject;

public record CustomerContact(String value) {

    public CustomerContact {
        if (value == null || !value.matches("\\+[1-9][0-9]{7,14}")) {
            throw new IllegalArgumentException("Customer contact must be a valid international phone number.");
        }
    }
}
