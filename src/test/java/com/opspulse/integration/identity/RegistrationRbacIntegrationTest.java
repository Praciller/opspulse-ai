package com.opspulse.integration.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import com.opspulse.shared.observability.RequestIdContext;
import org.junit.jupiter.api.Test;
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
class RegistrationRbacIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insufficientRoleReturnsJson403() throws Exception {
        String viewerToken = login("viewer@opspulse.demo");

        mockMvc.perform(post("/api/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(
                                "viewer-denied@opspulse.test", "[\"VIEWER\"]")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanCreateUserWithApprovedRole() throws Exception {
        String adminToken = login("admin@opspulse.demo");
        String requestId = "admin-registration-request";

        var result = mockMvc.perform(post("/api/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .header(RequestIdContext.HEADER_NAME, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(
                                "admin-created@opspulse.test", "[\"OPERATOR\"]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("admin-created@opspulse.test"))
                .andExpect(jsonPath("$.roles[0]").value("OPERATOR"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        String userId = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
        String passwordHash = jdbcTemplate.queryForObject(
                "select password_hash from users where id = ?::uuid", String.class, userId);
        var audit = jdbcTemplate.queryForMap(
                """
                select actor_user_id::text as actor_user_id,
                       entity_id::text as entity_id,
                       after_snapshot::text as after_snapshot,
                       request_id
                  from audit_logs
                 where action = 'auth.register' and request_id = ?
                """,
                requestId);

        assertThat(passwordHash)
                .startsWith("$2")
                .doesNotContain("ValidPassword!2026");
        assertThat(audit.get("actor_user_id"))
                .isEqualTo("10000000-0000-0000-0000-000000000001");
        assertThat(audit.get("entity_id")).isEqualTo(userId);
        assertThat(audit.get("request_id")).isEqualTo(requestId);
        assertThat(audit.get("after_snapshot").toString())
                .contains("admin-created@opspulse.test", "OPERATOR")
                .doesNotContain("ValidPassword!2026", "password", "token", "secret");
    }

    @Test
    void duplicateRegistrationReturnsConflict() throws Exception {
        String adminToken = login("admin@opspulse.demo");

        mockMvc.perform(post("/api/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("duplicate-register@opspulse.test", "[\"VIEWER\"]")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("duplicate-register@opspulse.test", "[\"VIEWER\"]")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDENTITY_CONFLICT"));
    }

    @Test
    void invalidRegistrationIsRejectedWithoutPersistingUser() throws Exception {
        String adminToken = login("admin@opspulse.demo");

        mockMvc.perform(post("/api/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"invalid-registration@opspulse.test",
                                  "password":"too-short",
                                  "fullName":"Invalid Registration"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields[0].field").value("password"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from users where email = ?",
                Integer.class,
                "invalid-registration@opspulse.test");
        assertThat(count).isZero();
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

    private static String registrationBody(String email, String roles) {
        return """
                {
                  "email":"%s",
                  "password":"ValidPassword!2026",
                  "fullName":"RBAC Test User",
                  "roles":%s
                }
                """.formatted(email, roles);
    }
}
