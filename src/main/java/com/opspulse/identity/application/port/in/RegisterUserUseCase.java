package com.opspulse.identity.application.port.in;

import com.opspulse.identity.domain.Role;
import com.opspulse.identity.domain.User;
import java.util.Set;
import java.util.UUID;

public interface RegisterUserUseCase {

    User register(RegisterUserCommand command);

    record RegisterUserCommand(
            String email,
            String password,
            String fullName,
            Set<Role> requestedRoles,
            UUID actorUserId,
            Set<Role> actorRoles,
            String requestId,
            String ipAddress) {}
}
