BEGIN;

-- V2 moves operational lifecycle ownership from work_orders
-- to each row in work_order_equipments.
ALTER TABLE work_orders
    ALTER COLUMN heater_brand DROP NOT NULL,
    ALTER COLUMN heater_model DROP NOT NULL,
    ALTER COLUMN reported_issue DROP NOT NULL,
    ALTER COLUMN status DROP NOT NULL;

ALTER TABLE work_orders
    ADD CONSTRAINT work_orders_v2_global_fields_check
    CHECK (
        (
            lifecycle_version IN ('LEGACY', 'V1')
            AND heater_brand IS NOT NULL
            AND heater_model IS NOT NULL
            AND reported_issue IS NOT NULL
            AND status IS NOT NULL
        )
        OR
        (
            lifecycle_version = 'V2'
            AND heater_brand IS NULL
            AND heater_model IS NULL
            AND service_type IS NULL
            AND reported_issue IS NULL
            AND status IS NULL
            AND diagnosis IS NULL
            AND completed_at IS NULL
            AND customer_decision IS NULL
        )
    );

COMMIT;
