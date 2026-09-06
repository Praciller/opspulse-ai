package com.opspulse.identity.application;

import com.opspulse.identity.application.port.out.RefreshTokenRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class RefreshTokenFamilyRevocation {

    private final RefreshTokenRepository refreshTokenRepository;

    RefreshTokenFamilyRevocation(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeFamily(UUID familyId, Instant revokedAt) {
        refreshTokenRepository.revokeFamilyAt(familyId, revokedAt);
    }
}
