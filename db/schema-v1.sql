-- Fresh PostgreSQL installation only. Existing installations use work-orders-v1.sql.
BEGIN;
CREATE TABLE work_orders (
    order_id varchar(64) PRIMARY KEY,
    customer_name varchar(200) NOT NULL,
    customer_contact varchar(16) NOT NULL,
    heater_brand varchar(120) NOT NULL,
    heater_model varchar(120) NOT NULL,
    service_type varchar(32) CHECK (service_type IN ('REPAIR','MAINTENANCE')),
    reported_issue varchar(2000),
    status varchar(32) NOT NULL CHECK (status IN ('RECEIVED','DIAGNOSIS','WAITING_CUSTOMER','WAITING_PARTS','IN_PROGRESS','COMPLETED','NOT_APPROVED')),
    diagnosis varchar(1000),
    received_at timestamp(6) with time zone NOT NULL,
    completed_at timestamp(6) with time zone,
    lifecycle_version varchar(16) NOT NULL CHECK (lifecycle_version IN ('LEGACY','V1')),
    legacy_status varchar(32),
    customer_decision varchar(16) CHECK (customer_decision IN ('APPROVED','REJECTED')),
    CONSTRAINT work_orders_lifecycle_check CHECK (
        (lifecycle_version = 'V1' AND legacy_status IS NULL) OR
        (lifecycle_version = 'LEGACY' AND legacy_status IS NOT NULL AND legacy_status IN ('RECEIVED','IN_PROGRESS','COMPLETED'))
    )
);
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
CREATE TABLE workshop_users (
    id varchar(64) PRIMARY KEY,
    email varchar(254) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    enabled boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);
COMMIT;
