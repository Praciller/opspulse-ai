package com.opspulse.integration.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest(properties = "opspulse.security.registration-mode=PUBLIC_VIEWER")
@AutoConfigureMockMvc
class PublicRegistrationIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicRegistrationCreatesViewerOnly() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"public-viewer@opspulse.test",
                                  "password":"ValidPassword!2026",
                                  "fullName":"Public Viewer"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("VIEWER"));
    }

    @Test
    void publicRegistrationCannotAssignElevatedRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"public-admin@opspulse.test",
                                  "password":"ValidPassword!2026",
                                  "fullName":"Public Admin Attempt",
                                  "roles":["ADMIN"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void publicRegistrationOpenApiDoesNotRequireBearerToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.security").isEmpty());
    }
}
