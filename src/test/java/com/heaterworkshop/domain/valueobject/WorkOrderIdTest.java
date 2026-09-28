package com.heaterworkshop.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkOrderIdTest {

    @Test
    void acceptsTheBusinessFormat() {
        String id = "ORDER-550E8400-E29B-41D4-A716-446655440000";
        assertEquals(id, new WorkOrderId(id).value());
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new WorkOrderId(null));
    }

    @Test
    void rejectsAnInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> new WorkOrderId("ORDER-001"));
    }
}
