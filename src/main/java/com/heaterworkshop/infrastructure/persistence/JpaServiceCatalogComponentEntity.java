package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.service.ServiceCatalogComponent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "service_catalog_components")
@IdClass(JpaServiceCatalogComponentId.class)
public class JpaServiceCatalogComponentEntity {
    @Id
    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Id
    @Column(name = "inventory_item_id", nullable = false)
    private UUID inventoryItemId;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;

    protected JpaServiceCatalogComponentEntity() {}

    public JpaServiceCatalogComponentEntity(ServiceCatalogComponent value) {
        serviceId = value.serviceId();
        inventoryItemId = value.inventoryItemId();
        quantity = value.quantity();
    }

    public ServiceCatalogComponent toDomain() {
        return new ServiceCatalogComponent(serviceId, inventoryItemId, quantity);
    }
}
