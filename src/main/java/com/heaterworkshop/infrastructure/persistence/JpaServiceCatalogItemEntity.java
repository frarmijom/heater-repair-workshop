package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.service.ServiceCatalogItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.ColumnTransformer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_catalog_items")
public class JpaServiceCatalogItemEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 64) private String code;
    @ColumnTransformer(write = "upper(?)")
    @Column(name = "code_normalized", nullable = false, unique = true, length = 64) private String codeNormalized;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 1000) private String description;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal price;
    @Column(nullable = false) private boolean active;
    @Version @Column(nullable = false) private Long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected JpaServiceCatalogItemEntity() {}

    public JpaServiceCatalogItemEntity(ServiceCatalogItem value) {
        id = value.id();
        createdAt = value.createdAt();
        apply(value);
    }

    public void apply(ServiceCatalogItem value) {
        code = value.code();
        codeNormalized = value.code();
        name = value.name();
        description = value.description();
        price = value.price();
        active = value.active();
        updatedAt = value.updatedAt();
    }

    public ServiceCatalogItem toDomain() {
        return ServiceCatalogItem.restore(id, code, name, description, price, active, version, createdAt, updatedAt);
    }
}
