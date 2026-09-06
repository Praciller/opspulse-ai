package com.opspulse.integration.phase7;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspulse.integration.support.Phase2ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class DashboardApiIntegrationTest extends Phase2ApiIntegrationTestSupport {

    @Test
    void authenticatedViewerCanReadDashboardSnapshot() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + login("viewer@opspulse.demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").isNumber())
                .andExpect(jsonPath("$.totalOpenOrders").isNumber())
                .andExpect(jsonPath("$.delayedOrdersCount").isNumber())
                .andExpect(jsonPath("$.openRiskEventsCount").isNumber())
                .andExpect(jsonPath("$.asOf").exists());
    }

    @Test
    void anonymousDashboardReadIsRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }
}
