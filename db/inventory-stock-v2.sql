-- I2 stock core. Apply after Work Orders v1 and inventory-catalogs-v1.sql.
BEGIN;
LOCK TABLE work_orders, workshop_users, inventory_categories, inventory_units IN ACCESS SHARE MODE;
CREATE TABLE inventory_items (
    id uuid PRIMARY KEY,
    sku varchar(64) NOT NULL CHECK (sku = upper(btrim(sku)) AND sku ~ '^[A-Z0-9][A-Z0-9._/-]{0,63}$'),
    sku_normalized varchar(64) NOT NULL UNIQUE CHECK (sku_normalized = upper(sku)),
    name varchar(160) NOT NULL CHECK (name = btrim(name) AND length(name) > 0),
    description varchar(1000),
    category_id uuid NOT NULL REFERENCES inventory_categories(id) ON DELETE RESTRICT,
    unit_id uuid NOT NULL REFERENCES inventory_units(id) ON DELETE RESTRICT,
    stock_current numeric(19,3) NOT NULL CHECK (stock_current >= 0),
    stock_minimum numeric(19,3) NOT NULL CHECK (stock_minimum >= 0),
    reference_unit_cost numeric(19,4) NOT NULL CHECK (reference_unit_cost >= 0),
    active boolean NOT NULL,
    version bigint NOT NULL CHECK (version >= 0),
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL CHECK (updated_at >= created_at)
);
CREATE TABLE inventory_movements (
    id uuid PRIMARY KEY,
    item_id uuid NOT NULL REFERENCES inventory_items(id) ON DELETE RESTRICT,
    unit_id uuid NOT NULL REFERENCES inventory_units(id) ON DELETE RESTRICT,
    type varchar(32) NOT NULL CHECK (type IN ('INITIAL_ENTRY','ENTRY','WORK_ORDER_CONSUMPTION','ADJUSTMENT','REVERSAL')),
    direction varchar(16) NOT NULL CHECK (direction IN ('INCREASE','DECREASE')),
    quantity numeric(19,3) NOT NULL CHECK (quantity > 0),
    stock_before numeric(19,3) NOT NULL CHECK (stock_before >= 0),
    stock_after numeric(19,3) NOT NULL CHECK (stock_after >= 0),
    unit_cost_snapshot numeric(19,4) NOT NULL CHECK (unit_cost_snapshot >= 0),
    request_id varchar(128) NOT NULL UNIQUE CHECK (request_id = btrim(request_id) AND length(request_id) > 0),
    occurred_at timestamp(6) with time zone NOT NULL,
    actor varchar(254) NOT NULL CHECK (actor = btrim(actor) AND length(actor) > 0),
    reason varchar(1000),
    reference_type varchar(80),
    reference_id varchar(160),
    work_order_id varchar(64) REFERENCES work_orders(order_id) ON DELETE RESTRICT,
    reversal_of_movement_id uuid REFERENCES inventory_movements(id) ON DELETE RESTRICT,
    sku_snapshot varchar(64) NOT NULL,
    item_name_snapshot varchar(160) NOT NULL,
    unit_name_snapshot varchar(120) NOT NULL,
    unit_symbol_snapshot varchar(16) NOT NULL,
    CONSTRAINT inventory_movement_delta_check CHECK (
        (direction = 'INCREASE' AND stock_after = stock_before + quantity) OR
        (direction = 'DECREASE' AND stock_after = stock_before - quantity)
    ),
    CONSTRAINT inventory_movement_direction_check CHECK (
        (type IN ('INITIAL_ENTRY','ENTRY') AND direction = 'INCREASE') OR
        (type = 'WORK_ORDER_CONSUMPTION' AND direction = 'DECREASE') OR
        type IN ('ADJUSTMENT','REVERSAL')
    ),
    CONSTRAINT inventory_movement_reference_check CHECK ((reference_type IS NULL) = (reference_id IS NULL)),
    CONSTRAINT inventory_movement_work_order_check CHECK
        ((type = 'WORK_ORDER_CONSUMPTION') = (work_order_id IS NOT NULL)),
    CONSTRAINT inventory_movement_reversal_check CHECK
        ((type = 'REVERSAL') = (reversal_of_movement_id IS NOT NULL))
);
CREATE INDEX inventory_movements_item_occurred_idx ON inventory_movements(item_id, occurred_at, id);
CREATE UNIQUE INDEX inventory_movements_single_reversal_idx
    ON inventory_movements(reversal_of_movement_id) WHERE reversal_of_movement_id IS NOT NULL;
CREATE FUNCTION reject_inventory_movement_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Inventory movements are immutable';
END;
$$;
CREATE TRIGGER inventory_movements_immutable
    BEFORE UPDATE OR DELETE ON inventory_movements
    FOR EACH ROW EXECUTE FUNCTION reject_inventory_movement_mutation();
COMMIT;