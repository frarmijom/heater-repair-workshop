package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.application.service.ServiceCatalogUseCases;
import com.heaterworkshop.application.service.ServiceCatalogCompositionUseCases;
import com.heaterworkshop.domain.service.ServiceCatalogComponent;
import com.heaterworkshop.domain.service.ServiceCatalogItem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
public class ServiceCatalogController {

    private static final Set<String> CREATE_FIELDS =
            Set.of("code", "name", "description", "price");

    private static final Set<String> EDIT_FIELDS =
            Set.of("code", "name", "description", "price", "active", "expectedVersion");

    private final ServiceCatalogUseCases services;
    private final ServiceCatalogCompositionUseCases compositions;

    public ServiceCatalogController(
            ServiceCatalogUseCases services,
            ServiceCatalogCompositionUseCases compositions) {
        this.services = services;
        this.compositions = compositions;
    }

    @GetMapping
    public List<ServiceCatalogResponse> list() {
        return services.list().stream()
                .map(ServiceCatalogResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ServiceCatalogResponse get(@PathVariable UUID id) {
        return ServiceCatalogResponse.from(services.get(id));
    }

    @PostMapping
    public ResponseEntity<ServiceCatalogResponse> create(@RequestBody JsonNode body) {
        validateObject(body, CREATE_FIELDS, false);

        ServiceCatalogItem created = services.create(
                text(body, "code", true),
                text(body, "name", true),
                optionalText(body, "description"),
                requiredDecimal(body.get("price"), "price"));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ServiceCatalogResponse.from(created));
    }

    @PatchMapping("/{id}")
    public ServiceCatalogResponse edit(@PathVariable UUID id, @RequestBody JsonNode body) {
        validateObject(body, EDIT_FIELDS, false);

        long expectedVersion = version(body);

        if (body.size() < 2) {
            throw new IllegalArgumentException(
                    "expectedVersion y al menos un cambio son obligatorios.");
        }

        ServiceCatalogItem current = services.get(id);

        String description = body.has("description")
                ? optionalText(body, "description")
                : current.description();

        ServiceCatalogItem updated = services.edit(
                id,
                expectedVersion,
                text(body, "code", false),
                text(body, "name", false),
                description,
                optionalDecimal(body, "price"),
                optionalBoolean(body, "active"));

        return ServiceCatalogResponse.from(updated);
    }


    @GetMapping("/{id}/composition")
    public List<ServiceComponentResponse> composition(@PathVariable UUID id) {
        return compositions.getComposition(id).stream()
                .map(ServiceComponentResponse::from)
                .toList();
    }

    @PutMapping("/{id}/composition")
    public List<ServiceComponentResponse> replaceComposition(
            @PathVariable UUID id,
            @RequestBody JsonNode body) {

        validateObject(body, Set.of("components"), false);

        JsonNode componentNodes = body.get("components");
        if (componentNodes == null || !componentNodes.isArray()) {
            throw new IllegalArgumentException(
                    "components debe ser un arreglo.");
        }

        List<ServiceCatalogCompositionUseCases.ComponentInput> inputs =
                new java.util.ArrayList<>();

        for (JsonNode component : componentNodes) {
            validateObject(
                    component,
                    Set.of("inventoryItemId", "quantity"),
                    false);

            JsonNode itemId = component.get("inventoryItemId");
            if (itemId == null || !itemId.isTextual()) {
                throw new IllegalArgumentException(
                        "inventoryItemId es obligatorio.");
            }

            UUID inventoryItemId;
            try {
                inventoryItemId = UUID.fromString(itemId.textValue());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "inventoryItemId inválido.");
            }

            BigDecimal quantity =
                    requiredDecimal(component.get("quantity"), "quantity");

            inputs.add(
                    new ServiceCatalogCompositionUseCases.ComponentInput(
                            inventoryItemId,
                            quantity));
        }

        return compositions.replaceComposition(id, inputs).stream()
                .map(ServiceComponentResponse::from)
                .toList();
    }


    private static void validateObject(JsonNode body, Set<String> allowed, boolean allowEmpty) {
        if (body == null || !body.isObject() || (!allowEmpty && body.isEmpty())) {
            throw new IllegalArgumentException("Se esperaba un objeto JSON válido.");
        }

        body.properties().forEach(entry -> {
            if (!allowed.contains(entry.getKey())) {
                throw new IllegalArgumentException(
                        "Campo no autorizado: " + entry.getKey());
            }
        });
    }

    private static String text(JsonNode body, String field, boolean required) {
        JsonNode value = body.get(field);

        if (value == null && !required) {
            return null;
        }

        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException(
                    "Campo obligatorio o inválido: " + field);
        }

        return value.textValue();
    }

    private static String optionalText(JsonNode body, String field) {
        JsonNode value = body.get(field);

        if (value == null || value.isNull()) {
            return null;
        }

        if (!value.isTextual()) {
            throw new IllegalArgumentException("Campo inválido: " + field);
        }

        return value.textValue();
    }

    private static BigDecimal optionalDecimal(JsonNode body, String field) {
        return body.has(field)
                ? requiredDecimal(body.get(field), field)
                : null;
    }

    private static BigDecimal requiredDecimal(JsonNode value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Campo numérico inválido: " + field);
        }

        try {
            if (value.isNumber()) {
                return value.decimalValue();
            }

            if (value.isTextual()
                    && value.textValue().matches("\\d+(\\.\\d+)?")) {
                return new BigDecimal(value.textValue());
            }
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException(
                    "Campo numérico inválido: " + field);
        }

        throw new IllegalArgumentException(
                "Campo numérico inválido: " + field);
    }

    private static Boolean optionalBoolean(JsonNode body, String field) {
        if (!body.has(field)) {
            return null;
        }

        JsonNode value = body.get(field);

        if (value == null || !value.isBoolean()) {
            throw new IllegalArgumentException(
                    "Campo booleano inválido: " + field);
        }

        return value.booleanValue();
    }

    private static long version(JsonNode body) {
        JsonNode value = body.get("expectedVersion");

        if (value == null
                || !value.isIntegralNumber()
                || !value.canConvertToLong()
                || value.longValue() < 0) {
            throw new IllegalArgumentException(
                    "expectedVersion debe ser un entero no negativo.");
        }

        return value.longValue();
    }
}
