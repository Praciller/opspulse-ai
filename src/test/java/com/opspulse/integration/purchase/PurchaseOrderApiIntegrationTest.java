package com.opspulse.integration.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspulse.integration.support.Phase2ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseOrderApiIntegrationTest extends Phase2ApiIntegrationTestSupport {

    @Test
    void purchaseOrderLifecycleReceivesInventoryWithoutDoubleCounting() throws Exception {
        String operatorToken = login("operator1@opspulse.demo");
        String supplierId = createSupplier(operatorToken);
        String productId = createProduct(operatorToken);

        var created = mockMvc.perform(post("/api/purchase-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "poNumber":"PO-2026-001",
                                  "supplierId":"%s",
                                  "expectedDeliveryDate":"2026-07-20",
                                  "items":[{"productId":"%s","quantity":10,"unitCost":8.5000}]
                                }
                                """.formatted(supplierId, productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items[0].receivedQuantity").value(0))
                .andReturn();
        String purchaseOrderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/purchase-orders/{id}", purchaseOrderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId":"%s",
                                  "expectedDeliveryDate":"2026-07-21",
                                  "items":[{"productId":"%s","quantity":10,"unitCost":8.2500}],
                                  "version":0
                                }
                                """.formatted(supplierId, productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(patch("/api/purchase-orders/{id}/status", purchaseOrderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login("manager@opspulse.demo"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SENT\",\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));

        receive(operatorToken, purchaseOrderId, productId, "4", 2, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_RECEIVED"))
                .andExpect(jsonPath("$.items[0].receivedQuantity").value(4));

        receive(operatorToken, purchaseOrderId, productId, "6", 3, "2026-07-14")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.actualDeliveryDate").value("2026-07-14"))
                .andExpect(jsonPath("$.items[0].receivedQuantity").value(10));

        receive(operatorToken, purchaseOrderId, productId, "1", 4, "2026-07-14")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        assertThat(jdbcTemplate.queryForObject("select current_stock from products where id = ?::uuid", java.math.BigDecimal.class, productId))
                .isEqualByComparingTo("10");
        assertThat(jdbcTemplate.queryForObject("select count(*) from inventory_movements where reference_type = 'PURCHASE_ORDER' and reference_id = ?::uuid", Integer.class, purchaseOrderId))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_logs where entity_type = 'PURCHASE_ORDER' and entity_id = ?::uuid", Integer.class, purchaseOrderId))
                .isEqualTo(5);
    }

    private org.springframework.test.web.servlet.ResultActions receive(
            String token, String poId, String productId, String quantity, long version, String actualDate) throws Exception {
        String dateField = actualDate == null ? "" : ",\"actualDeliveryDate\":\"" + actualDate + "\"";
        return mockMvc.perform(post("/api/purchase-orders/{id}/receive", poId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"items":[{"productId":"%s","quantity":%s}],"version":%d%s}
                        """.formatted(productId, quantity, version, dateField)));
    }

    private String createSupplier(String token) throws Exception {
        var result = mockMvc.perform(post("/api/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Purchase Supplier","contact":{"email":"po@example.com"},"expectedSlaDays":5}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createProduct(String token) throws Exception {
        var result = mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"PURCHASE-001","name":"Purchase Product","category":"Purchase","unit":"PCS",
                                 "currentStock":0,"safetyStock":0,"reorderPoint":0,"cost":8,"sellingPrice":12}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
