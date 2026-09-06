package com.opspulse.risk.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRiskEventRepository extends JpaRepository<RiskEventJpaEntity, UUID> {

    Optional<RiskEventJpaEntity> findFirstByDedupKeyAndStatusInOrderByCreatedAtDesc(
            String dedupKey, List<String> statuses);

    Optional<RiskEventJpaEntity> findFirstByDedupKeyOrderByCreatedAtDesc(String dedupKey);

    List<RiskEventJpaEntity> findByStatusIn(List<String> statuses);
}
