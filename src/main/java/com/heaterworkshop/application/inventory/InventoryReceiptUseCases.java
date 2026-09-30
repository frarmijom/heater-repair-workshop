package com.heaterworkshop.application.inventory;

import com.heaterworkshop.domain.inventory.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryReceiptUseCases {
    private final InventoryItemRepository items;
    private final InventoryMovementRepository movements;
    private final UnitOfMeasureRepository units;

    public InventoryReceiptUseCases(InventoryItemRepository items, InventoryMovementRepository movements,
                                    UnitOfMeasureRepository units) {
        this.items = items;
        this.movements = movements;
        this.units = units;
    }

    @Transactional
    public InventoryMovement receive(UUID itemId, BigDecimal quantity, BigDecimal unitCost, String requestId,
                                     String actor, String reason) {
        quantity = InventoryQuantity.exact(quantity, 3, "quantity");
        unitCost = InventoryQuantity.nonNegative(unitCost, 4, "unitCost");
        if (quantity.signum() <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        reason = CatalogText.required(reason, 1000);
        requestId = CatalogText.required(requestId, 128);

        InventoryMovement prior = movements.findByRequestId(requestId).orElse(null);
        if (prior != null) {
            if (prior.itemId().equals(itemId) && prior.type() == InventoryMovementType.ENTRY
                    && prior.direction() == InventoryMovementDirection.INCREASE
                    && prior.quantity().compareTo(quantity) == 0
                    && prior.unitCostSnapshot().compareTo(unitCost) == 0
                    && prior.actor().equals(actor) && reason.equals(prior.reason())) return prior;
            throw new CatalogConflictException("El requestId ya fue utilizado con otra operación.");
        }

        InventoryItem current = items.findByIdForUpdate(itemId).orElseThrow(CatalogNotFoundException::new);
        if (!current.active()) throw new CatalogConflictException("No se puede recibir stock en un artículo inactivo.");
        UnitOfMeasure unit = units.findByIdForUpdate(current.unitId()).orElseThrow(CatalogNotFoundException::new);
        if (!unit.allowsDecimal() && quantity.stripTrailingZeros().scale() > 0)
            throw new IllegalArgumentException("La unidad no admite cantidades fraccionarias.");

        InventoryMovement movement = InventoryMovement.create(current, unit, InventoryMovementType.ENTRY,
                InventoryMovementDirection.INCREASE, quantity, unitCost, requestId, actor, reason,
                "INVENTORY_RECEIPT", itemId.toString(), null, null);
        InventoryItem changed = current.applyMovement(movement, unit.allowsDecimal());
        items.saveStock(changed);
        return movements.append(movement);
    }

    @Transactional(readOnly = true)
    public List<InventoryMovement> history(UUID itemId) {
        if (items.findById(itemId).isEmpty()) throw new CatalogNotFoundException();
        return movements.findByItemId(itemId);
    }
}
