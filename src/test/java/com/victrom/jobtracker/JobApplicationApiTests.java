package com.victrom.jobtracker;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobApplicationApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsApplicationAndReadsItBack() throws Exception {
        long id = createApplication("Brightline", "APPLIED");

        mockMvc.perform(get("/api/applications/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.company").value("Brightline"))
                .andExpect(jsonPath("$.events[0].type").value("CREATED"));
    }

    @Test
    void rejectsInvalidSalaryRange() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("Salary Test", "APPLIED", 80000, 50000)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business rule"));
    }

    @Test
    void blocksIllegalPipelineJump() throws Exception {
        long id = createApplication("Leap Co", "SAVED");

        mockMvc.perform(patch("/api/applications/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFFER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.from").value("SAVED"))
                .andExpect(jsonPath("$.to").value("OFFER"));
    }

    @Test
    void statsEndpointReturnsFunnel() throws Exception {
        createApplication("Stats Co", "APPLIED");

        mockMvc.perform(get("/api/applications/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.funnel").isArray());
    }

    @Test
    void addsTimelineNote() throws Exception {
        long id = createApplication("Note Co", "APPLIED");

        mockMvc.perform(post("/api/applications/" + id + "/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Recruiter called back.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[*].type", hasItem("NOTE_ADDED")));
    }

    private long createApplication(String company, String status) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(company, status, 40000, 50000)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.application.id");
        return id.longValue();
    }

    private String payload(String company, String status, int min, int max) {
        return """
                {
                  "company": "%s",
                  "role": "Java Developer",
                  "status": "%s",
                  "appliedDate": "%s",
                  "location": "London",
                  "salaryMin": %d,
                  "salaryMax": %d,
                  "source": "LINKEDIN",
                  "priority": "MEDIUM",
                  "tags": ["java"]
                }
                """.formatted(company, status, LocalDate.now(), min, max);
    }
}
