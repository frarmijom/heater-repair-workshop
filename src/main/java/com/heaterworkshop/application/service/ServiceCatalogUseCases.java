package com.heaterworkshop.application.service;

import com.heaterworkshop.domain.inventory.CatalogConflictException;
import com.heaterworkshop.domain.inventory.CatalogNotFoundException;
import com.heaterworkshop.domain.service.ServiceCatalogItem;
import com.heaterworkshop.domain.service.ServiceCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ServiceCatalogUseCases {
    private final ServiceCatalogRepository services;

    public ServiceCatalogUseCases(ServiceCatalogRepository services) {
        this.services = services;
    }

    @Transactional(readOnly = true)
    public List<ServiceCatalogItem> list() {
        return services.findAll();
    }

    @Transactional(readOnly = true)
    public ServiceCatalogItem get(UUID id) {
        return services.findById(id).orElseThrow(CatalogNotFoundException::new);
    }

    @Transactional
    public ServiceCatalogItem create(String code, String name, String description, BigDecimal price) {
        ServiceCatalogItem item = ServiceCatalogItem.create(code, name, description, price);
        services.findByCode(item.code()).ifPresent(existing -> {
            throw new CatalogConflictException("El código de servicio ya existe.");
        });
        return services.create(item);
    }

    @Transactional
    public ServiceCatalogItem edit(UUID id, long expectedVersion, String code, String name,
                                   String description, BigDecimal price, Boolean active) {
        ServiceCatalogItem current = services.findByIdForUpdate(id).orElseThrow(CatalogNotFoundException::new);
        if (current.version() != expectedVersion) {
            throw new CatalogConflictException("El servicio cambió. Recarga antes de editar.");
        }
        ServiceCatalogItem changed = current.edit(
                code == null ? current.code() : code,
                name == null ? current.name() : name,
                description,
                price == null ? current.price() : price,
                active == null ? current.active() : active);
        services.findByCode(changed.code())
                .filter(existing -> !existing.id().equals(id))
                .ifPresent(existing -> { throw new CatalogConflictException("El código de servicio ya existe."); });
        return services.save(changed, expectedVersion);
    }
}
