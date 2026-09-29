package com.heaterworkshop.application.inventory;

import com.heaterworkshop.domain.inventory.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class InventoryAdjustmentUseCases {
    private final RecordInventoryMovementUseCase recorder;

    public InventoryAdjustmentUseCases(RecordInventoryMovementUseCase recorder) {
        this.recorder = recorder;
    }

    public InventoryMovement adjust(UUID itemId, InventoryMovementDirection direction, BigDecimal quantity,
                                    String requestId, String actor, String reason) {
        if (direction == null) throw new IllegalArgumentException("La dirección del ajuste es obligatoria.");
        reason = CatalogText.required(reason, 1000);
        return recorder.record(itemId, InventoryMovementType.ADJUSTMENT, direction, quantity,
                requestId, actor, reason, "INVENTORY_ADJUSTMENT", itemId.toString(), null, null);
    }
}
