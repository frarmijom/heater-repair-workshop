package com.heaterworkshop.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepairOrderIdTest {

    @Test
    void acceptsTheBusinessFormat() {
        String id = "ORDER-550E8400-E29B-41D4-A716-446655440000";
        assertEquals(id, new RepairOrderId(id).value());
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new RepairOrderId(null));
    }

    @Test
    void rejectsAnInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> new RepairOrderId("ORDER-001"));
    }
}
