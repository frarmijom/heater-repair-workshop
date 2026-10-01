-- HITO 07 / I4.2 - Composición de servicios
CREATE TABLE IF NOT EXISTS service_catalog_components (
    service_id UUID NOT NULL,
    inventory_item_id UUID NOT NULL,
    quantity NUMERIC(19,3) NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (service_id, inventory_item_id),
    CONSTRAINT service_catalog_components_service_fkey
        FOREIGN KEY (service_id)
        REFERENCES service_catalog_items(id)
        ON DELETE CASCADE,
    CONSTRAINT service_catalog_components_inventory_item_fkey
        FOREIGN KEY (inventory_item_id)
        REFERENCES inventory_items(id)
        ON DELETE RESTRICT
);
