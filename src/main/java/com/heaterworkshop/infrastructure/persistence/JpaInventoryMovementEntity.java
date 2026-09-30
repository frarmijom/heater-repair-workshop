package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.inventory.InventoryMovement;
import com.heaterworkshop.domain.inventory.InventoryMovementDirection;
import com.heaterworkshop.domain.inventory.InventoryMovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
public class JpaInventoryMovementEntity {
    @Id private UUID id;
    @Column(name = "item_id", nullable = false) private UUID itemId;
    @Column(name = "unit_id", nullable = false) private UUID unitId;
    @Column(nullable = false, length = 32) private String type;
    @Column(nullable = false, length = 16) private String direction;
    @Column(nullable = false, precision = 19, scale = 3) private BigDecimal quantity;
    @Column(name = "stock_before", nullable = false, precision = 19, scale = 3) private BigDecimal stockBefore;
    @Column(name = "stock_after", nullable = false, precision = 19, scale = 3) private BigDecimal stockAfter;
    @Column(name = "unit_cost_snapshot", nullable = false, precision = 19, scale = 4) private BigDecimal unitCostSnapshot;
    @Column(name = "request_id", nullable = false, unique = true, length = 128) private String requestId;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(nullable = false, length = 254) private String actor;
    @Column(length = 1000) private String reason;
    @Column(name = "reference_type", length = 80) private String referenceType;
    @Column(name = "reference_id", length = 160) private String referenceId;
    @Column(name = "work_order_id", length = 64) private String workOrderId;
    @Column(name = "reversal_of_movement_id") private UUID reversalOfMovementId;
    @Column(name = "sku_snapshot", nullable = false, length = 64) private String skuSnapshot;
    @Column(name = "item_name_snapshot", nullable = false, length = 160) private String itemNameSnapshot;
    @Column(name = "unit_name_snapshot", nullable = false, length = 120) private String unitNameSnapshot;
    @Column(name = "unit_symbol_snapshot", nullable = false, length = 16) private String unitSymbolSnapshot;

    protected JpaInventoryMovementEntity() {}

    public JpaInventoryMovementEntity(InventoryMovement value, UUID unitId) {
        id = value.id();
        this.unitId = unitId;
        itemId = value.itemId();
        type = value.type().name();
        direction = value.direction().name();
        quantity = value.quantity();
        stockBefore = value.stockBefore();
        stockAfter = value.stockAfter();
        unitCostSnapshot = value.unitCostSnapshot();
        requestId = value.requestId();
        occurredAt = value.occurredAt();
        actor = value.actor();
        reason = value.reason();
        referenceType = value.referenceType();
        referenceId = value.referenceId();
        workOrderId = value.workOrderId();
        reversalOfMovementId = value.reversalOfMovementId();
        skuSnapshot = value.skuSnapshot();
        itemNameSnapshot = value.itemNameSnapshot();
        unitNameSnapshot = value.unitNameSnapshot();
        unitSymbolSnapshot = value.unitSymbolSnapshot();
    }

    public InventoryMovement toDomain() {
        return InventoryMovement.restore(id, itemId, InventoryMovementType.valueOf(type),
                InventoryMovementDirection.valueOf(direction), quantity, stockBefore, stockAfter,
                unitCostSnapshot, requestId, occurredAt, actor, reason, referenceType, referenceId,
                workOrderId, reversalOfMovementId, skuSnapshot, itemNameSnapshot,
                unitNameSnapshot, unitSymbolSnapshot);
    }

    public UUID unitId() { return unitId; }
}