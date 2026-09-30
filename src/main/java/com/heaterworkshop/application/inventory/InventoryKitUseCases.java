package com.heaterworkshop.application.inventory;

import com.heaterworkshop.domain.inventory.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryKitUseCases {
    private final InventoryItemRepository items;
    private final InventoryKitComponentRepository components;
    private final UnitOfMeasureRepository units;

    public InventoryKitUseCases(InventoryItemRepository items, InventoryKitComponentRepository components,
                                UnitOfMeasureRepository units) {
        this.items = items;
        this.components = components;
        this.units = units;
    }

    @Transactional(readOnly = true)
    public KitBom get(UUID kitItemId) {
        InventoryItem kit = requireKit(kitItemId);
        return new KitBom(kit, components.findByKitItemId(kitItemId));
    }

    @Transactional
    public KitBom replace(UUID kitItemId, List<ComponentInput> inputs) {
        InventoryItem kit = requireKit(kitItemId);
        List<InventoryKitComponent> next = inputs.stream().map(input -> {
            InventoryItem component = items.findById(input.componentItemId()).orElseThrow(CatalogNotFoundException::new);
            if (!component.active()) throw new CatalogConflictException("El BOM no puede usar componentes inactivos.");
            UnitOfMeasure unit = units.findById(component.unitId()).orElseThrow(CatalogNotFoundException::new);
            if (!unit.allowsDecimal() && input.quantity().stripTrailingZeros().scale() > 0)
                throw new IllegalArgumentException("La unidad del componente no admite cantidades fraccionarias.");
            return new InventoryKitComponent(kitItemId, component.id(), input.quantity());
        }).toList();
        components.replace(kitItemId, next);
        return new KitBom(kit, components.findByKitItemId(kitItemId));
    }

    private InventoryItem requireKit(UUID id) {
        InventoryItem item = items.findById(id).orElseThrow(CatalogNotFoundException::new);
        if (item.itemType() != InventoryItemType.KIT)
            throw new CatalogConflictException("El artículo no es un KIT.");
        return item;
    }

    public record ComponentInput(UUID componentItemId, BigDecimal quantity) {}
    public record KitBom(InventoryItem kit, List<InventoryKitComponent> components) {}
}
