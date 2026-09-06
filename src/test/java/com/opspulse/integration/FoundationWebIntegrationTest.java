package com.opspulse.integration;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import com.opspulse.shared.observability.RequestIdContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(FoundationWebIntegrationTest.TestController.class)
class FoundationWebIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void returnsContractErrorEnvelopeForValidationFailure() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .header(RequestIdContext.HEADER_NAME, "validation-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(RequestIdContext.HEADER_NAME, "validation-request"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fields[0].field").value("name"))
                .andExpect(jsonPath("$.fields[0].message").value("must not be blank"))
                .andExpect(jsonPath("$.requestId").value("validation-request"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    void sanitizesUnexpectedErrors() throws Exception {
        mockMvc.perform(get("/test/failure")
                        .header(RequestIdContext.HEADER_NAME, "failure-request"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("sensitive detail"))))
                .andExpect(jsonPath("$.requestId").value("failure-request"));
    }

    @Test
    void exposesFoundationEndpointsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }

    @Test
    void returnsContractEnvelopeForUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/future")
                        .header(RequestIdContext.HEADER_NAME, "security-request"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(RequestIdContext.HEADER_NAME, "security-request"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").value("security-request"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exposesPrometheusMetricsToAdministrators() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_")));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @PostMapping("/validation")
        void validate(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/failure")
        void fail() {
            throw new IllegalStateException("sensitive detail");
        }
    }

    record TestRequest(@NotBlank String name) {
    }
}
