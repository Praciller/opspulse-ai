package com.opspulse.product.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
class ProductJpaEntity {

    @Id UUID id;
    @Column(nullable = false, unique = true, length = 64) String sku;
    @Column(nullable = false, length = 200) String name;
    @Column(length = 80) String category;
    @Column(nullable = false, length = 20) String unit;
    @Column(name = "current_stock", nullable = false, precision = 18, scale = 3) BigDecimal currentStock;
    @Column(name = "safety_stock", nullable = false, precision = 18, scale = 3) BigDecimal safetyStock;
    @Column(name = "reorder_point", nullable = false, precision = 18, scale = 3) BigDecimal reorderPoint;
    @Column(nullable = false, precision = 18, scale = 4) BigDecimal cost;
    @Column(name = "selling_price", nullable = false, precision = 18, scale = 4) BigDecimal sellingPrice;
    @Column(nullable = false) boolean active;
    @Column(name = "organization_id") UUID organizationId;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "created_by") UUID createdBy;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(name = "updated_by") UUID updatedBy;
    @Version @Column(nullable = false) long version;

    protected ProductJpaEntity() {}
}
