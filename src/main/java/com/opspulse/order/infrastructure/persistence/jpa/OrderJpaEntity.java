package com.opspulse.order.infrastructure.persistence.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
class OrderJpaEntity {

    @Id UUID id;
    @Column(name = "order_number", nullable = false, unique = true, length = 64) String orderNumber;
    @Column(name = "customer_name", nullable = false, length = 200) String customerName;
    @Column(nullable = false, length = 16) String status;
    @Column(name = "expected_ship_date", nullable = false) LocalDate expectedShipDate;
    @Column(name = "actual_ship_date") LocalDate actualShipDate;
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 4) BigDecimal totalAmount;
    @Column(name = "organization_id") UUID organizationId;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "created_by") UUID createdBy;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(name = "updated_by") UUID updatedBy;
    @Version @Column(nullable = false) long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    List<OrderItemJpaEntity> items = new ArrayList<>();

    protected OrderJpaEntity() {}
}
