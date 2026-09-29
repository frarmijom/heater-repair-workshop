-- I3 item creation idempotency. Apply after inventory-stock-v2.sql.
BEGIN;
LOCK TABLE inventory_items, inventory_movements IN ACCESS SHARE MODE;
CREATE TABLE inventory_item_creation_requests (
    request_id varchar(128) PRIMARY KEY CHECK (request_id = btrim(request_id) AND length(request_id) > 0),
    item_id uuid NOT NULL UNIQUE REFERENCES inventory_items(id) ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED,
    payload_hash varchar(64) NOT NULL CHECK (payload_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamp(6) with time zone NOT NULL
);
COMMIT;