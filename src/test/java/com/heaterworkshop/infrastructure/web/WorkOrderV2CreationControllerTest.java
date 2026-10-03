package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.application.service.WorkOrderEquipmentServiceUseCases;
import com.heaterworkshop.application.usecase.CompleteWorkUseCase;
import com.heaterworkshop.application.usecase.CreateWorkOrderUseCase;
import com.heaterworkshop.application.usecase.GetWorkOrderUseCase;
import com.heaterworkshop.application.usecase.ListWorkOrdersUseCase;
import com.heaterworkshop.application.usecase.StartWorkUseCase;
import com.heaterworkshop.application.usecase.WorkOrderEquipmentWorkflowUseCase;
import com.heaterworkshop.application.usecase.WorkOrderWorkflowUseCase;
import com.heaterworkshop.infrastructure.persistence.InMemoryWorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkOrderV2CreationControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InMemoryWorkOrderRepository repository = new InMemoryWorkOrderRepository();

        WorkOrderController controller = new WorkOrderController(
                new CreateWorkOrderUseCase(repository),
                new ListWorkOrdersUseCase(repository),
                new GetWorkOrderUseCase(repository),
                new StartWorkUseCase(repository),
                new CompleteWorkUseCase(repository, (destination, message) -> { }),
                new WorkOrderWorkflowUseCase(repository),
                mock(WorkOrderEquipmentServiceUseCases.class),
                new WorkOrderEquipmentWorkflowUseCase(repository));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createsV2WorkOrderWithIndependentEquipmentIntakeRoutes() throws Exception {
        var result = mockMvc.perform(post("/api/work-orders/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName":"Maria Gonzalez",
                                  "customerContact":"+56911112222",
                                  "equipments":[
                                    {
                                      "type":"CALEFONT",
                                      "brand":"Junkers",
                                      "model":"WR11",
                                      "capacity":"11 L",
                                      "serialNumber":"SN-001",
                                      "position":1,
                                      "intakeRoute":"DIRECT_SERVICE"
                                    },
                                    {
                                      "type":"CALEFONT",
                                      "brand":"Bosch",
                                      "model":"Therm 5700",
                                      "capacity":"10 L",
                                      "position":2,
                                      "intakeRoute":"DIAGNOSIS_REQUIRED",
                                      "reportedIssue":"Does not ignite"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lifecycleVersion").value("V2"))
                .andExpect(jsonPath("$.serviceType").doesNotExist())
                .andExpect(jsonPath("$.reportedIssue").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.equipments.length()").value(2))

                .andExpect(jsonPath("$.equipments[0].type").value("CALEFONT"))
                .andExpect(jsonPath("$.equipments[0].intakeRoute").value("DIRECT_SERVICE"))
                .andExpect(jsonPath("$.equipments[0].status").value("RECEIVED"))
                .andExpect(jsonPath("$.equipments[0].reportedIssue").doesNotExist())

                .andExpect(jsonPath("$.equipments[1].type").value("CALEFONT"))
                .andExpect(jsonPath("$.equipments[1].intakeRoute").value("DIAGNOSIS_REQUIRED"))
                .andExpect(jsonPath("$.equipments[1].status").value("RECEIVED"))
                .andExpect(jsonPath("$.equipments[1].reportedIssue").value("Does not ignite"))
                .andReturn();

        String location = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(location.contains("\"id\":\"ORDER-"));
    }

    @Test
    void persistsV2WorkOrderAndReturnsItThroughExistingGetEndpoint() throws Exception {
        var creation = mockMvc.perform(post("/api/work-orders/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName":"Maria Gonzalez",
                                  "customerContact":"+56911112222",
                                  "equipments":[
                                    {
                                      "type":"CALEFONT",
                                      "brand":"Junkers",
                                      "model":"WR11",
                                      "position":1,
                                      "intakeRoute":"DIRECT_SERVICE"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String body = creation.getResponse().getContentAsString();
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"").matcher(body);

        if (!matcher.find()) {
            throw new AssertionError("Response did not contain an ID: " + body);
        }

        String id = matcher.group(1);

        mockMvc.perform(get("/api/work-orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.lifecycleVersion").value("V2"))
                .andExpect(jsonPath("$.equipments[0].intakeRoute").value("DIRECT_SERVICE"))
                .andExpect(jsonPath("$.equipments[0].status").value("RECEIVED"));
    }

    @Test
    void rejectsV2WorkOrderWithoutEquipments() throws Exception {
        mockMvc.perform(post("/api/work-orders/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName":"Maria Gonzalez",
                                  "customerContact":"+56911112222",
                                  "equipments":[]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDiagnosisRequiredEquipmentWithoutReportedIssue() throws Exception {
        mockMvc.perform(post("/api/work-orders/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName":"Maria Gonzalez",
                                  "customerContact":"+56911112222",
                                  "equipments":[
                                    {
                                      "type":"CALEFONT",
                                      "brand":"Bosch",
                                      "model":"Therm 5700",
                                      "position":1,
                                      "intakeRoute":"DIAGNOSIS_REQUIRED"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

}
