package com.opspulse.integration.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final Path SNAPSHOT = Path.of("frontend", "public", "openapi.json");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void staticOpenApiSnapshotMatchesTheApplicationContract() throws Exception {
        byte[] response = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        ObjectNode actual = (ObjectNode) objectMapper.readTree(response);
        // Springdoc derives the server origin from the request. It is not part of the snapshot.
        actual.remove("servers");

        if ("true".equals(System.getenv("UPDATE_OPENAPI_SNAPSHOT"))) {
            Files.createDirectories(SNAPSHOT.getParent());
            Files.writeString(SNAPSHOT, objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(actual).replace("\r\n", "\n") + "\n");
            return;
        }

        assertTrue(Files.isRegularFile(SNAPSHOT), "Generate frontend/public/openapi.json as described in README.md");
        JsonNode published = objectMapper.readTree(Files.readAllBytes(SNAPSHOT));
        assertEquals(published, actual, "OpenAPI snapshot drifted; regenerate it as described in README.md");
    }

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
