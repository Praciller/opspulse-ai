package com.opspulse.identity.application;

import com.opspulse.audit.application.port.in.RecordAuditEventUseCase;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.identity.application.port.in.AuthenticationUseCase;
import com.opspulse.identity.application.port.out.PasswordHasher;
import com.opspulse.identity.application.port.out.RefreshTokenRepository;
import com.opspulse.identity.application.port.out.TokenService;
import com.opspulse.identity.application.port.out.UserRepository;
import com.opspulse.identity.domain.EmailAddress;
import com.opspulse.identity.domain.RefreshToken;
import com.opspulse.identity.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService implements AuthenticationUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final RecordAuditEventUseCase auditEvents;
    private final RefreshTokenFamilyRevocation familyRevocation;
    private final Clock clock;

    public AuthenticationService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasher passwordHasher,
            TokenService tokenService,
            RecordAuditEventUseCase auditEvents,
            RefreshTokenFamilyRevocation familyRevocation,
            Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.auditEvents = auditEvents;
        this.familyRevocation = familyRevocation;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthenticationResult login(LoginCommand command) {
        Optional<User> candidate = findCandidate(command.email());
        String passwordHash =
                candidate.map(User::passwordHash).orElseGet(passwordHasher::dummyHash);
        boolean passwordMatches = passwordHasher.matches(command.password(), passwordHash);

        if (candidate.isEmpty() || !passwordMatches || !candidate.orElseThrow().active()) {
            recordFailure(command);
            throw new IdentityAuthenticationException();
        }

        User user = candidate.orElseThrow();
        AuthenticationResult result = issueAndStore(user, UUID.randomUUID());
        auditEvents.record(new AuditEvent(
                UUID.randomUUID(),
                user.id(),
                AuditEvent.LOGIN_SUCCESS,
                AuditEvent.USER_ENTITY,
                user.id(),
                command.requestId(),
                command.ipAddress(),
                clock.instant()));
        return result;
    }

    @Override
    @Transactional
    public AuthenticationResult refresh(String rawRefreshToken) {
        String tokenHash = tokenService.hashRefreshToken(rawRefreshToken);
        Instant now = clock.instant();
        RefreshToken stored = refreshTokenRepository
                .findByHash(tokenHash)
                .orElseThrow(IdentityAuthenticationException::new);

        if (!stored.isActiveAt(now)) {
            familyRevocation.revokeFamily(stored.familyId(), now);
            throw new IdentityAuthenticationException();
        }

        stored = refreshTokenRepository
                .findByHashForUpdate(tokenHash)
                .filter(token -> token.isActiveAt(now))
                .orElseThrow(IdentityAuthenticationException::new);

        var parsed = tokenService.parseRefreshToken(rawRefreshToken);
        if (!stored.userId().equals(parsed.userId())) {
            throw new IdentityAuthenticationException();
        }

        User user = userRepository
                .findById(stored.userId())
                .filter(User::active)
                .orElseThrow(IdentityAuthenticationException::new);

        refreshTokenRepository.save(stored.revokeAt(now));
        return issueAndStore(user, stored.familyId());
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = tokenService.hashRefreshToken(rawRefreshToken);
        Instant now = clock.instant();
        refreshTokenRepository
                .findByHash(tokenHash)
                .ifPresent(token -> familyRevocation.revokeFamily(token.familyId(), now));
    }

    private Optional<User> findCandidate(String rawEmail) {
        try {
            return userRepository.findByEmail(EmailAddress.of(rawEmail));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private AuthenticationResult issueAndStore(User user, UUID familyId) {
        var issued = tokenService.issue(user);
        Instant now = clock.instant();
        refreshTokenRepository.save(new RefreshToken(
                UUID.randomUUID(),
                user.id(),
                familyId,
                tokenService.hashRefreshToken(issued.refreshToken()),
                issued.refreshExpiresAt(),
                null,
                now));
        return new AuthenticationResult(
                issued.accessToken(), issued.refreshToken(), issued.accessExpiresInSeconds(), user);
    }

    private void recordFailure(LoginCommand command) {
        auditEvents.recordInNewTransaction(new AuditEvent(
                UUID.randomUUID(),
                null,
                AuditEvent.LOGIN_FAILURE,
                AuditEvent.USER_ENTITY,
                null,
                command.requestId(),
                command.ipAddress(),
                clock.instant()));
    }
}
