package com.opspulse.integration.audit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import com.opspulse.shared.observability.RequestIdContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuditLogIntegrationTest extends PostgresIntegrationTestSupport {

    private static final UUID PRODUCT_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void adminCanListFilteredAuditLogsWithSnapshots() throws Exception {
        UUID auditId = UUID.randomUUID();
        insertAuditRow(auditId);

        mockMvc.perform(get("/api/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login("admin@opspulse.demo"))
                        .header(RequestIdContext.HEADER_NAME, "audit-list-request")
                        .param("entityType", "PRODUCT")
                        .param("entityId", PRODUCT_ID.toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdContext.HEADER_NAME, "audit-list-request"))
                .andExpect(jsonPath("$.items[0].id").value(auditId.toString()))
                .andExpect(jsonPath("$.items[0].beforeSnapshot.sellingPrice").value(18.99))
                .andExpect(jsonPath("$.items[0].afterSnapshot.sellingPrice").value(19.99))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void managerCanGetAuditLogById() throws Exception {
        UUID auditId = UUID.randomUUID();
        insertAuditRow(auditId);

        mockMvc.perform(get("/api/audit-logs/{id}", auditId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + login("manager@opspulse.demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(auditId.toString()))
                .andExpect(jsonPath("$.action").value("product.update"))
                .andExpect(jsonPath("$.entityType").value("PRODUCT"))
                .andExpect(jsonPath("$.entityId").value(PRODUCT_ID.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"operator1@opspulse.demo", "viewer@opspulse.demo"})
    void restrictedRolesCannotReadAuditLogs(String email) throws Exception {
        String requestId = "audit-denied-" + email.substring(0, email.indexOf('@'));
        mockMvc.perform(get("/api/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login(email))
                        .header(RequestIdContext.HEADER_NAME, requestId))
                .andExpect(status().isForbidden())
                .andExpect(header().string(RequestIdContext.HEADER_NAME, requestId))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    private void insertAuditRow(UUID auditId) {
        jdbcTemplate.update(
                """
                insert into audit_logs (
                    id, actor_user_id, action, entity_type, entity_id,
                    before_snapshot, after_snapshot, request_id, ip_address, created_at
                ) values (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, now())
                """,
                auditId,
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                "product.update",
                "PRODUCT",
                PRODUCT_ID,
                "{\"sellingPrice\":18.99}",
                "{\"sellingPrice\":19.99}",
                "audit-source-request",
                "127.0.0.1");
    }

    private String login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"OpsPulseDemo!2026"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }
}
