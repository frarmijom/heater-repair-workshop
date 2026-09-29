package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.application.inventory.*;
import com.heaterworkshop.domain.inventory.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/inventory")
public class InventoryCatalogController {
    private final InventoryCategoryUseCases categories;
    private final UnitOfMeasureUseCases units;
    public InventoryCatalogController(InventoryCategoryUseCases categories, UnitOfMeasureUseCases units) {
        this.categories=categories; this.units=units;
    }
    @GetMapping("/categories") public List<InventoryCategory> categories() { return categories.list(); }
    @GetMapping("/units") public List<UnitOfMeasure> units() { return units.list(); }
    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
    public InventoryCategory createCategory(@RequestBody Map<String,Object> body) {
        fields(body, Set.of("name"));
        return categories.create(text(body,"name",true));
    }
    @PostMapping("/units") @ResponseStatus(HttpStatus.CREATED)
    public UnitOfMeasure createUnit(@RequestBody Map<String,Object> body) {
        fields(body, Set.of("name","symbol","allowsDecimal"));
        return units.create(text(body,"name",true),text(body,"symbol",true),bool(body,"allowsDecimal",true));
    }
    @PatchMapping("/categories/{id}")
    public InventoryCategory editCategory(@PathVariable UUID id, @RequestBody Map<String,Object> body) {
        fields(body, Set.of("name","active","expectedVersion"));
        return categories.edit(id, version(body),text(body,"name",false),bool(body,"active",false));
    }
    @PatchMapping("/units/{id}")
    public UnitOfMeasure editUnit(@PathVariable UUID id, @RequestBody Map<String,Object> body) {
        fields(body, Set.of("name","symbol","allowsDecimal","active","expectedVersion"));
        return units.edit(id,version(body),text(body,"name",false),text(body,"symbol",false),bool(body,"allowsDecimal",false),bool(body,"active",false));
    }
    private static void fields(Map<String,Object> body, Set<String> allowed) {
        if (body.isEmpty() || !allowed.containsAll(body.keySet())) throw new IllegalArgumentException("Campos vacíos o no autorizados.");
    }
    private static String text(Map<String,Object> body, String key, boolean required) {
        if (!body.containsKey(key) && !required) return null;
        if (!(body.get(key) instanceof String value)) throw new IllegalArgumentException("Campo obligatorio o inválido: " + key);
        return value;
    }
    private static Boolean bool(Map<String,Object> body, String key, boolean required) {
        if (!body.containsKey(key) && !required) return null;
        if (!(body.get(key) instanceof Boolean value)) throw new IllegalArgumentException("Campo obligatorio o inválido: " + key);
        return value;
    }
    private static long version(Map<String,Object> body) {
        Object value=body.get("expectedVersion");
        if (!(value instanceof Integer || value instanceof Long) || ((Number)value).longValue()<0 || body.size()<2)
            throw new IllegalArgumentException("expectedVersion y al menos un cambio son obligatorios.");
        return ((Number)value).longValue();
    }
}
