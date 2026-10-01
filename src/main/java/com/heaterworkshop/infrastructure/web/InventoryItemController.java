package com.heaterworkshop.infrastructure.web;

import tools.jackson.databind.JsonNode;
import com.heaterworkshop.application.inventory.InventoryAdjustmentUseCases;
import com.heaterworkshop.application.inventory.InventoryItemUseCases;
import com.heaterworkshop.application.inventory.InventoryReceiptUseCases;
import com.heaterworkshop.application.inventory.InventoryReversalUseCases;
import com.heaterworkshop.application.inventory.InventoryKitUseCases;
import com.heaterworkshop.application.inventory.InventoryKitAssemblyUseCases;
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
            "stockMinimum", "referenceUnitCost", "initialStock", "itemType", "requestId");
    private static final Set<String> BOM_FIELDS = Set.of("components");
    private static final Set<String> BOM_COMPONENT_FIELDS = Set.of("componentItemId", "quantity");
    private static final Set<String> ASSEMBLY_FIELDS = Set.of("requestId", "quantity", "reason");
    private static final Set<String> RECEIPT_FIELDS = Set.of("requestId", "quantity", "unitCost", "reason");
    private static final Set<String> ADJUSTMENT_FIELDS = Set.of("requestId", "direction", "quantity", "reason");
    private static final Set<String> REVERSAL_FIELDS = Set.of("requestId", "reason");
    private static final Set<String> EDIT_FIELDS = Set.of("sku", "name", "description", "categoryId", "unitId",
            "stockMinimum", "referenceUnitCost", "active", "expectedVersion");

    private final InventoryItemUseCases items;
    private final InventoryCategoryRepository categories;
    private final UnitOfMeasureRepository units;
    private final InventoryMovementRepository movements;
    private final InventoryReceiptUseCases receipts;
    private final InventoryAdjustmentUseCases adjustments;
    private final InventoryReversalUseCases reversals;
    private final InventoryKitUseCases kits;
    private final InventoryKitAssemblyUseCases assemblies;

    public InventoryItemController(InventoryItemUseCases items, InventoryCategoryRepository categories,
                                   UnitOfMeasureRepository units, InventoryMovementRepository movements,
                                   InventoryReceiptUseCases receipts, InventoryAdjustmentUseCases adjustments,
                                   InventoryReversalUseCases reversals, InventoryKitUseCases kits,
                                   InventoryKitAssemblyUseCases assemblies) {
        this.items = items;
        this.categories = categories;
        this.units = units;
        this.movements = movements;
        this.receipts = receipts;
        this.adjustments = adjustments;
        this.reversals = reversals;
        this.kits = kits;
        this.assemblies = assemblies;
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
                decimal(body, "initialStock", BigDecimal.ZERO), itemType(body), text(body, "requestId", true),
                authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response(item,
                categories.findById(item.categoryId()).orElseThrow(CatalogNotFoundException::new),
                units.findById(item.unitId()).orElseThrow(CatalogNotFoundException::new)));
    }


    @GetMapping("/{id}/bom")
    public InventoryKitBomResponse bom(@PathVariable UUID id) {
        return InventoryKitBomResponse.from(kits.get(id));
    }

    @PutMapping("/{id}/bom")
    public InventoryKitBomResponse replaceBom(@PathVariable UUID id, @RequestBody JsonNode body) {
        validateObject(body, BOM_FIELDS, false);
        JsonNode components = body.get("components");
        if (components == null || !components.isArray())
            throw new IllegalArgumentException("components debe ser un arreglo.");
        List<InventoryKitUseCases.ComponentInput> inputs = new ArrayList<>();
        for (JsonNode component : components) {
            validateObject(component, BOM_COMPONENT_FIELDS, false);
            inputs.add(new InventoryKitUseCases.ComponentInput(uuid(component, "componentItemId", true),
                    requiredDecimal(component.get("quantity"), "quantity")));
        }
        return InventoryKitBomResponse.from(kits.replace(id, inputs));
    }

    @PostMapping("/{id}/assemblies")
    public ResponseEntity<InventoryKitAssemblyResponse> assemble(@PathVariable UUID id,
                                                                 @RequestBody JsonNode body,
                                                                 Authentication authentication) {
        validateObject(body, ASSEMBLY_FIELDS, true);
        var result = assemblies.assemble(
                id,
                requiredDecimal(body.get("quantity"), "quantity"),
                text(body, "requestId", true),
                authentication.getName(),
                text(body, "reason", true));
        InventoryItem kit = result.kit();
        var kitResponse = response(
                kit,
                categories.findById(kit.categoryId()).orElseThrow(CatalogNotFoundException::new),
                units.findById(kit.unitId()).orElseThrow(CatalogNotFoundException::new));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InventoryKitAssemblyResponse.from(result, kitResponse));
    }

    @PostMapping("/{id}/receipts")
    public ResponseEntity<InventoryMovementResponse> receive(@PathVariable UUID id, @RequestBody JsonNode body,
                                                              Authentication authentication) {
        validateObject(body, RECEIPT_FIELDS, true);
        InventoryMovement movement = receipts.receive(id, requiredDecimal(body.get("quantity"), "quantity"),
                requiredDecimal(body.get("unitCost"), "unitCost"), text(body, "requestId", true),
                authentication.getName(), text(body, "reason", true));
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryMovementResponse.from(movement));
    }

    @PostMapping("/{id}/adjustments")
    public ResponseEntity<InventoryMovementResponse> adjust(@PathVariable UUID id, @RequestBody JsonNode body,
                                                            Authentication authentication) {
        validateObject(body, ADJUSTMENT_FIELDS, true);
        InventoryMovementDirection direction;
        try {
            direction = InventoryMovementDirection.valueOf(text(body, "direction", true));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("direction debe ser INCREASE o DECREASE.");
        }
        InventoryMovement movement = adjustments.adjust(id, direction,
                requiredDecimal(body.get("quantity"), "quantity"), text(body, "requestId", true),
                authentication.getName(), text(body, "reason", true));
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryMovementResponse.from(movement));
    }

    @PostMapping("/movements/{movementId}/reversal")
    public ResponseEntity<InventoryMovementResponse> reverse(@PathVariable UUID movementId, @RequestBody JsonNode body,
                                                             Authentication authentication) {
        validateObject(body, REVERSAL_FIELDS, true);
        InventoryMovement movement = reversals.reverse(movementId, text(body, "requestId", true),
                authentication.getName(), text(body, "reason", true));
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryMovementResponse.from(movement));
    }

    @GetMapping("/{id}/movements")
    public List<InventoryMovementResponse> history(@PathVariable UUID id) {
        return receipts.history(id).stream().map(InventoryMovementResponse::from).toList();
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


    private static InventoryItemType itemType(JsonNode body) {
        String value = text(body, "itemType", false);
        if (value == null) return InventoryItemType.STANDARD;
        try { return InventoryItemType.valueOf(value); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("itemType debe ser STANDARD o KIT."); }
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