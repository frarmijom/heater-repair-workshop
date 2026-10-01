package com.heaterworkshop.application.inventory;

import com.heaterworkshop.domain.inventory.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InventoryKitAssemblyUseCases {
    private final InventoryItemRepository items;
    private final InventoryKitComponentRepository components;
    private final InventoryMovementRepository movements;
    private final UnitOfMeasureRepository units;

    public InventoryKitAssemblyUseCases(InventoryItemRepository items,
                                        InventoryKitComponentRepository components,
                                        InventoryMovementRepository movements,
                                        UnitOfMeasureRepository units) {
        this.items = items;
        this.components = components;
        this.movements = movements;
        this.units = units;
    }

    @Transactional
    public AssemblyResult assemble(UUID kitItemId, BigDecimal quantity,
                                   String requestId, String actor, String reason) {
        quantity = InventoryQuantity.exact(quantity, 3, "quantity");
        if (quantity.signum() <= 0)
            throw new IllegalArgumentException("La cantidad a ensamblar debe ser mayor que cero.");

        requestId = CatalogText.required(requestId, 128);
        actor = CatalogText.required(actor, 254);
        reason = CatalogText.required(reason, 1000);

        InventoryItem snapshot = items.findById(kitItemId).orElseThrow(CatalogNotFoundException::new);
        if (snapshot.itemType() != InventoryItemType.KIT)
            throw new CatalogConflictException("El artículo no es un KIT.");
        if (!snapshot.active())
            throw new CatalogConflictException("No se puede ensamblar un KIT inactivo.");

        List<InventoryKitComponent> bom = components.findByKitItemId(kitItemId);
        if (bom.isEmpty())
            throw new CatalogConflictException("El KIT no tiene una composición definida.");

        List<UUID> lockIds = new ArrayList<>();
        lockIds.add(kitItemId);
        bom.stream().map(InventoryKitComponent::componentItemId).forEach(lockIds::add);
        lockIds = lockIds.stream().distinct().sorted(Comparator.comparing(UUID::toString)).toList();

        Map<UUID, InventoryItem> locked = new LinkedHashMap<>();
        for (UUID id : lockIds)
            locked.put(id, items.findByIdForUpdate(id).orElseThrow(CatalogNotFoundException::new));

        InventoryItem kit = locked.get(kitItemId);
        if (kit.itemType() != InventoryItemType.KIT || !kit.active())
            throw new CatalogConflictException("El KIT no está disponible para ensamblaje.");

        for (InventoryKitComponent line : bom) {
            InventoryItem component = locked.get(line.componentItemId());
            if (component == null) throw new CatalogNotFoundException();
            if (!component.active())
                throw new CatalogConflictException("El BOM contiene un componente inactivo.");

            BigDecimal required = line.quantity().multiply(quantity);
            UnitOfMeasure unit = units.findByIdForUpdate(component.unitId())
                    .orElseThrow(CatalogNotFoundException::new);

            if (!unit.allowsDecimal() && required.stripTrailingZeros().scale() > 0)
                throw new IllegalArgumentException("La unidad del componente no admite cantidades fraccionarias.");
            if (component.stockCurrent().compareTo(required) < 0)
                throw new CatalogConflictException("Stock insuficiente para ensamblar el KIT.");
        }

        UnitOfMeasure kitUnit = units.findByIdForUpdate(kit.unitId())
                .orElseThrow(CatalogNotFoundException::new);
        if (!kitUnit.allowsDecimal() && quantity.stripTrailingZeros().scale() > 0)
            throw new IllegalArgumentException("La unidad del KIT no admite cantidades fraccionarias.");

        UUID assemblyId = UUID.randomUUID();
        String referenceId = assemblyId.toString();
        List<InventoryMovement> recorded = new ArrayList<>();

        for (InventoryKitComponent line : bom) {
            InventoryItem component = locked.get(line.componentItemId());
            UnitOfMeasure unit = units.findByIdForUpdate(component.unitId())
                    .orElseThrow(CatalogNotFoundException::new);
            BigDecimal required = line.quantity().multiply(quantity);

            InventoryMovement movement = InventoryMovement.create(
                    component, unit, InventoryMovementType.KIT_ASSEMBLY_CONSUMPTION,
                    InventoryMovementDirection.DECREASE, required, component.referenceUnitCost(),
                    requestId + ":component:" + component.id(), actor, reason,
                    "KIT_ASSEMBLY", referenceId, null, null);

            InventoryItem changed = component.applyMovement(movement, unit.allowsDecimal());
            items.saveStock(changed);
            locked.put(component.id(), changed);
            recorded.add(movements.append(movement));
        }

        InventoryMovement production = InventoryMovement.create(
                kit, kitUnit, InventoryMovementType.KIT_ASSEMBLY_PRODUCTION,
                InventoryMovementDirection.INCREASE, quantity, kit.referenceUnitCost(),
                requestId + ":kit", actor, reason, "KIT_ASSEMBLY", referenceId, null, null);

        InventoryItem changedKit = kit.applyMovement(production, kitUnit.allowsDecimal());
        items.saveStock(changedKit);
        recorded.add(movements.append(production));

        return new AssemblyResult(assemblyId, changedKit, List.copyOf(recorded));
    }

    public record AssemblyResult(UUID assemblyId, InventoryItem kit,
                                 List<InventoryMovement> movements) {}
}
