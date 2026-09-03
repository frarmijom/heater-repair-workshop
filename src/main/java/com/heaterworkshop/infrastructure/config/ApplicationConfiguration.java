package com.heaterworkshop.infrastructure.config;

import com.heaterworkshop.application.port.CustomerNotifier;
import com.heaterworkshop.application.usecase.*;
import com.heaterworkshop.domain.repository.RepairOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);
    @Bean CreateRepairOrderUseCase createRepairOrderUseCase(RepairOrderRepository r) { return new CreateRepairOrderUseCase(r); }
    @Bean ListRepairOrdersUseCase listRepairOrdersUseCase(RepairOrderRepository r) { return new ListRepairOrdersUseCase(r); }
    @Bean GetRepairOrderUseCase getRepairOrderUseCase(RepairOrderRepository r) { return new GetRepairOrderUseCase(r); }
    @Bean StartRepairUseCase startRepairUseCase(RepairOrderRepository r) { return new StartRepairUseCase(r); }
    @Bean CompleteRepairUseCase completeRepairUseCase(RepairOrderRepository r, CustomerNotifier n) { return new CompleteRepairUseCase(r, n); }
    @Bean CustomerNotifier customerNotifier() {
        return (destination, message) -> LOGGER.info("Notification to {}: {}", destination.value(), message);
    }
}
