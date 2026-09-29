package com.heaterworkshop.infrastructure.persistence;
import com.heaterworkshop.domain.inventory.*;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="inventory_categories")
public class JpaInventoryCategoryEntity {
    @Id private UUID id;
    @Column(nullable=false, length=120) private String name;
    @ColumnTransformer(write="lower(?)")
    @Column(name="name_normalized", nullable=false, unique=true, length=240) private String nameNormalized;
    
    @Column(nullable=false) private boolean active;
    @Version @Column(nullable=false) private Long version;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected JpaInventoryCategoryEntity() {}
    public JpaInventoryCategoryEntity(InventoryCategory value) { id=value.id(); createdAt=value.createdAt(); apply(value); }
    public void apply(InventoryCategory value) {
        name=value.name(); nameNormalized=name; 
        active=value.active(); updatedAt=value.updatedAt();
    }
    public InventoryCategory toDomain() { return new InventoryCategory(id,name,active,version,createdAt,updatedAt); }
}
