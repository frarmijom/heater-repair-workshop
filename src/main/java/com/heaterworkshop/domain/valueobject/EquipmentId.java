package com.heaterworkshop.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record EquipmentId(UUID value) {
    public EquipmentId {
        Objects.requireNonNull(value, "Equipment ID is required.");
    }
}
