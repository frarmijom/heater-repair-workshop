-- HITO 07 / I3
-- Extiende los movimientos de inventario para soportar ensamblaje de kits.

BEGIN;

ALTER TABLE inventory_movements
    DROP CONSTRAINT IF EXISTS inventory_movements_type_check;

ALTER TABLE inventory_movements
    ADD CONSTRAINT inventory_movements_type_check
    CHECK (
        type IN (
            'INITIAL_ENTRY',
            'ENTRY',
            'WORK_ORDER_CONSUMPTION',
            'ADJUSTMENT',
            'REVERSAL',
            'KIT_ASSEMBLY_CONSUMPTION',
            'KIT_ASSEMBLY_PRODUCTION'
        )
    );

ALTER TABLE inventory_movements
    DROP CONSTRAINT IF EXISTS inventory_movement_direction_check;

ALTER TABLE inventory_movements
    ADD CONSTRAINT inventory_movement_direction_check
    CHECK (
        (
            type IN (
                'INITIAL_ENTRY',
                'ENTRY',
                'KIT_ASSEMBLY_PRODUCTION'
            )
            AND direction = 'INCREASE'
        )
        OR
        (
            type IN (
                'WORK_ORDER_CONSUMPTION',
                'KIT_ASSEMBLY_CONSUMPTION'
            )
            AND direction = 'DECREASE'
        )
        OR
        type IN ('ADJUSTMENT', 'REVERSAL')
    );

COMMIT;
