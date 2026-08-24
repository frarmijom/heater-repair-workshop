package com.heaterworkshop.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerContactTest {

    @Test
    void acceptsAnInternationalPhoneNumber() {
        assertEquals("+56911112222", new CustomerContact("+56911112222").value());
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new CustomerContact(null));
    }

    @Test
    void rejectsAnInvalidPhoneNumber() {
        assertThrows(IllegalArgumentException.class, () -> new CustomerContact("911112222"));
    }
}
