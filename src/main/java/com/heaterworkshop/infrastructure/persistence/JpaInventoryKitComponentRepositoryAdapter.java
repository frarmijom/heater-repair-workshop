package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.inventory.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Repository
@Transactional
public class JpaInventoryKitComponentRepositoryAdapter implements InventoryKitComponentRepository {
    private final SpringDataInventoryKitComponentRepository repository;
    private final InventoryItemRepository items;
    public JpaInventoryKitComponentRepositoryAdapter(SpringDataInventoryKitComponentRepository repository, InventoryItemRepository items) { this.repository=repository; this.items=items; }
    @Override @Transactional(readOnly=true)
    public List<InventoryKitComponent> findByKitItemId(UUID kitItemId) { return repository.findByKitItemId(kitItemId).stream().map(JpaInventoryKitComponentEntity::toDomain).toList(); }
    @Override public void replace(UUID kitItemId, List<InventoryKitComponent> components) {
        InventoryItem kit=items.findById(kitItemId).orElseThrow(CatalogNotFoundException::new);
        if (kit.itemType()!=InventoryItemType.KIT) throw new IllegalArgumentException("El BOM solo puede pertenecer a un artículo KIT.");
        Set<UUID> seen=new HashSet<>();
        for (InventoryKitComponent c: components) {
            if (!kitItemId.equals(c.kitItemId())) throw new IllegalArgumentException("Componente asociado a otro kit.");
            if (!seen.add(c.componentItemId())) throw new IllegalArgumentException("Un componente no puede repetirse en el BOM.");
            InventoryItem component=items.findById(c.componentItemId()).orElseThrow(CatalogNotFoundException::new);
            if (!component.active()) throw new IllegalArgumentException("El BOM no puede usar componentes inactivos.");
        }
        repository.deleteByKitItemId(kitItemId);
        repository.flush();
        repository.saveAllAndFlush(components.stream().map(JpaInventoryKitComponentEntity::new).toList());
    }
}
