-- HITO 07 / I5.1 - Servicios planificados por equipo
CREATE TABLE IF NOT EXISTS work_order_equipment_services (
    work_order_id VARCHAR(64) NOT NULL,
    equipment_id UUID NOT NULL,
    service_id UUID NOT NULL,
    PRIMARY KEY (work_order_id, equipment_id, service_id),

    CONSTRAINT work_order_equipment_services_work_order_fkey
        FOREIGN KEY (work_order_id)
        REFERENCES work_orders(order_id)
        ON DELETE CASCADE,

    CONSTRAINT work_order_equipment_services_equipment_fkey
        FOREIGN KEY (equipment_id)
        REFERENCES work_order_equipments(equipment_id)
        ON DELETE CASCADE,

    CONSTRAINT work_order_equipment_services_service_fkey
        FOREIGN KEY (service_id)
        REFERENCES service_catalog_items(id)
        ON DELETE RESTRICT
);
