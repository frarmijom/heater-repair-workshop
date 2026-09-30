package com.heaterworkshop.domain.inventory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record InventoryCategory(UUID id, String name, boolean active, long version,
                         Instant createdAt, Instant updatedAt) {
    public InventoryCategory {
        Objects.requireNonNull(id); Objects.requireNonNull(createdAt); Objects.requireNonNull(updatedAt);
        name = CatalogText.required(name, 120);
        if (version < 0 || updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("Versión o fecha inválida.");
    }
    public static InventoryCategory create(String name) {
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        return new InventoryCategory(UUID.randomUUID(), name, true, 0, now, now);
    }
    public InventoryCategory edit(String name, boolean active) {
        return new InventoryCategory(id, name, active, version, createdAt, Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    }
}
