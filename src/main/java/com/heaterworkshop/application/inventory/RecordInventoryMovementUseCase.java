package com.heaterworkshop.application.inventory;

import com.heaterworkshop.domain.inventory.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Service
public class RecordInventoryMovementUseCase {
    private final InventoryItemRepository items;
    private final InventoryMovementRepository movements;
    private final UnitOfMeasureRepository units;

    public RecordInventoryMovementUseCase(InventoryItemRepository items,
                                         InventoryMovementRepository movements,
                                         UnitOfMeasureRepository units) {
        this.items = items;
        this.movements = movements;
        this.units = units;
    }

    @Transactional
    public InventoryMovement record(UUID itemId, InventoryMovementType type,
                                   InventoryMovementDirection direction, BigDecimal quantity,
                                   String requestId, String actor, String reason,
                                   String referenceType, String referenceId,
                                   String workOrderId, UUID reversalOfMovementId) {
                    requestId = CatalogText.required(requestId, 128);
                    quantity = InventoryQuantity.exact(quantity, 3, "quantity");
                    if (quantity.signum() <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        InventoryItem current = items.findByIdForUpdate(itemId).orElseThrow(CatalogNotFoundException::new);
        InventoryMovement existing = movements.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            if (sameRequest(existing, itemId, type, direction, quantity, actor, reason,
                    referenceType, referenceId, workOrderId, reversalOfMovementId)) return existing;
            throw new CatalogConflictException("El requestId ya fue utilizado con otra operación.");
        }
        InventoryMovement original = null;
        if (type == InventoryMovementType.REVERSAL) {
            original = movements.findById(Objects.requireNonNull(reversalOfMovementId)).orElseThrow(CatalogNotFoundException::new);
            if (!original.itemId().equals(itemId) || original.type() == InventoryMovementType.REVERSAL
                    || original.direction() == direction || original.quantity().compareTo(quantity) != 0) {
                throw new CatalogConflictException("La reversión debe compensar totalmente el movimiento original.");
            }
            if (movements.findByReversalOfMovementId(original.id()).isPresent())
                throw new CatalogConflictException("El movimiento ya fue revertido.");
        }
        if (!current.active() && type != InventoryMovementType.ADJUSTMENT && type != InventoryMovementType.REVERSAL)
            throw new CatalogConflictException("No se puede operar sobre un artículo inactivo.");
        UnitOfMeasure unit = units.findByIdForUpdate(current.unitId()).orElseThrow(CatalogNotFoundException::new);
        InventoryMovement movement = InventoryMovement.create(current, unit, type, direction, quantity,
            original == null ? current.referenceUnitCost() : original.unitCostSnapshot(),
            requestId, actor, reason, referenceType, referenceId,
                workOrderId, reversalOfMovementId);
        InventoryItem changed = current.applyMovement(movement, unit.allowsDecimal());
        items.saveStock(changed);
        return movements.append(movement);
    }

    private boolean sameRequest(InventoryMovement movement, UUID itemId, InventoryMovementType type,
                                InventoryMovementDirection direction, BigDecimal quantity, String actor,
                                String reason, String referenceType, String referenceId,
                                String workOrderId, UUID reversalOfMovementId) {
        return movement.itemId().equals(itemId) && movement.type() == type && movement.direction() == direction
                && movement.quantity().compareTo(quantity) == 0 && Objects.equals(movement.actor(), actor)
                && Objects.equals(movement.reason(), reason) && Objects.equals(movement.referenceType(), referenceType)
                && Objects.equals(movement.referenceId(), referenceId) && Objects.equals(movement.workOrderId(), workOrderId)
                && Objects.equals(movement.reversalOfMovementId(), reversalOfMovementId);
    }
}