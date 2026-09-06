package com.opspulse.integration.phase6;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspulse.integration.support.Phase2ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ReportApiIntegrationTest extends Phase2ApiIntegrationTestSupport {

    @Test
    void authenticatedViewerCanReadAllReports() throws Exception {
        String token = login("viewer@opspulse.demo");
        for (String path : new String[]{
                "/api/reports/daily-ops-brief", "/api/reports/inventory-risk", "/api/reports/supplier-sla",
                "/api/reports/order-delay", "/api/reports/product-margin"}) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.asOf").exists());
        }
    }
}
