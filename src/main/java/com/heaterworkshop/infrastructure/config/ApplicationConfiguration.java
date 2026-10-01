package com.heaterworkshop.infrastructure.config;

import com.heaterworkshop.application.port.CustomerNotifier;
import com.heaterworkshop.application.usecase.*;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean com.heaterworkshop.application.inventory.InventoryCategoryUseCases inventoryCategories(com.heaterworkshop.domain.inventory.InventoryCategoryRepository repository) {
        return new com.heaterworkshop.application.inventory.InventoryCategoryUseCases(repository);
    }
    @Bean com.heaterworkshop.application.inventory.UnitOfMeasureUseCases inventoryUnits(com.heaterworkshop.domain.inventory.UnitOfMeasureRepository repository,
                                                                                       com.heaterworkshop.domain.inventory.InventoryMovementRepository movements) {
        return new com.heaterworkshop.application.inventory.UnitOfMeasureUseCases(repository, movements);
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);
    @Bean CreateWorkOrderUseCase createWorkOrderUseCase(WorkOrderRepository r) { return new CreateWorkOrderUseCase(r); }
    @Bean ListWorkOrdersUseCase listWorkOrdersUseCase(WorkOrderRepository r) { return new ListWorkOrdersUseCase(r); }
    @Bean GetWorkOrderUseCase getWorkOrderUseCase(WorkOrderRepository r) { return new GetWorkOrderUseCase(r); }
    @Bean StartWorkUseCase startRepairUseCase(WorkOrderRepository r) { return new StartWorkUseCase(r); }
    @Bean CompleteWorkUseCase completeRepairUseCase(WorkOrderRepository r, CustomerNotifier n) { return new CompleteWorkUseCase(r, n); }
    @Bean WorkOrderWorkflowUseCase workOrderWorkflowUseCase(WorkOrderRepository r) { return new WorkOrderWorkflowUseCase(r); }
    @Bean com.heaterworkshop.application.service.WorkOrderEquipmentServiceUseCases workOrderEquipmentServices(
            WorkOrderRepository workOrders,
            com.heaterworkshop.domain.service.ServiceCatalogRepository services,
            com.heaterworkshop.domain.service.WorkOrderEquipmentServiceRepository assignments) {
        return new com.heaterworkshop.application.service.WorkOrderEquipmentServiceUseCases(workOrders, services, assignments);
    }
    @Bean CustomerNotifier customerNotifier() {
        return (destination, message) -> LOGGER.info("Notification to {}: {}", destination.value(), message);
    }
}
