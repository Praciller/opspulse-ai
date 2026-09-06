package com.opspulse.supplier.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "suppliers")
class SupplierJpaEntity {
    @Id UUID id;
    @Column(nullable = false, length = 200) String name;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "contact_info", columnDefinition = "jsonb") Map<String, String> contactInfo;
    @Column(name = "average_lead_time_days", precision = 6, scale = 2) BigDecimal averageLeadTimeDays;
    @Column(name = "expected_sla_days", nullable = false) int expectedSlaDays;
    @Column(nullable = false) boolean active;
    @Column(name = "organization_id") UUID organizationId;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "created_by") UUID createdBy;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(name = "updated_by") UUID updatedBy;
    @Version @Column(nullable = false) long version;
    protected SupplierJpaEntity() {}
}
