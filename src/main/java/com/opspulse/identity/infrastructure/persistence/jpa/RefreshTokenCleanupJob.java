package com.opspulse.identity.infrastructure.persistence.jpa;

import com.opspulse.identity.application.port.out.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class RefreshTokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;
    private final int retentionDays;

    RefreshTokenCleanupJob(
            RefreshTokenRepository refreshTokenRepository,
            Clock clock,
            @Value("${opspulse.security.refresh-token-retention-days:7}") int retentionDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${opspulse.security.refresh-token-cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        Instant cutoff = clock.instant().minus(retentionDays, ChronoUnit.DAYS);
        refreshTokenRepository.deleteExpiredBefore(cutoff);
    }
}
