package com.opspulse.unit.phase2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.opspulse.inventory.domain.MovementType;
import com.opspulse.order.domain.InvalidOrderStatusTransitionException;
import com.opspulse.order.domain.Order;
import com.opspulse.order.domain.OrderItem;
import com.opspulse.order.domain.OrderStatus;
import com.opspulse.product.domain.Product;
import com.opspulse.purchase.domain.InvalidPurchaseOrderStatusTransitionException;
import com.opspulse.purchase.domain.PurchaseOrder;
import com.opspulse.purchase.domain.PurchaseOrderItem;
import com.opspulse.purchase.domain.PurchaseOrderStatus;
import com.opspulse.supplier.domain.Supplier;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Phase2DomainTest {

    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");
    private static final UUID ACTOR = UUID.fromString("10000000-0000-0000-0000-000000000003");

    @Test
    void productNormalizesSkuAndRejectsDirectInitialStock() {
        var product = Product.create(UUID.randomUUID(), " widget-1 ", "Widget", null, "pcs",
                BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, ACTOR, NOW);
        assertThat(product.sku()).isEqualTo("WIDGET-1");
        assertThat(product.unit()).isEqualTo("PCS");
        assertThatThrownBy(() -> Product.create(UUID.randomUUID(), "X", "X", null, "PCS",
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, ACTOR, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void supplierValidatesContactLeadTimeAndSla() {
        assertThatThrownBy(() -> Supplier.create(UUID.randomUUID(), "Supplier", Map.of("secret", "x"),
                BigDecimal.ONE, 1, ACTOR, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Supplier.create(UUID.randomUUID(), "Supplier", Map.of(),
                new BigDecimal("-1"), -1, ACTOR, NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void orderCalculatesTotalsAndControlsTransitions() {
        UUID productId = UUID.randomUUID();
        var order = Order.create(UUID.randomUUID(), "SO-1", "Customer", LocalDate.of(2026, 7, 20),
                List.of(new OrderItem(UUID.randomUUID(), productId, new BigDecimal("2.5"), new BigDecimal("4.20"))), ACTOR, NOW);
        assertThat(order.totalAmount()).isEqualByComparingTo("10.5");
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.SHIPPED, LocalDate.of(2026, 7, 20), ACTOR, NOW))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    void purchaseOrderCalculatesTotalsAndControlsReceiptState() {
        UUID productId = UUID.randomUUID();
        var order = PurchaseOrder.create(UUID.randomUUID(), "PO-1", UUID.randomUUID(), LocalDate.of(2026, 7, 20),
                List.of(new PurchaseOrderItem(UUID.randomUUID(), productId, new BigDecimal("3"), new BigDecimal("2.50"), BigDecimal.ZERO)), ACTOR, NOW);
        assertThat(order.totalAmount()).isEqualByComparingTo("7.5");
        assertThatThrownBy(() -> order.receive(Map.of(productId, BigDecimal.ONE), null, ACTOR, NOW))
                .isInstanceOf(InvalidPurchaseOrderStatusTransitionException.class);
        assertThat(order.transitionTo(PurchaseOrderStatus.SENT, ACTOR, NOW).status()).isEqualTo(PurchaseOrderStatus.SENT);
    }

    @Test
    void movementTypesProduceApprovedSignedQuantities() {
        assertThat(MovementType.INBOUND.signedQuantity(new BigDecimal("2.5"))).isEqualByComparingTo("2.5");
        assertThat(MovementType.OUTBOUND.signedQuantity(new BigDecimal("2.5"))).isEqualByComparingTo("-2.5");
        assertThat(MovementType.ADJUSTMENT.signedQuantity(new BigDecimal("-1"))).isEqualByComparingTo("-1");
        assertThatThrownBy(() -> MovementType.RETURN.signedQuantity(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }
}
