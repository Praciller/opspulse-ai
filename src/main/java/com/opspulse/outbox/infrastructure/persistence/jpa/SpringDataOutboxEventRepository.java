package com.opspulse.outbox.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {}
