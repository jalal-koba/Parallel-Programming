package com.example.ecosystem.nfr;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NFR #4 — REST surface for batch monitoring and summaries.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("nfr4")
class Nfr4BatchControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void jobHistoryEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/batch/job-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void summaryEndpointAcceptsDateParameter() throws Exception {
        mockMvc.perform(get("/batch/daily-sales/summary").param("date", "2026-05-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
