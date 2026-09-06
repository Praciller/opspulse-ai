package com.opspulse.integration.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import com.opspulse.shared.observability.RequestIdContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest extends PostgresIntegrationTestSupport {

    private static final String DEMO_PASSWORD = "OpsPulseDemo!2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void successfulLoginReturnsDocumentedTokenResponseAndAuditRow() throws Exception {
        String requestId = "login-success-request";

        mockMvc.perform(post("/api/auth/login")
                        .header(RequestIdContext.HEADER_NAME, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@opspulse.demo", DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdContext.HEADER_NAME, requestId))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value("admin@opspulse.demo"))
                .andExpect(jsonPath("$.user.roles[0]").value("ADMIN"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where action = ? and request_id = ?",
                Integer.class,
                "auth.login.success",
                requestId);
        assertThat(count).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({
        "admin@opspulse.demo,ADMIN",
        "manager@opspulse.demo,MANAGER",
        "operator1@opspulse.demo,OPERATOR",
        "viewer@opspulse.demo,VIEWER"
    })
    void loginAndRefreshWorkForEveryRole(String email, String role) throws Exception {
        JsonNode login = login(email);

        assertThat(login.path("user").path("roles").get(0).asText()).isEqualTo(role);

        JsonNode refreshed = refresh(login.path("refreshToken").asText());
        assertThat(refreshed.path("user").path("roles").get(0).asText()).isEqualTo(role);
    }

    @Test
    void invalidCredentialsUseSameResponseAndPreserveCorrelationId() throws Exception {
        String requestId = "login-failure-request";

        var unknown = mockMvc.perform(post("/api/auth/login")
                        .header(RequestIdContext.HEADER_NAME, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("missing@opspulse.demo", "WrongPassword!2026")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(RequestIdContext.HEADER_NAME, requestId))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication failed"))
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andReturn();

        var wrongPassword = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@opspulse.demo", "WrongPassword!2026")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication failed"))
                .andReturn();

        assertThat(objectMapper.readTree(unknown.getResponse().getContentAsString()).get("message"))
                .isEqualTo(objectMapper
                        .readTree(wrongPassword.getResponse().getContentAsString())
                        .get("message"));
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where action = ? and request_id = ? "
                        + "and actor_user_id is null and entity_id is null",
                Integer.class,
                "auth.login.failure",
                requestId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void inactiveUserReceivesGenericAuthenticationFailure() throws Exception {
        jdbcTemplate.update(
                "update users set active = false where email = ?",
                "viewer@opspulse.demo");
        try {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("viewer@opspulse.demo", DEMO_PASSWORD)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Authentication failed"));
        } finally {
            jdbcTemplate.update(
                    "update users set active = true where email = ?",
                    "viewer@opspulse.demo");
        }
    }

    @Test
    void refreshRotatesTokenAndLogoutRevokesReplacement() throws Exception {
        JsonNode login = login("manager@opspulse.demo");
        String originalRefresh = login.get("refreshToken").asText();

        var refreshedResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(originalRefresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        JsonNode refreshed =
                objectMapper.readTree(refreshedResult.getResponse().getContentAsString());
        String replacementRefresh = refreshed.get("refreshToken").asText();
        assertThat(replacementRefresh).isNotEqualTo(originalRefresh);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(originalRefresh)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(replacementRefresh)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(replacementRefresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void replayedRefreshTokenRevokesEntireFamily() throws Exception {
        JsonNode login = login("operator1@opspulse.demo");
        String originalRefresh = login.get("refreshToken").asText();

        JsonNode rotated = refresh(originalRefresh);
        String replacementRefresh = rotated.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(originalRefresh)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(replacementRefresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutIsIdempotentAndDoesNotDiscloseTokenState() throws Exception {
        JsonNode login = login("admin@opspulse.demo");
        String refreshToken = login.get("refreshToken").asText();
        String accessToken = login.get("accessToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"not-a-jwt\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(accessToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void persistedRefreshTokenHashDoesNotContainRawToken() throws Exception {
        JsonNode login = login("viewer@opspulse.demo");
        String refreshToken = login.get("refreshToken").asText();

        List<String> storedHashes = jdbcTemplate.queryForList(
                "select token_hash from refresh_tokens", String.class);

        assertThat(storedHashes).isNotEmpty();
        assertThat(storedHashes).noneMatch(hash -> hash.contains(refreshToken));
        assertThat(storedHashes).allMatch(hash -> !refreshToken.contains(hash));
    }

    @Test
    void loginAuditRowsDoNotContainCredentialMaterial() throws Exception {
        String requestId = "audit-secret-check";
        mockMvc.perform(post("/api/auth/login")
                        .header(RequestIdContext.HEADER_NAME, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@opspulse.demo", DEMO_PASSWORD)))
                .andExpect(status().isOk());

        List<String> serializedRows = jdbcTemplate.queryForList(
                """
                select coalesce(before_snapshot::text, '')
                     || coalesce(after_snapshot::text, '')
                     || coalesce(action, '')
                     || coalesce(request_id, '')
                  from audit_logs
                 where request_id = ?
                """,
                String.class,
                requestId);

        assertThat(serializedRows).hasSize(1);
        assertThat(serializedRows.getFirst())
                .doesNotContain(DEMO_PASSWORD)
                .doesNotContain("refreshToken")
                .doesNotContain("accessToken");
    }

    private JsonNode refresh(String refreshToken) throws Exception {
        var result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String loginBody(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private static String refreshBody(String refreshToken) {
        return """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);
    }
}
