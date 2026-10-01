package com.heaterworkshop.domain.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record ServiceCatalogItem(UUID id, String code, String name, String description,
                                 BigDecimal price, boolean active, long version,
                                 Instant createdAt, Instant updatedAt) {
    public ServiceCatalogItem {
        Objects.requireNonNull(id);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        code = normalizeCode(code);
        name = requiredText(name, 160, "nombre");
        description = description == null || description.isBlank()
                ? null : requiredText(description, 1000, "descripción");
        price = money(price);
        if (version < 0 || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Versión o fecha inválida.");
        }
    }

    public static ServiceCatalogItem create(String code, String name, String description, BigDecimal price) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return new ServiceCatalogItem(UUID.randomUUID(), code, name, description, price, true, 0, now, now);
    }

    public static ServiceCatalogItem restore(UUID id, String code, String name, String description,
                                             BigDecimal price, boolean active, long version,
                                             Instant createdAt, Instant updatedAt) {
        return new ServiceCatalogItem(id, code, name, description, price, active, version, createdAt, updatedAt);
    }

    public ServiceCatalogItem edit(String code, String name, String description, BigDecimal price, boolean active) {
        return restore(id, code, name, description, price, active, version, createdAt,
                Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    private static String normalizeCode(String value) {
        String normalized = Normalizer.normalize(requiredText(value, 64, "código"), Normalizer.Form.NFC)
                .toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9][A-Z0-9._/-]{0,63}")) {
            throw new IllegalArgumentException("Código de servicio inválido.");
        }
        return normalized;
    }

    private static String requiredText(String value, int max, String field) {
        if (value == null) throw new IllegalArgumentException("El " + field + " es obligatorio.");
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty() || normalized.length() > max || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("El " + field + " es inválido.");
        }
        return normalized;
    }

    private static BigDecimal money(BigDecimal value) {
        Objects.requireNonNull(value, "price");
        if (value.signum() < 0 || value.scale() > 4) {
            throw new IllegalArgumentException("Precio inválido.");
        }
        return value.setScale(4);
    }
}
