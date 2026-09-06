package com.opspulse.purchase.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_items")
class PurchaseOrderItemJpaEntity {
    @Id UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    PurchaseOrderJpaEntity purchaseOrder;
    @Column(name = "product_id", nullable = false) UUID productId;
    @Column(nullable = false, precision = 18, scale = 3) BigDecimal quantity;
    @Column(name = "unit_cost", nullable = false, precision = 18, scale = 4) BigDecimal unitCost;
    @Column(name = "received_quantity", nullable = false, precision = 18, scale = 3) BigDecimal receivedQuantity;
    protected PurchaseOrderItemJpaEntity() {}
}
