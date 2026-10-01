package com.heaterworkshop.domain.entity;

import com.heaterworkshop.domain.valueobject.EquipmentId;

import java.util.Objects;

public final class WorkOrderEquipment {
    private final EquipmentId id;
    private final EquipmentType type;
    private final String brand;
    private final String model;
    private final String capacity;
    private final String serialNumber;
    private final String notes;
    private final int position;
    private final WorkOrderEquipmentLifecycle lifecycle;

    public WorkOrderEquipment(EquipmentId id, String brand, String model, String capacity,
                              String serialNumber, String notes, int position) {
        this(id, EquipmentType.CALEFONT, brand, model, capacity, serialNumber, notes, position, null);
    }

    public WorkOrderEquipment(EquipmentId id, EquipmentType type, String brand, String model, String capacity,
                              String serialNumber, String notes, int position,
                              WorkOrderEquipmentLifecycle lifecycle) {
        this.id = Objects.requireNonNull(id, "Equipment ID is required.");
        this.type = Objects.requireNonNull(type, "Equipment type is required.");
        this.brand = requireText(brand, "Equipment brand is required.");
        this.model = requireText(model, "Equipment model is required.");
        this.capacity = normalizeOptional(capacity);
        this.serialNumber = normalizeOptional(serialNumber);
        this.notes = normalizeOptional(notes);
        if (position < 1) throw new IllegalArgumentException("Equipment position must be at least 1.");
        this.position = position;
        this.lifecycle = lifecycle;
    }

    public EquipmentId id() { return id; }
    public EquipmentType type() { return type; }
    public String brand() { return brand; }
    public String model() { return model; }
    public String capacity() { return capacity; }
    public String serialNumber() { return serialNumber; }
    public String notes() { return notes; }
    public int position() { return position; }
    public WorkOrderEquipmentLifecycle lifecycle() { return lifecycle; }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
