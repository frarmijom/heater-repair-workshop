package com.heaterworkshop.infrastructure.persistence;
import com.heaterworkshop.domain.inventory.*;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="inventory_units")
public class JpaUnitOfMeasureEntity {
    @Id private UUID id;
    @Column(nullable=false, length=120) private String name;
    @ColumnTransformer(write="lower(?)")
    @Column(name="name_normalized", nullable=false, unique=true, length=240) private String nameNormalized;
    
    @Column(nullable=false, length=16) private String symbol;
    @ColumnTransformer(write="lower(?)")
    @Column(name="symbol_normalized", nullable=false, unique=true, length=32) private String symbolNormalized;
    @Column(name="allows_decimal", nullable=false) private boolean allowsDecimal;

    @Column(nullable=false) private boolean active;
    @Version @Column(nullable=false) private Long version;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected JpaUnitOfMeasureEntity() {}
    public JpaUnitOfMeasureEntity(UnitOfMeasure value) { id=value.id(); createdAt=value.createdAt(); apply(value); }
    public void apply(UnitOfMeasure value) {
        name=value.name(); nameNormalized=name; symbol=value.symbol(); symbolNormalized=value.symbol(); allowsDecimal=value.allowsDecimal();
        active=value.active(); updatedAt=value.updatedAt();
    }
    public UnitOfMeasure toDomain() { return new UnitOfMeasure(id,name, symbol, allowsDecimal,active,version,createdAt,updatedAt); }
}
