package com.heaterworkshop.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsConfigurationTest {

    @Test
    void acceptsAConfiguredOrigin() throws Exception {
        MockMvc mockMvc = mockMvc("http://localhost:5173, https://workshop.example.com");

        mockMvc.perform(options("/api/repair-orders")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void rejectsAnOriginThatWasNotConfigured() throws Exception {
        MockMvc mockMvc = mockMvc("http://localhost:5173");

        mockMvc.perform(options("/api/repair-orders")
                        .header("Origin", "https://untrusted.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void rejectsWildcardConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new CorsConfiguration("*"));
    }

    private MockMvc mockMvc(String origins) {
        CorsConfiguration configuration = new CorsConfiguration(origins);
        return MockMvcBuilders.standaloneSetup(new ApiProbeController())
                .addFilters(configuration.corsFilter(configuration.corsConfigurationSource()))
                .build();
    }

    @RestController
    @RequestMapping("/api/repair-orders")
    static class ApiProbeController {
        @GetMapping
        String list() {
            return "ok";
        }
    }
}
