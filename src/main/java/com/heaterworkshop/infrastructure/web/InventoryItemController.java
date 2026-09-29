package com.heaterworkshop.infrastructure.web;

import tools.jackson.databind.JsonNode;
import com.heaterworkshop.application.inventory.InventoryItemUseCases;
import com.heaterworkshop.domain.inventory.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventory/items")
public class InventoryItemController {
    private static final Set<String> CREATE_FIELDS = Set.of("sku", "name", "description", "categoryId", "unitId",
            "stockMinimum", "referenceUnitCost", "initialStock", "requestId");
    private static final Set<String> EDIT_FIELDS = Set.of("sku", "name", "description", "categoryId", "unitId",
            "stockMinimum", "referenceUnitCost", "active", "expectedVersion");

    private final InventoryItemUseCases items;
    private final InventoryCategoryRepository categories;
    private final UnitOfMeasureRepository units;
    private final InventoryMovementRepository movements;

    public InventoryItemController(InventoryItemUseCases items, InventoryCategoryRepository categories,
                                   UnitOfMeasureRepository units, InventoryMovementRepository movements) {
        this.items = items;
        this.categories = categories;
        this.units = units;
        this.movements = movements;
    }

    @GetMapping
    public List<InventoryItemResponse> list() {
        Map<UUID, InventoryCategory> categoryMap = categories.findAll().stream()
                .collect(Collectors.toMap(InventoryCategory::id, Function.identity()));
        Map<UUID, UnitOfMeasure> unitMap = units.findAll().stream()
                .collect(Collectors.toMap(UnitOfMeasure::id, Function.identity()));
        return items.list().stream().map(item -> response(item,
                categoryMap.get(item.categoryId()), unitMap.get(item.unitId()))).toList();
    }

    @GetMapping("/{id}")
    public InventoryItemResponse get(@PathVariable UUID id) {
        InventoryItem item = items.get(id);
        return response(item, categories.findById(item.categoryId()).orElseThrow(CatalogNotFoundException::new),
                units.findById(item.unitId()).orElseThrow(CatalogNotFoundException::new));
    }

    @PostMapping
    public ResponseEntity<InventoryItemResponse> create(@RequestBody JsonNode body, Authentication authentication) {
        validateObject(body, CREATE_FIELDS, true);
        InventoryItem item = items.create(text(body, "sku", true), text(body, "name", true),
                optionalText(body, "description"), uuid(body, "categoryId", true), uuid(body, "unitId", true),
                decimal(body, "stockMinimum", BigDecimal.ZERO), decimal(body, "referenceUnitCost", BigDecimal.ZERO),
                decimal(body, "initialStock", BigDecimal.ZERO), text(body, "requestId", true),
                authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response(item,
                categories.findById(item.categoryId()).orElseThrow(CatalogNotFoundException::new),
                units.findById(item.unitId()).orElseThrow(CatalogNotFoundException::new)));
    }

    @PatchMapping("/{id}")
    public InventoryItemResponse edit(@PathVariable UUID id, @RequestBody JsonNode body) {
        validateObject(body, EDIT_FIELDS, false);
        long expectedVersion = version(body);
        InventoryItem current = items.get(id);
        if (body.size() < 2) throw new IllegalArgumentException("expectedVersion y al menos un cambio son obligatorios.");
        String description = body.has("description") ? optionalText(body, "description") : current.description();
        InventoryItem updated = items.edit(id, expectedVersion,
                text(body, "sku", false), text(body, "name", false), description,
                uuid(body, "categoryId", false), uuid(body, "unitId", false),
                optionalDecimal(body, "stockMinimum"), optionalDecimal(body, "referenceUnitCost"),
                optionalBoolean(body, "active"));
        return response(updated, categories.findById(updated.categoryId()).orElseThrow(CatalogNotFoundException::new),
                units.findById(updated.unitId()).orElseThrow(CatalogNotFoundException::new));
    }

    private InventoryItemResponse response(InventoryItem item, InventoryCategory category, UnitOfMeasure unit) {
        return InventoryItemResponse.from(item, category, unit, movements.countByItemId(item.id()) > 0);
    }

    private static void validateObject(JsonNode body, Set<String> allowed, boolean allowEmpty) {
        if (body == null || !body.isObject() || (!allowEmpty && body.isEmpty()))
            throw new IllegalArgumentException("Se esperaba un objeto JSON válido.");
        body.properties().forEach(entry -> {
            String name = entry.getKey();
            if (!allowed.contains(name)) throw new IllegalArgumentException("Campo no autorizado: " + name);
        });
    }

    private static String text(JsonNode body, String field, boolean required) {
        JsonNode value = body.get(field);
        if (value == null && !required) return null;
        if (value == null || !value.isTextual()) throw new IllegalArgumentException("Campo obligatorio o inválido: " + field);
        return value.textValue();
    }

    private static String optionalText(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw new IllegalArgumentException("Campo inválido: " + field);
        return value.textValue();
    }

    private static UUID uuid(JsonNode body, String field, boolean required) {
        String value = text(body, field, required);
        if (value == null) return null;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Identificador inválido: " + field); }
    }

    private static BigDecimal decimal(JsonNode body, String field, BigDecimal fallback) {
        return body.has(field) ? requiredDecimal(body.get(field), field) : fallback;
    }

    private static BigDecimal optionalDecimal(JsonNode body, String field) {
        return body.has(field) ? requiredDecimal(body.get(field), field) : null;
    }

    private static BigDecimal requiredDecimal(JsonNode value, String field) {
        if (value == null) throw new IllegalArgumentException("Campo numérico inválido: " + field);
        try {
            if (value.isNumber()) return value.decimalValue();
            if (value.isTextual() && value.textValue().matches("\\d+(\\.\\d+)?"))
                return new BigDecimal(value.textValue());
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException("Campo numérico inválido: " + field);
        }
        throw new IllegalArgumentException("Campo numérico inválido: " + field);
    }

    private static Boolean optionalBoolean(JsonNode body, String field) {
        if (!body.has(field)) return null;
        JsonNode value = body.get(field);
        if (value == null || !value.isBoolean()) throw new IllegalArgumentException("Campo booleano inválido: " + field);
        return value.booleanValue();
    }

    private static long version(JsonNode body) {
        JsonNode value = body.get("expectedVersion");
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0)
            throw new IllegalArgumentException("expectedVersion debe ser un entero no negativo.");
        return value.longValue();
    }
}