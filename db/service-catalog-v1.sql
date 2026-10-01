-- HITO 07 / I4.1 - Catálogo de servicios
CREATE TABLE IF NOT EXISTS service_catalog_items (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    code_normalized VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    price NUMERIC(19,4) NOT NULL CHECK (price >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT service_catalog_code_check CHECK (code = btrim(code) AND length(code) > 0),
    CONSTRAINT service_catalog_name_check CHECK (name = btrim(name) AND length(name) > 0),
    CONSTRAINT service_catalog_dates_check CHECK (updated_at >= created_at)
);
