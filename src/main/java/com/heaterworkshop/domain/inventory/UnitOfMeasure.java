package com.heaterworkshop.domain.inventory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UnitOfMeasure(UUID id, String name, String symbol, boolean allowsDecimal, boolean active, long version,
                         Instant createdAt, Instant updatedAt) {
    public UnitOfMeasure {
        Objects.requireNonNull(id); Objects.requireNonNull(createdAt); Objects.requireNonNull(updatedAt);
        name = CatalogText.required(name, 120);
        symbol = CatalogText.required(symbol, 16);
        if (version < 0 || updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("Versión o fecha inválida.");
    }
    public static UnitOfMeasure create(String name, String symbol, boolean allowsDecimal) {
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        return new UnitOfMeasure(UUID.randomUUID(), name, symbol, allowsDecimal, true, 0, now, now);
    }
    public UnitOfMeasure edit(String name, String symbol, boolean allowsDecimal, boolean active) {
        return new UnitOfMeasure(id, name, symbol, allowsDecimal, active, version, createdAt, Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    }
}
