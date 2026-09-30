-- PostgreSQL. Run once after db/work-orders-v1.sql, with the application stopped and after taking a backup.
-- Backfills one equipment (position 1) for every existing work order from legacy heater columns.
-- Legacy heater_brand/heater_model are intentionally retained during the compatibility phase.
BEGIN;
LOCK TABLE work_orders IN ACCESS EXCLUSIVE MODE;
CREATE TABLE work_order_equipments (
    equipment_id uuid PRIMARY KEY,
    work_order_id varchar(64) NOT NULL REFERENCES work_orders(order_id) ON DELETE CASCADE,
    brand varchar(120) NOT NULL,
    model varchar(120) NOT NULL,
    capacity varchar(80),
    serial_number varchar(120),
    notes varchar(1000),
    position integer NOT NULL CHECK (position >= 1),
    CONSTRAINT work_order_equipments_order_position_unique UNIQUE (work_order_id, position)
);
CREATE INDEX idx_work_order_equipments_work_order_id ON work_order_equipments(work_order_id);
INSERT INTO work_order_equipments (equipment_id, work_order_id, brand, model, capacity, serial_number, notes, position)
SELECT (substr(md5(order_id),1,8)||'-'||substr(md5(order_id),9,4)||'-'||substr(md5(order_id),13,4)||'-'||substr(md5(order_id),17,4)||'-'||substr(md5(order_id),21,12))::uuid,
       order_id, heater_brand, heater_model, NULL, NULL, NULL, 1
FROM work_orders;
DO $$
DECLARE order_count bigint; equipment_count bigint;
BEGIN
    SELECT count(*) INTO order_count FROM work_orders;
    SELECT count(*) INTO equipment_count FROM work_order_equipments;
    IF equipment_count <> order_count THEN
        RAISE EXCEPTION 'Equipment backfill mismatch: % work orders, % equipments', order_count, equipment_count;
    END IF;
    IF EXISTS (
        SELECT 1 FROM work_orders wo
        LEFT JOIN work_order_equipments e ON e.work_order_id=wo.order_id AND e.position=1
        WHERE e.equipment_id IS NULL OR e.brand<>wo.heater_brand OR e.model<>wo.heater_model
    ) THEN RAISE EXCEPTION 'Equipment backfill verification failed'; END IF;
END $$;
COMMIT;
