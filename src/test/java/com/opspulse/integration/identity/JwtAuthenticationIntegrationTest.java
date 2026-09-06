package com.opspulse.integration.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import com.opspulse.shared.observability.RequestIdContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void protectedEndpointWithoutTokenReturnsJson401WithCorrelationId() throws Exception {
        String requestId = "missing-token-request";

        mockMvc.perform(post("/api/auth/register")
                        .header(RequestIdContext.HEADER_NAME, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistration("missing-token@opspulse.test")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(RequestIdContext.HEADER_NAME, requestId))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    @Test
    void malformedAccessTokenReturnsJson401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistration("malformed-token@opspulse.test")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void accessTokenWithInvalidSignatureReturnsJson401() throws Exception {
        String validToken = loginAdmin();
        int signatureStart = validToken.lastIndexOf('.') + 1;
        char replacement = validToken.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tamperedToken = validToken.substring(0, signatureStart)
                + replacement
                + validToken.substring(signatureStart + 1);

        mockMvc.perform(post("/api/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistration("invalid-signature@opspulse.test")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void expiredAccessTokenReturnsJson401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistration("expired-token@opspulse.test")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void accessTokenIsRejectedAfterCurrentUserIsDisabled() throws Exception {
        String accessToken = loginAdmin();
        jdbcTemplate.update(
                "update users set active = false where email = ?",
                "admin@opspulse.demo");
        try {
            mockMvc.perform(post("/api/auth/register")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRegistration("disabled-user@opspulse.test")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        } finally {
            jdbcTemplate.update(
                    "update users set active = true where email = ?",
                    "admin@opspulse.demo");
        }
    }

    @Test
    void refreshTokenCannotAuthenticateProtectedEndpoint() throws Exception {
        String refreshToken = loginAdminRefreshToken();

        mockMvc.perform(post("/api/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistration("refresh-bearer@opspulse.test")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String loginAdminRefreshToken() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@opspulse.demo","password":"OpsPulseDemo!2026"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("refreshToken")
                .asText();
    }

    private String loginAdmin() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@opspulse.demo","password":"OpsPulseDemo!2026"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    private String expiredAccessToken() {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject("10000000-0000-0000-0000-000000000001")
                .issuedAt(now.minusSeconds(180))
                .expiresAt(now.minusSeconds(120))
                .claim("email", "admin@opspulse.demo")
                .claim("roles", List.of("ADMIN"))
                .claim("token_use", "access")
                .id(UUID.randomUUID().toString())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

    private static String validRegistration(String email) {
        return """
                {
                  "email":"%s",
                  "password":"ValidPassword!2026",
                  "fullName":"JWT Test User",
                  "roles":["VIEWER"]
                }
                """.formatted(email);
    }
}
