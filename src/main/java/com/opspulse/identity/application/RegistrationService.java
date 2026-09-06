package com.opspulse.identity.application;

import com.opspulse.audit.application.port.in.RecordAuditEventUseCase;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.identity.application.port.in.RegisterUserUseCase;
import com.opspulse.identity.application.port.out.PasswordHasher;
import com.opspulse.identity.application.port.out.UserRepository;
import com.opspulse.identity.domain.EmailAddress;
import com.opspulse.identity.domain.Role;
import com.opspulse.identity.domain.User;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService implements RegisterUserUseCase {

    private static final int MIN_PASSWORD_BYTES = 12;
    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final RecordAuditEventUseCase auditEvents;
    private final Clock clock;

    public RegistrationService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            RecordAuditEventUseCase auditEvents,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.auditEvents = auditEvents;
        this.clock = clock;
    }

    @Override
    @Transactional
    public User register(RegisterUserCommand command) {
        EmailAddress email = EmailAddress.of(command.email());
        validatePassword(command.password());
        String fullName = normalizeFullName(command.fullName());
        Set<Role> roles = rolesFor(command);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        UUID id = UUID.randomUUID();
        UUID creator = command.actorUserId() == null ? id : command.actorUserId();
        var now = clock.instant();
        User user = userRepository.save(new User(
                id,
                email,
                passwordHasher.hash(command.password()),
                fullName,
                true,
                roles,
                null,
                now,
                creator,
                now,
                creator,
                0));
        auditEvents.record(new AuditEvent(
                UUID.randomUUID(),
                command.actorUserId(),
                AuditEvent.USER_REGISTERED,
                AuditEvent.USER_ENTITY,
                user.id(),
                null,
                Map.of(
                        "email", user.email().value(),
                        "fullName", user.fullName(),
                        "active", user.active(),
                        "roles", user.roles().stream().map(Role::name).sorted().toList()),
                command.requestId(),
                command.ipAddress(),
                now));
        return user;
    }

    private Set<Role> rolesFor(RegisterUserCommand command) {
        Set<Role> requested = command.requestedRoles() == null || command.requestedRoles().isEmpty()
                ? Set.of(Role.VIEWER)
                : Set.copyOf(command.requestedRoles());
        Set<Role> actorRoles =
                command.actorRoles() == null ? Set.of() : Set.copyOf(command.actorRoles());
        if (!actorRoles.contains(Role.ADMIN) && !requested.equals(Set.of(Role.VIEWER))) {
            throw new IdentityAuthorizationException();
        }
        return requested;
    }

    private static void validatePassword(String password) {
        int byteLength = password.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength < MIN_PASSWORD_BYTES || byteLength > MAX_PASSWORD_BYTES) {
            throw new IllegalArgumentException("password must be between 12 and 72 UTF-8 bytes");
        }
    }

    private static String normalizeFullName(String rawFullName) {
        String fullName = rawFullName == null ? "" : rawFullName.trim();
        if (fullName.isEmpty() || fullName.length() > 120) {
            throw new IllegalArgumentException("fullName must contain between 1 and 120 characters");
        }
        return fullName;
    }
}
