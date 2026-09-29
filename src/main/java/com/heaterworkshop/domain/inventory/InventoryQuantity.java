package com.heaterworkshop.domain.inventory;

import java.math.BigDecimal;

public final class InventoryQuantity {
    private InventoryQuantity() {}

    public static BigDecimal exact(BigDecimal value, int scale, String field) {
        if (value == null || value.scale() > scale || value.precision() - value.scale() > 19 - scale) {
            throw new IllegalArgumentException(field + " excede la precisión permitida.");
        }
        return value;
    }

    public static BigDecimal nonNegative(BigDecimal value, int scale, String field) {
        BigDecimal exact = exact(value, scale, field);
        if (exact.signum() < 0) throw new IllegalArgumentException(field + " no puede ser negativo.");
        return exact;
    }
}