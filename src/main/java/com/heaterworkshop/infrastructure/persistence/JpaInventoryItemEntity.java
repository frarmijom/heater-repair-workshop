package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.inventory.InventoryItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.ColumnTransformer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class JpaInventoryItemEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 64) private String sku;
    @ColumnTransformer(write = "upper(?)")
    @Column(name = "sku_normalized", nullable = false, unique = true, length = 64) private String skuNormalized;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 1000) private String description;
    @Column(name = "category_id", nullable = false) private UUID categoryId;
    @Column(name = "unit_id", nullable = false) private UUID unitId;
    @Column(name = "stock_current", nullable = false, precision = 19, scale = 3) private BigDecimal stockCurrent;
    @Column(name = "stock_minimum", nullable = false, precision = 19, scale = 3) private BigDecimal stockMinimum;
    @Column(name = "reference_unit_cost", nullable = false, precision = 19, scale = 4) private BigDecimal referenceUnitCost;
    @Column(nullable = false) private boolean active;
    @Version @Column(nullable = false) private Long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected JpaInventoryItemEntity() {}

    public JpaInventoryItemEntity(InventoryItem value) {
        id = value.id();
        createdAt = value.createdAt();
        apply(value);
    }

    public void apply(InventoryItem value) {
        sku = value.sku();
        skuNormalized = value.sku();
        name = value.name();
        description = value.description();
        categoryId = value.categoryId();
        unitId = value.unitId();
        stockCurrent = value.stockCurrent();
        stockMinimum = value.stockMinimum();
        referenceUnitCost = value.referenceUnitCost();
        active = value.active();
        updatedAt = value.updatedAt();
    }

    public void applyStock(InventoryItem value) {
        stockCurrent = value.stockCurrent();
        updatedAt = value.updatedAt();
    }

    public InventoryItem toDomain() {
        return InventoryItem.restore(id, sku, name, description, categoryId, unitId, stockCurrent,
                stockMinimum, referenceUnitCost, active, version, createdAt, updatedAt);
    }
}