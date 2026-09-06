package com.opspulse.purchase.infrastructure.persistence.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders")
class PurchaseOrderJpaEntity {
    @Id UUID id;
    @Column(name = "po_number", nullable = false, unique = true, length = 64) String poNumber;
    @Column(name = "supplier_id", nullable = false) UUID supplierId;
    @Column(nullable = false, length = 20) String status;
    @Column(name = "expected_delivery_date", nullable = false) LocalDate expectedDeliveryDate;
    @Column(name = "actual_delivery_date") LocalDate actualDeliveryDate;
    @Column(name = "organization_id") UUID organizationId;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "created_by") UUID createdBy;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(name = "updated_by") UUID updatedBy;
    @Version @Column(nullable = false) long version;
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PurchaseOrderItemJpaEntity> items = new ArrayList<>();
    protected PurchaseOrderJpaEntity() {}
}
