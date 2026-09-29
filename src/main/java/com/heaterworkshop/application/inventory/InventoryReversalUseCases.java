package com.heaterworkshop.application.inventory;

import com.heaterworkshop.domain.inventory.*;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class InventoryReversalUseCases {
    private final InventoryMovementRepository movements;
    private final RecordInventoryMovementUseCase recorder;

    public InventoryReversalUseCases(InventoryMovementRepository movements, RecordInventoryMovementUseCase recorder) {
        this.movements = movements;
        this.recorder = recorder;
    }

    public InventoryMovement reverse(UUID movementId, String requestId, String actor, String reason) {
        reason = CatalogText.required(reason, 1000);
        InventoryMovement original = movements.findById(movementId).orElseThrow(CatalogNotFoundException::new);
        if (!original.type().isReversible()) {
            throw new CatalogConflictException("El tipo de movimiento no admite reversión.");
        }
        InventoryMovementDirection opposite = original.direction() == InventoryMovementDirection.INCREASE
                ? InventoryMovementDirection.DECREASE : InventoryMovementDirection.INCREASE;
        return recorder.record(original.itemId(), InventoryMovementType.REVERSAL, opposite, original.quantity(),
                requestId, actor, reason, "INVENTORY_REVERSAL", movementId.toString(), null, movementId);
    }
}
