package com.heaterworkshop.application.service;

import com.heaterworkshop.domain.inventory.CatalogNotFoundException;
import com.heaterworkshop.domain.inventory.InventoryItem;
import com.heaterworkshop.domain.inventory.InventoryItemRepository;
import com.heaterworkshop.domain.service.ServiceCatalogComponent;
import com.heaterworkshop.domain.service.ServiceCatalogComponentRepository;
import com.heaterworkshop.domain.service.ServiceCatalogItem;
import com.heaterworkshop.domain.service.ServiceCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ServiceCatalogCompositionUseCases {
    private final ServiceCatalogRepository services;
    private final ServiceCatalogComponentRepository components;
    private final InventoryItemRepository items;

    public ServiceCatalogCompositionUseCases(
            ServiceCatalogRepository services,
            ServiceCatalogComponentRepository components,
            InventoryItemRepository items) {
        this.services = services;
        this.components = components;
        this.items = items;
    }

    @Transactional(readOnly = true)
    public List<ServiceCatalogComponent> getComposition(UUID serviceId) {
        requireService(serviceId);
        return components.findByServiceId(serviceId);
    }

    public List<ServiceCatalogComponent> replaceComposition(
            UUID serviceId,
            List<ComponentInput> requestedComponents) {
        ServiceCatalogItem service = requireService(serviceId);
        if (!service.active()) {
            throw new IllegalArgumentException("No se puede modificar la composición de un servicio inactivo.");
        }

        List<ComponentInput> inputs = List.copyOf(Objects.requireNonNull(requestedComponents));
        Set<UUID> seen = new HashSet<>();

        List<ServiceCatalogComponent> replacement = inputs.stream()
                .map(input -> {
                    Objects.requireNonNull(input);
                    UUID inventoryItemId = Objects.requireNonNull(input.inventoryItemId());
                    if (!seen.add(inventoryItemId)) {
                        throw new IllegalArgumentException(
                                "Un artículo no puede repetirse en la composición del servicio.");
                    }

                    InventoryItem item = items.findById(inventoryItemId)
                            .orElseThrow(CatalogNotFoundException::new);
                    if (!item.active()) {
                        throw new IllegalArgumentException(
                                "La composición no puede usar artículos inactivos.");
                    }

                    return new ServiceCatalogComponent(serviceId, inventoryItemId, input.quantity());
                })
                .toList();

        components.replace(serviceId, replacement);
        return components.findByServiceId(serviceId);
    }

    private ServiceCatalogItem requireService(UUID serviceId) {
        return services.findById(Objects.requireNonNull(serviceId))
                .orElseThrow(CatalogNotFoundException::new);
    }

    public record ComponentInput(UUID inventoryItemId, BigDecimal quantity) {}
}
