package com.heaterworkshop.application.inventory;

import com.heaterworkshop.domain.inventory.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryItemUseCases {
    private final InventoryItemRepository items;
    private final InventoryItemCreationRequestRepository requests;
    private final InventoryCategoryRepository categories;
    private final UnitOfMeasureRepository units;
    private final InventoryMovementRepository movements;
    private final RecordInventoryMovementUseCase recordMovement;

    public InventoryItemUseCases(InventoryItemRepository items, InventoryItemCreationRequestRepository requests,
                                 InventoryCategoryRepository categories, UnitOfMeasureRepository units,
                                 InventoryMovementRepository movements, RecordInventoryMovementUseCase recordMovement) {
        this.items = items;
        this.requests = requests;
        this.categories = categories;
        this.units = units;
        this.movements = movements;
        this.recordMovement = recordMovement;
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> list() {
        return items.findAll();
    }

    @Transactional(readOnly = true)
    public InventoryItem get(UUID id) {
        return items.findById(id).orElseThrow(CatalogNotFoundException::new);
    }

    @Transactional
    public InventoryItem create(String sku, String name, String description, UUID categoryId, UUID unitId,
                                BigDecimal stockMinimum, BigDecimal referenceUnitCost, BigDecimal initialStock,
                                String requestId, String actor) {
        requestId = CatalogText.required(requestId, 128);
        InventoryQuantity.nonNegative(stockMinimum, 3, "stockMinimum");
        InventoryQuantity.nonNegative(referenceUnitCost, 4, "referenceUnitCost");
        InventoryQuantity.nonNegative(initialStock, 3, "initialStock");
        InventoryItem item = InventoryItem.create(sku, name, description, categoryId, unitId,
                stockMinimum, referenceUnitCost);
        String payloadHash = payloadHash(item, initialStock, actor);
        if (!requests.claim(requestId, item.id(), payloadHash)) {
            InventoryItemCreationRequest prior = requests.findByRequestId(requestId).orElseThrow();
            if (!prior.payloadHash().equals(payloadHash))
                throw new CatalogConflictException("El requestId ya fue utilizado con otra creación.");
            return items.findById(prior.itemId()).orElseThrow(CatalogNotFoundException::new);
        }

        InventoryCategory category = categories.findByIdForUpdate(categoryId).orElseThrow(CatalogNotFoundException::new);
        UnitOfMeasure unit = units.findByIdForUpdate(unitId).orElseThrow(CatalogNotFoundException::new);
        if (!category.active()) throw new CatalogConflictException("La categoría está inactiva.");
        if (!unit.active()) throw new CatalogConflictException("La unidad está inactiva.");
        if (!unit.allowsDecimal() && initialStock.stripTrailingZeros().scale() > 0)
            throw new IllegalArgumentException("La unidad no admite cantidades fraccionarias.");

        items.create(item);
        if (initialStock.signum() > 0) {
            recordMovement.record(item.id(), InventoryMovementType.INITIAL_ENTRY, InventoryMovementDirection.INCREASE,
                    initialStock, requestId, actor, "Stock disponible al registrar el artículo",
                    "INVENTORY_ITEM_CREATION", item.id().toString(), null, null);
        }
        return items.findById(item.id()).orElseThrow(CatalogNotFoundException::new);
    }

    @Transactional
    public InventoryItem edit(UUID id, long expectedVersion, String sku, String name, String description,
                              UUID categoryId, UUID unitId, BigDecimal stockMinimum,
                              BigDecimal referenceUnitCost, Boolean active) {
        InventoryItem current = items.findByIdForUpdate(id).orElseThrow(CatalogNotFoundException::new);
        if (current.version() != expectedVersion)
            throw new CatalogConflictException("El artículo cambió. Recarga antes de editar.");
        boolean categoryChanged = categoryId != null && !categoryId.equals(current.categoryId());
        boolean unitChanged = unitId != null && !unitId.equals(current.unitId());
        if (unitChanged && movements.countByItemId(id) > 0)
            throw new CatalogConflictException("La unidad no puede modificarse porque el artículo ya posee movimientos.");
        UUID nextCategoryId = categoryId == null ? current.categoryId() : categoryId;
        UUID nextUnitId = unitId == null ? current.unitId() : unitId;
        if (categoryChanged) requireActiveCategory(nextCategoryId);
        if (unitChanged) requireActiveUnit(nextUnitId);
        InventoryItem changed = current.edit(sku == null ? current.sku() : sku,
                name == null ? current.name() : name, description,
                nextCategoryId, nextUnitId,
                stockMinimum == null ? current.stockMinimum() : stockMinimum,
                referenceUnitCost == null ? current.referenceUnitCost() : referenceUnitCost,
                active == null ? current.active() : active);
        return items.saveMetadata(changed, expectedVersion);
    }

    private void requireActiveCategory(UUID id) {
        if (!categories.findByIdForUpdate(id).orElseThrow(CatalogNotFoundException::new).active())
            throw new CatalogConflictException("La categoría está inactiva.");
    }

    private void requireActiveUnit(UUID id) {
        if (!units.findByIdForUpdate(id).orElseThrow(CatalogNotFoundException::new).active())
            throw new CatalogConflictException("La unidad está inactiva.");
    }

    private String payloadHash(InventoryItem item, BigDecimal initialStock, String actor) {
        String value = String.join("\n", item.sku(), item.name(), item.description() == null ? "" : item.description(),
                item.categoryId().toString(), item.unitId().toString(), canonical(item.stockMinimum()),
                canonical(item.referenceUnitCost()), canonical(initialStock), actor);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String canonical(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}