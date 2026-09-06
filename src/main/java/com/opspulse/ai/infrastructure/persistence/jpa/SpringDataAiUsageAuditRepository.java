package com.opspulse.ai.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAiUsageAuditRepository extends JpaRepository<AiUsageAuditJpaEntity, UUID> {}
