package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.application.usecase.CompleteRepairUseCase;
import com.heaterworkshop.application.usecase.CreateRepairOrderUseCase;
import com.heaterworkshop.application.usecase.GetRepairOrderUseCase;
import com.heaterworkshop.application.usecase.ListRepairOrdersUseCase;
import com.heaterworkshop.application.usecase.StartRepairUseCase;
import com.heaterworkshop.infrastructure.persistence.InMemoryRepairOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(strings = {"REPAIR", "MAINTENANCE"})
    void supportsTheCompleteRepairOrderLifecycle(String type) throws Exception {
        MvcResult creation = mockMvc.perform(post("/api/repair-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Maria Gonzalez","customerContact":"+56911112222",
                                 "heaterBrand":"Bosch","heaterModel":"Therm 5700",
                                 "serviceType":"%s","reportedIssue":"Turns off after a few minutes"}
                                """.formatted(type)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceType").value(type))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.diagnosis").doesNotExist())
                .andExpect(jsonPath("$.completedAt").doesNotExist())
                .andReturn();
        String id = extractId(creation.getResponse().getContentAsString());
        assertTrue(id.matches("ORDER-[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}"));

        mockMvc.perform(get("/api/repair-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].serviceType").value(type));
        mockMvc.perform(get("/api/repair-orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Maria Gonzalez"))
                .andExpect(jsonPath("$.serviceType").value(type));
        mockMvc.perform(patch("/api/repair-orders/{id}/start", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"diagnosis\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/repair-orders/{id}/start", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\":\"Damaged ignition sensor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.serviceType").value(type));
        mockMvc.perform(patch("/api/repair-orders/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.serviceType").value(type))
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
                                 "heaterBrand":"Bosch","heaterModel":"Therm 5700","serviceType":"REPAIR","reportedIssue":"Turns off"}
                                """))
                .andReturn();
        String id = extractId(creation.getResponse().getContentAsString());
        mockMvc.perform(patch("/api/repair-orders/{id}/complete", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/repair-orders/" + id + "/complete"));
    }

    @Test
    void returnsNotFoundForAnUnknownValidIdentifier() throws Exception {
        String id = "ORDER-550E8400-E29B-41D4-A716-446655440404";

        mockMvc.perform(get("/api/repair-orders/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/repair-orders/" + id));
    }

    @Test
    void returnsFieldErrorsForAnInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/repair-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.validationErrors.customerName").exists())
                .andExpect(jsonPath("$.validationErrors.customerContact").exists());
    }

    @Test
    void returnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/repair-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed."));
    }

    @Test
    void returnsNotFoundForAnUnknownRoute() throws Exception {
        mockMvc.perform(get("/route-that-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("The requested resource was not found."))
                .andExpect(jsonPath("$.path").value("/route-that-does-not-exist"));
    }

    private String extractId(String json) {
        Matcher matcher = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"").matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Response did not contain an ID: " + json);
        }
        return matcher.group(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", ",\"reportedIssue\":null", ",\"reportedIssue\":\"\"", ",\"reportedIssue\":\"   \""})
    void rejectsRepairWithoutIssue(String issueField) throws Exception {
        mockMvc.perform(post("/api/repair-orders").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("\"REPAIR\"", issueField)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Reported issue must not be blank."));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", ",\"reportedIssue\":null", ",\"reportedIssue\":\"\"", ",\"reportedIssue\":\"   \""})
    void acceptsMaintenanceWithoutObservations(String issueField) throws Exception {
        mockMvc.perform(post("/api/repair-orders").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("\"MAINTENANCE\"", issueField)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceType").value("MAINTENANCE"))
                .andExpect(jsonPath("$.reportedIssue").value(""));
    }

    @Test
    void trimsMaintenanceObservations() throws Exception {
        mockMvc.perform(post("/api/repair-orders").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("\"MAINTENANCE\"", ",\"reportedIssue\":\"  Mantención preventiva  \"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportedIssue").value("Mantención preventiva"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"OTHER\"", "\"\""})
    void rejectsInvalidServiceTypeSafely(String type) throws Exception {
        mockMvc.perform(post("/api/repair-orders").contentType(MediaType.APPLICATION_JSON)
                        .content(payload(type, ",\"reportedIssue\":\"Issue\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(type.equals("null")
                        ? "Request validation failed." : "Request body is missing or malformed."))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void rejectsLegacyRequestWithoutServiceType() throws Exception {
        mockMvc.perform(post("/api/repair-orders").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("null", ",\"reportedIssue\":\"Issue\"")
                                .replace(",\"serviceType\":null", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.serviceType").exists());
    }

    private String payload(String type, String issueField) {
        return """
                {"customerName":"Juan Pérez","customerContact":"+56912345678",
                 "heaterBrand":"Junkers","heaterModel":"WR11","serviceType":%s%s}
                """.formatted(type, issueField);
    }
}
