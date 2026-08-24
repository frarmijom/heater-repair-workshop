package com.heaterworkshop.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepairOrderIdTest {

    @Test
    void acceptsTheBusinessFormat() {
        assertEquals("ORDER-001", new RepairOrderId("ORDER-001").value());
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new RepairOrderId(null));
    }

    @Test
    void rejectsAnInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> new RepairOrderId("001"));
    }
}
