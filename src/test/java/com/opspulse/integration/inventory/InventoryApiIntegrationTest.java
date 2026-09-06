package com.opspulse.integration.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspulse.integration.support.Phase2ApiIntegrationTestSupport;
import com.opspulse.inventory.application.port.in.InventoryUseCase;
import com.opspulse.inventory.domain.InventoryMovement;
import com.opspulse.inventory.domain.MovementType;
import com.opspulse.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class InventoryApiIntegrationTest extends Phase2ApiIntegrationTestSupport {

    @Autowired
    private InventoryUseCase inventory;

    @Test
    void movementsUpdateStockAtomicallyAndRejectNegativeBalance() throws Exception {
        String operatorToken = login("operator1@opspulse.demo");
        String productId = createProduct(operatorToken, "INVENTORY-API-001");

        createMovement(operatorToken, productId, "INBOUND", "10")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.balanceBefore").value(0))
                .andExpect(jsonPath("$.balanceAfter").value(10));

        createMovement(operatorToken, productId, "OUTBOUND", "4")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(-4))
                .andExpect(jsonPath("$.balanceBefore").value(10))
                .andExpect(jsonPath("$.balanceAfter").value(6));

        mockMvc.perform(get("/api/inventory-movements/product/{id}", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login("viewer@opspulse.demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(6))
                .andExpect(jsonPath("$.movements.total").value(2));

        mockMvc.perform(get("/api/products/{id}/movements", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login("viewer@opspulse.demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        createMovement(operatorToken, productId, "OUTBOUND", "7")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NEGATIVE_STOCK_BLOCKED"));

        mockMvc.perform(delete("/api/products/{id}", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login("admin@opspulse.demo"))
                        .param("version", "2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_HAS_MOVEMENTS"));

        assertThat(jdbcTemplate.queryForObject(
                        "select current_stock from products where id = ?::uuid",
                        java.math.BigDecimal.class,
                        productId))
                .isEqualByComparingTo("6");
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from inventory_movements where product_id = ?::uuid",
                        Integer.class,
                        productId))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from audit_logs where action = 'inventory.movement' and entity_id = ?::uuid",
                        Integer.class,
                        productId))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from outbox_events where aggregate_id = ?::uuid and event_type = 'opspulse.product.stock.changed'",
                        Integer.class,
                        productId))
                .isEqualTo(2);
    }

    @Test
    void concurrentOutboundMovementsSerializeOnTheProductRow() throws Exception {
        String productId = createProduct(login("operator1@opspulse.demo"), "INVENTORY-RACE-001");
        UUID productUuid = UUID.fromString(productId);
        UUID operatorId = UUID.fromString("10000000-0000-0000-0000-000000000003");
        inventory.createMovement(command(productUuid, MovementType.INBOUND, "10", operatorId, "race-seed"));

        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> concurrentOutbound(productUuid, operatorId, ready, start, "race-first"));
            var second = executor.submit(() -> concurrentOutbound(productUuid, operatorId, ready, start, "race-second"));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            var results = java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertThat(results).filteredOn(InventoryMovement.class::isInstance).hasSize(1);
            assertThat(results).filteredOn(ApiException.class::isInstance).singleElement().satisfies(result ->
                    assertThat(((ApiException) result).errorCode()).isEqualTo(com.opspulse.shared.error.ErrorCode.NEGATIVE_STOCK_BLOCKED));
        }

        assertThat(jdbcTemplate.queryForObject("select current_stock from products where id = ?::uuid", BigDecimal.class, productId))
                .isEqualByComparingTo("3");
        assertThat(jdbcTemplate.queryForObject("select count(*) from inventory_movements where product_id = ?::uuid", Integer.class, productId))
                .isEqualTo(2);
    }

    private org.springframework.test.web.servlet.ResultActions createMovement(
            String token, String productId, String type, String quantity) throws Exception {
        return mockMvc.perform(post("/api/inventory-movements")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "productId":"%s",
                          "movementType":"%s",
                          "quantity":%s,
                          "reason":"integration test"
                        }
                        """.formatted(productId, type, quantity)));
    }

    private Object concurrentOutbound(UUID productId, UUID actor, CountDownLatch ready, CountDownLatch start, String requestId) {
        ready.countDown();
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("concurrent inventory test did not start");
            }
            return inventory.createMovement(command(productId, MovementType.OUTBOUND, "7", actor, requestId));
        } catch (ApiException exception) {
            return exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static InventoryUseCase.CreateMovementCommand command(
            UUID productId, MovementType type, String quantity, UUID actor, String requestId) {
        return new InventoryUseCase.CreateMovementCommand(productId, type, new BigDecimal(quantity), "concurrency test", null, null,
                actor, requestId, "127.0.0.1");
    }

    private String createProduct(String token, String sku) throws Exception {
        var result = mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku":"%s","name":"Inventory Product","category":"Inventory","unit":"PCS",
                                  "currentStock":0,"safetyStock":2,"reorderPoint":5,"cost":10,"sellingPrice":15
                                }
                                """.formatted(sku)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
