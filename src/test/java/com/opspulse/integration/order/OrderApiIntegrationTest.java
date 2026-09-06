package com.opspulse.integration.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class OrderApiIntegrationTest extends Phase2ApiIntegrationTestSupport {

    @Test
    void orderLifecycleCalculatesTotalsAndGuardsStatusTransitions() throws Exception {
        String operatorToken = login("operator1@opspulse.demo");
        String productId = createProduct(operatorToken, "ORDER-PRODUCT-001");

        var created = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .header(RequestIdContext.HEADER_NAME, "order-create-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderNumber":"SO-2026-001",
                                  "customerName":"Acme Retail",
                                  "expectedShipDate":"2026-07-20",
                                  "items":[
                                    {"productId":"%s","quantity":3,"unitPrice":12.5000}
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.totalAmount").value(37.5))
                .andExpect(jsonPath("$.items[0].lineTotal").value(37.5))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();
        var createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
        String orderId = createdJson.get("id").asText();

        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login("viewer@opspulse.demo"))
                        .param("search", "acme")
                        .param("status", "NEW")
                        .param("sort", "orderNumber,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(orderId));

        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName":"Acme Retail Updated",
                                  "expectedShipDate":"2026-07-21",
                                  "items":[
                                    {"productId":"%s","quantity":4,"unitPrice":12.5000}
                                  ],
                                  "version":0
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(50.0))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED","version":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SHIPPED","actualShipDate":"2026-07-22","version":2}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PICKING","version":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKING"))
                .andExpect(jsonPath("$.version").value(3));

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SHIPPED","actualShipDate":"2026-07-22","version":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.actualShipDate").value("2026-07-22"));

        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from audit_logs where entity_type = 'ORDER' and entity_id = ?::uuid",
                        Integer.class,
                        orderId))
                .isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from outbox_events where aggregate_type = 'ORDER' and aggregate_id = ?::uuid",
                        Integer.class,
                        orderId))
                .isEqualTo(4);
    }

    private String createProduct(String token, String sku) throws Exception {
        var result = mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku":"%s","name":"Order Product","category":"Orders","unit":"PCS",
                                  "currentStock":0,"safetyStock":0,"reorderPoint":0,"cost":10,"sellingPrice":15
                                }
                                """.formatted(sku)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
