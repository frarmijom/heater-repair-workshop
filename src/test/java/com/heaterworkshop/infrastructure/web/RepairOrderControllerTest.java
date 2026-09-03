package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.application.usecase.CompleteRepairUseCase;
import com.heaterworkshop.application.usecase.CreateRepairOrderUseCase;
import com.heaterworkshop.application.usecase.GetRepairOrderUseCase;
import com.heaterworkshop.application.usecase.ListRepairOrdersUseCase;
import com.heaterworkshop.application.usecase.StartRepairUseCase;
import com.heaterworkshop.infrastructure.persistence.InMemoryRepairOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RepairOrderControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InMemoryRepairOrderRepository repository = new InMemoryRepairOrderRepository();
        RepairOrderController controller = new RepairOrderController(
                new CreateRepairOrderUseCase(repository), new ListRepairOrdersUseCase(repository),
                new GetRepairOrderUseCase(repository), new StartRepairUseCase(repository),
                new CompleteRepairUseCase(repository, (destination, message) -> { }));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void supportsTheCompleteRepairOrderLifecycle() throws Exception {
        MvcResult creation = mockMvc.perform(post("/api/repair-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Maria Gonzalez","customerContact":"+56911112222",
                                 "heaterBrand":"Bosch","heaterModel":"Therm 5700",
                                 "reportedIssue":"Turns off after a few minutes"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.diagnosis").doesNotExist())
                .andExpect(jsonPath("$.completedAt").doesNotExist())
                .andReturn();
        String id = extractId(creation.getResponse().getContentAsString());
        assertTrue(id.matches("ORDER-[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}"));

        mockMvc.perform(get("/api/repair-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(get("/api/repair-orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Maria Gonzalez"));
        mockMvc.perform(patch("/api/repair-orders/{id}/start", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\":\"Damaged ignition sensor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(patch("/api/repair-orders/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    void returnsContractErrorsForInvalidInputAndTransitions() throws Exception {
        mockMvc.perform(get("/api/repair-orders/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").isMap());

        MvcResult creation = mockMvc.perform(post("/api/repair-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Maria Gonzalez","customerContact":"+56911112222",
                                 "heaterBrand":"Bosch","heaterModel":"Therm 5700","reportedIssue":"Turns off"}
                                """))
                .andReturn();
        String id = extractId(creation.getResponse().getContentAsString());
        mockMvc.perform(patch("/api/repair-orders/{id}/complete", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/repair-orders/" + id + "/complete"));
    }

    private String extractId(String json) {
        Matcher matcher = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"").matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Response did not contain an ID: " + json);
        }
        return matcher.group(1);
    }
}
