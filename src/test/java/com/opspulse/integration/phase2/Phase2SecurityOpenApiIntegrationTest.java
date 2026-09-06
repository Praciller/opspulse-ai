package com.opspulse.integration.phase2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class Phase2SecurityOpenApiIntegrationTest extends Phase2ApiIntegrationTestSupport {

    @Test
    void phase2EndpointsRequireAuthenticationAndApprovedWriteRoles() throws Exception {
        String requestId = "phase2-security-request";
        mockMvc.perform(get("/api/products").header(RequestIdContext.HEADER_NAME, requestId))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(RequestIdContext.HEADER_NAME, requestId))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").value(requestId));

        for (String email : java.util.List.of("manager@opspulse.demo", "viewer@opspulse.demo")) {
            mockMvc.perform(post("/api/products")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + login(email))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(productJson("FORBIDDEN-" + email.substring(0, email.indexOf('@')).toUpperCase())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Test
    void validationAndUniquenessUseStableErrorEnvelopes() throws Exception {
        String token = login("operator1@opspulse.demo");
        mockMvc.perform(get("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("DUPLICATE-SKU-001")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("duplicate-sku-001")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_SKU_DUPLICATE"));
    }

    @Test
    void openApiContainsOnlyImplementedPhase2GroupsWithBearerSecurity() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.type").value("http"))
                .andExpect(jsonPath("$.paths['/api/products']").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}/movements']").exists())
                .andExpect(jsonPath("$.paths['/api/suppliers']").exists())
                .andExpect(jsonPath("$.paths['/api/orders']").exists())
                .andExpect(jsonPath("$.paths['/api/inventory-movements']").exists())
                .andExpect(jsonPath("$.paths['/api/purchase-orders']").exists())
                .andExpect(jsonPath("$.paths['/api/products'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/products'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/products'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/products'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/products'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/inventory-movements'].post.responses['422']").exists())
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse").exists())
                .andExpect(jsonPath("$.components.schemas.CreateOrderRequest").exists())
                .andExpect(jsonPath("$.components.schemas.OrderItemRequest.properties.unitPrice").exists())
                .andExpect(jsonPath("$.components.schemas.OrderItemResponse.properties.lineTotal").exists())
                .andExpect(jsonPath("$.components.schemas.CreatePurchaseOrderRequest").exists())
                .andExpect(jsonPath("$.components.schemas.PurchaseOrderItemRequest.properties.unitCost").exists())
                .andExpect(jsonPath("$.components.schemas.PurchaseOrderItemResponse.properties.receivedQuantity").exists())
                .andExpect(jsonPath("$.paths['/api/risks']").exists())
                .andExpect(jsonPath("$.paths['/api/risks/scan']").exists())
                .andExpect(jsonPath("$.paths['/api/ai/recommendations']").exists())
                .andExpect(jsonPath("$.paths['/api/ai/recommendations/generate']").exists());
    }

    private static String productJson(String sku) {
        return """
                {"sku":"%s","name":"Security Product","category":"Security","unit":"PCS",
                 "currentStock":0,"safetyStock":0,"reorderPoint":0,"cost":1,"sellingPrice":2}
                """.formatted(sku);
    }
}
