package com.opspulse.order.infrastructure.persistence.jpa;

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
@Table(name = "order_items")
class OrderItemJpaEntity {

    @Id UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    OrderJpaEntity order;
    @Column(name = "product_id", nullable = false) UUID productId;
    @Column(nullable = false, precision = 18, scale = 3) BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4) BigDecimal unitPrice;

    protected OrderItemJpaEntity() {}
}
