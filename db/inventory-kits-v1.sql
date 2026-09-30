-- HITO 07 / I2: kit classification and BOM. Apply once after inventory-items-v3.sql.
-- Existing inventory items remain STANDARD. This migration does not change stock.
BEGIN;
LOCK TABLE inventory_items IN ACCESS EXCLUSIVE MODE;
ALTER TABLE inventory_items
    ADD COLUMN item_type varchar(16) NOT NULL DEFAULT 'STANDARD'
    CHECK (item_type IN ('STANDARD','KIT'));
ALTER TABLE inventory_items ALTER COLUMN item_type DROP DEFAULT;
CREATE TABLE inventory_kit_components (
    kit_item_id uuid NOT NULL REFERENCES inventory_items(id) ON DELETE RESTRICT,
    component_item_id uuid NOT NULL REFERENCES inventory_items(id) ON DELETE RESTRICT,
    quantity numeric(19,3) NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (kit_item_id, component_item_id),
    CHECK (kit_item_id <> component_item_id)
);
CREATE INDEX inventory_kit_components_component_idx ON inventory_kit_components(component_item_id);
COMMIT;
