package com.opspulse.integration.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthOpenApiIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsAuthenticationEndpointsAndBearerJwtScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/refresh'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post").exists())
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme")
                        .value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].bearerFormat")
                        .value("JWT"))
                .andExpect(jsonPath(
                                "$.paths['/api/auth/register'].post.security[0]['bearer-jwt']")
                        .isArray())
                .andExpect(jsonPath(
                                "$.paths['/api/audit-logs'].get.security[0]['bearer-jwt']")
                        .isArray());
    }
}
