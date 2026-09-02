package com.heaterworkshop.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class OpenApiConfiguration {
    @Bean
    OpenAPI heaterWorkshopOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Heater Repair Workshop API")
                .description("API for receiving, starting and completing heater repair orders")
                .version("1.0"));
    }
}
