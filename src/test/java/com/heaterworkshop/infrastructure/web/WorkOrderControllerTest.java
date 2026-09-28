package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.application.usecase.CompleteWorkUseCase;
import com.heaterworkshop.application.usecase.CreateWorkOrderUseCase;
import com.heaterworkshop.application.usecase.GetWorkOrderUseCase;
import com.heaterworkshop.application.usecase.ListWorkOrdersUseCase;
import com.heaterworkshop.application.usecase.StartWorkUseCase;
import com.heaterworkshop.application.usecase.WorkOrderWorkflowUseCase;
import com.heaterworkshop.infrastructure.persistence.InMemoryWorkOrderRepository;
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

class WorkOrderControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InMemoryWorkOrderRepository repository = new InMemoryWorkOrderRepository();
        WorkOrderController controller = new WorkOrderController(
                new CreateWorkOrderUseCase(repository), new ListWorkOrdersUseCase(repository),
                new GetWorkOrderUseCase(repository), new StartWorkUseCase(repository),
                new CompleteWorkUseCase(repository, (destination, message) -> { }), new WorkOrderWorkflowUseCase(repository));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"REPAIR", "MAINTENANCE"})
    void supportsTheCompleteWorkOrderLifecycle(String type) throws Exception {
        MvcResult creation = mockMvc.perform(post("/api/work-orders")
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

        mockMvc.perform(get("/api/work-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].serviceType").value(type));
        mockMvc.perform(get("/api/work-orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Maria Gonzalez"))
                .andExpect(jsonPath("$.serviceType").value(type));
        if (type.equals("REPAIR")) {
            mockMvc.perform(patch("/api/work-orders/{id}/start", id)).andExpect(status().isConflict());
            mockMvc.perform(patch("/api/work-orders/{id}/diagnosis/begin", id)).andExpect(status().isOk());
            mockMvc.perform(patch("/api/work-orders/{id}/diagnosis/complete", id)).andExpect(status().isBadRequest());
            mockMvc.perform(patch("/api/work-orders/{id}/diagnosis", id)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"diagnosis\":\"   \"}"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(patch("/api/work-orders/{id}/diagnosis", id)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"diagnosis\":\"Damaged sensor\"}"))
                    .andExpect(status().isOk());
            mockMvc.perform(patch("/api/work-orders/{id}/diagnosis/complete", id))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WAITING_CUSTOMER"));
            mockMvc.perform(patch("/api/work-orders/{id}/approve", id)
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(patch("/api/work-orders/{id}/approve", id)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"partsAvailable\":true}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.customerDecision").value("APPROVED"));
        } else {
            mockMvc.perform(patch("/api/work-orders/{id}/start", id))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.diagnosis").doesNotExist());
        }
        mockMvc.perform(patch("/api/work-orders/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.serviceType").value(type))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    void returnsContractErrorsForInvalidInputAndTransitions() throws Exception {
        mockMvc.perform(get("/api/work-orders/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").isMap());

        MvcResult creation = mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Maria Gonzalez","customerContact":"+56911112222",
                                 "heaterBrand":"Bosch","heaterModel":"Therm 5700","serviceType":"REPAIR","reportedIssue":"Turns off"}
                                """))
                .andReturn();
        String id = extractId(creation.getResponse().getContentAsString());
        mockMvc.perform(patch("/api/work-orders/{id}/complete", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/work-orders/" + id + "/complete"));
    }

    @Test
    void returnsNotFoundForAnUnknownValidIdentifier() throws Exception {
        String id = "ORDER-550E8400-E29B-41D4-A716-446655440404";

        mockMvc.perform(get("/api/work-orders/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/work-orders/" + id));
    }

    @Test
    void returnsFieldErrorsForAnInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.validationErrors.customerName").exists())
                .andExpect(jsonPath("$.validationErrors.customerContact").exists());
    }

    @Test
    void returnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/work-orders")
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
        mockMvc.perform(post("/api/work-orders").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("\"REPAIR\"", issueField)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Reported issue must not be blank."));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", ",\"reportedIssue\":null", ",\"reportedIssue\":\"\"", ",\"reportedIssue\":\"   \""})
    void acceptsMaintenanceWithoutObservations(String issueField) throws Exception {
        mockMvc.perform(post("/api/work-orders").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("\"MAINTENANCE\"", issueField)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceType").value("MAINTENANCE"))
                .andExpect(jsonPath("$.reportedIssue").value(""));
    }

    @Test
    void trimsMaintenanceObservations() throws Exception {
        mockMvc.perform(post("/api/work-orders").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("\"MAINTENANCE\"", ",\"reportedIssue\":\"  Mantención preventiva  \"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportedIssue").value("Mantención preventiva"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"OTHER\"", "\"\""})
    void rejectsInvalidServiceTypeSafely(String type) throws Exception {
        mockMvc.perform(post("/api/work-orders").contentType(MediaType.APPLICATION_JSON)
                        .content(payload(type, ",\"reportedIssue\":\"Issue\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(type.equals("null")
                        ? "Request validation failed." : "Request body is missing or malformed."))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void rejectsLegacyRequestWithoutServiceType() throws Exception {
        mockMvc.perform(post("/api/work-orders").contentType(MediaType.APPLICATION_JSON)
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

    private String createOrder(String type) throws Exception {
        var result = mockMvc.perform(post("/api/work-orders").contentType(MediaType.APPLICATION_JSON)
                .content(payload("\"" + type + "\"", ",\"reportedIssue\":\"Issue\"")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.lifecycleVersion").value("V1"))
                .andExpect(jsonPath("$.legacyStatus").doesNotExist()).andReturn();
        return extractId(result.getResponse().getContentAsString());
    }

    @Test void rejectsRepairAndKeepsAdministrativeClosureTerminal() throws Exception {
        String id = createOrder("REPAIR");
        mockMvc.perform(patch("/api/work-orders/{id}/diagnosis/begin", id)).andExpect(status().isOk());
        mockMvc.perform(patch("/api/work-orders/{id}/diagnosis", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"diagnosis\":\"Sensor\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/work-orders/{id}/diagnosis/complete", id)).andExpect(status().isOk());
        mockMvc.perform(patch("/api/work-orders/{id}/reject", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_APPROVED"))
                .andExpect(jsonPath("$.customerDecision").value("REJECTED"))
                .andExpect(jsonPath("$.completedAt").doesNotExist());
        mockMvc.perform(patch("/api/work-orders/{id}/start", id)).andExpect(status().isConflict());
        mockMvc.perform(patch("/api/work-orders/{id}/complete", id)).andExpect(status().isConflict());
        mockMvc.perform(get("/api/work-orders/{id}", id)).andExpect(jsonPath("$.status").value("NOT_APPROVED"));
    }

    @Test void maintenanceWaitsForPartsAndStartsWithoutDiagnosis() throws Exception {
        String id = createOrder("MAINTENANCE");
        mockMvc.perform(patch("/api/work-orders/{id}/diagnosis/begin", id)).andExpect(status().isConflict());
        mockMvc.perform(patch("/api/work-orders/{id}/waiting-parts", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_PARTS"));
        mockMvc.perform(patch("/api/work-orders/{id}/start", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").doesNotExist()).andExpect(jsonPath("$.customerDecision").doesNotExist());
        mockMvc.perform(patch("/api/work-orders/{id}/complete", id)).andExpect(status().isOk());
    }

    @Test void createCannotChooseLegacyProvenanceOrApproval() throws Exception {
        var result = mockMvc.perform(post("/api/work-orders").contentType(MediaType.APPLICATION_JSON)
                .content(payload("\"REPAIR\"", ",\"reportedIssue\":\"Issue\",\"lifecycleVersion\":\"LEGACY\",\"legacyStatus\":\"IN_PROGRESS\",\"customerDecision\":\"APPROVED\"")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.lifecycleVersion").value("V1"))
                .andExpect(jsonPath("$.legacyStatus").doesNotExist()).andExpect(jsonPath("$.customerDecision").doesNotExist()).andReturn();
        String id = extractId(result.getResponse().getContentAsString());
        mockMvc.perform(patch("/api/work-orders/{id}/start", id)).andExpect(status().isConflict());
        mockMvc.perform(patch("/api/work-orders/{id}/complete", id)).andExpect(status().isConflict());
    }
}
