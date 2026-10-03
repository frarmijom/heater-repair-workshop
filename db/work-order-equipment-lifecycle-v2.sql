BEGIN;

ALTER TABLE work_order_equipments
    ADD COLUMN equipment_type varchar(32),
    ADD COLUMN intake_route varchar(32),
    ADD COLUMN lifecycle_status varchar(32),
    ADD COLUMN reported_issue varchar(2000),
    ADD COLUMN diagnosis varchar(1000),
    ADD COLUMN customer_decision varchar(16),
    ADD COLUMN received_at timestamptz,
    ADD COLUMN completed_at timestamptz;

ALTER TABLE work_order_equipments
    ADD CONSTRAINT work_order_equipments_type_check
        CHECK (equipment_type IS NULL OR equipment_type = 'CALEFONT'),
    ADD CONSTRAINT work_order_equipments_intake_route_check
        CHECK (intake_route IS NULL OR intake_route IN ('DIRECT_SERVICE', 'DIAGNOSIS_REQUIRED')),
    ADD CONSTRAINT work_order_equipments_lifecycle_status_check
        CHECK (lifecycle_status IS NULL OR lifecycle_status IN (
          'RECEIVED','DIAGNOSIS','WAITING_CUSTOMER','WAITING_PARTS',
          'IN_PROGRESS','COMPLETED','NOT_APPROVED')),
    ADD CONSTRAINT work_order_equipments_decision_check
        CHECK (customer_decision IS NULL OR customer_decision IN ('APPROVED','REJECTED'));

ALTER TABLE work_orders DROP CONSTRAINT work_orders_lifecycle_check;

ALTER TABLE work_orders
    ADD CONSTRAINT work_orders_lifecycle_check
        CHECK (
            lifecycle_version IN ('LEGACY', 'V1', 'V2')
            AND (
                (lifecycle_version = 'LEGACY'
                 AND legacy_status IS NOT NULL
                 AND legacy_status IN ('RECEIVED','IN_PROGRESS','COMPLETED'))
                OR
                (lifecycle_version IN ('V1','V2')
                 AND legacy_status IS NULL)
            )
        );

COMMIT;
