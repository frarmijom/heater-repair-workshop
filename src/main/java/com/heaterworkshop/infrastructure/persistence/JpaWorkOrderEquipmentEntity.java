package com.heaterworkshop.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "work_order_equipments")
public class JpaWorkOrderEquipmentEntity {
    @Id
    @Column(name = "equipment_id", nullable = false)
    private UUID id;

    @Column(name = "brand", nullable = false, length = 120)
    private String brand;

    @Column(name = "model", nullable = false, length = 120)
    private String model;

    @Column(name = "capacity", length = 80)
    private String capacity;

    @Column(name = "serial_number", length = 120)
    private String serialNumber;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "position", nullable = false)
    private int position;

    protected JpaWorkOrderEquipmentEntity() { }

    public JpaWorkOrderEquipmentEntity(UUID id, String brand, String model, String capacity,
                                       String serialNumber, String notes, int position) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.capacity = capacity;
        this.serialNumber = serialNumber;
        this.notes = notes;
        this.position = position;
    }

    public UUID getId() { return id; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getCapacity() { return capacity; }
    public String getSerialNumber() { return serialNumber; }
    public String getNotes() { return notes; }
    public int getPosition() { return position; }
}
