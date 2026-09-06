package com.opspulse.identity.application.port.out;

import com.opspulse.identity.domain.EmailAddress;
import com.opspulse.identity.domain.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findByEmail(EmailAddress email);

    Optional<User> findById(UUID id);

    boolean existsByEmail(EmailAddress email);

    User save(User user);
}
