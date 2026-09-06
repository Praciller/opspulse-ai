package com.opspulse.integration.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspulse.integration.support.Phase2ApiIntegrationTestSupport;
import com.opspulse.shared.observability.RequestIdContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ProductApiIntegrationTest extends Phase2ApiIntegrationTestSupport {

    @Test
    void operatorCreatesProductWithAuditAndOutbox() throws Exception {
        String requestId = "product-create-request";
        var result = mockMvc.perform(post("/api/products")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + login("operator1@opspulse.demo"))
                        .header(RequestIdContext.HEADER_NAME, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku":"widget-001",
                                  "name":"Widget One",
                                  "category":"Widgets",
                                  "unit":"pcs",
                                  "currentStock":0,
                                  "safetyStock":5,
                                  "reorderPoint":10,
                                  "cost":12.5000,
                                  "sellingPrice":19.9900
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(RequestIdContext.HEADER_NAME, requestId))
                .andExpect(jsonPath("$.sku").value("WIDGET-001"))
                .andExpect(jsonPath("$.unit").value("PCS"))
                .andExpect(jsonPath("$.currentStock").value(0))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();

        String productId = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
        Integer audits = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where action = 'product.create' and request_id = ?",
                Integer.class,
                requestId);
        Integer events = jdbcTemplate.queryForObject(
                """
                select count(*) from outbox_events
                 where aggregate_id = ?::uuid
                   and event_type = 'opspulse.product.created'
                   and status = 'NEW'
                """,
                Integer.class,
                productId);

        assertThat(audits).isEqualTo(1);
        assertThat(events).isEqualTo(1);
        String payload = jdbcTemplate.queryForObject(
                "select payload::text from outbox_events where aggregate_id = ?::uuid and event_type = 'opspulse.product.created'",
                String.class,
                productId);
        assertThat(objectMapper.readTree(payload).get("specversion").asText()).isEqualTo("1.0");
        assertThat(objectMapper.readTree(payload).get("data").has("currentStock")).isTrue();
    }

    @Test
    void productLifecycleSupportsReadSearchUpdateAndSoftDeactivate() throws Exception {
        String operatorToken = login("operator1@opspulse.demo");
        var created = mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku":"lifecycle-001",
                                  "name":"Lifecycle Widget",
                                  "category":"Widgets",
                                  "unit":"PCS",
                                  "currentStock":0,
                                  "safetyStock":5,
                                  "reorderPoint":10,
                                  "cost":10,
                                  "sellingPrice":15
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        var createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
        String productId = createdJson.get("id").asText();

        mockMvc.perform(get("/api/products/{id}", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login("viewer@opspulse.demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("LIFECYCLE-001"));

        mockMvc.perform(get("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .param("search", "lifecycle")
                        .param("sort", "name,asc")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(productId))
                .andExpect(jsonPath("$.total").value(1));

        var updated = mockMvc.perform(put("/api/products/{id}", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .header(RequestIdContext.HEADER_NAME, "product-update-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Lifecycle Widget Updated",
                                  "category":"Updated",
                                  "unit":"BOX",
                                  "safetyStock":6,
                                  "reorderPoint":12,
                                  "cost":11,
                                  "sellingPrice":17,
                                  "version":0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lifecycle Widget Updated"))
                .andExpect(jsonPath("$.currentStock").value(0))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn();

        long updatedVersion = objectMapper
                .readTree(updated.getResponse().getContentAsString())
                .get("version")
                .asLong();
        mockMvc.perform(delete("/api/products/{id}", productId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + login("admin@opspulse.demo"))
                        .param("version", Long.toString(updatedVersion)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{id}", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
