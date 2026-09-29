-- I1 only. Apply after the Work Orders v1 baseline with psql -v ON_ERROR_STOP=1.
BEGIN;
-- Fail closed if the required baseline is missing; never create substitute baseline data.
LOCK TABLE work_orders, workshop_users IN ACCESS SHARE MODE;
CREATE TABLE inventory_categories (
    id uuid PRIMARY KEY,
    name varchar(120) NOT NULL CHECK (name = btrim(name) AND length(name) > 0 AND name !~ '[[:cntrl:]]' AND name !~ '  '),
    name_normalized varchar(240) NOT NULL UNIQUE CHECK (name_normalized = lower(name)),
    active boolean NOT NULL,
    version bigint NOT NULL CHECK (version >= 0),
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL CHECK (updated_at >= created_at)
);
CREATE TABLE inventory_units (
    id uuid PRIMARY KEY,
    name varchar(120) NOT NULL CHECK (name = btrim(name) AND length(name) > 0 AND name !~ '[[:cntrl:]]' AND name !~ '  '),
    name_normalized varchar(240) NOT NULL UNIQUE CHECK (name_normalized = lower(name)),
    symbol varchar(16) NOT NULL CHECK (symbol = btrim(symbol) AND length(symbol) > 0 AND symbol !~ '[[:cntrl:]]' AND symbol !~ '  '),
    symbol_normalized varchar(32) NOT NULL UNIQUE CHECK (symbol_normalized = lower(symbol)),
    allows_decimal boolean NOT NULL,
    active boolean NOT NULL,
    version bigint NOT NULL CHECK (version >= 0),
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL CHECK (updated_at >= created_at)
);
COMMIT;
