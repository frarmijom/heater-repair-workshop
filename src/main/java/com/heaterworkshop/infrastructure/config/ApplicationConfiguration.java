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
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);
    @Bean CreateWorkOrderUseCase createWorkOrderUseCase(WorkOrderRepository r) { return new CreateWorkOrderUseCase(r); }
    @Bean ListWorkOrdersUseCase listWorkOrdersUseCase(WorkOrderRepository r) { return new ListWorkOrdersUseCase(r); }
    @Bean GetWorkOrderUseCase getWorkOrderUseCase(WorkOrderRepository r) { return new GetWorkOrderUseCase(r); }
    @Bean StartWorkUseCase startRepairUseCase(WorkOrderRepository r) { return new StartWorkUseCase(r); }
    @Bean CompleteWorkUseCase completeRepairUseCase(WorkOrderRepository r, CustomerNotifier n) { return new CompleteWorkUseCase(r, n); }
    @Bean WorkOrderWorkflowUseCase workOrderWorkflowUseCase(WorkOrderRepository r) { return new WorkOrderWorkflowUseCase(r); }
    @Bean CustomerNotifier customerNotifier() {
        return (destination, message) -> LOGGER.info("Notification to {}: {}", destination.value(), message);
    }
}
