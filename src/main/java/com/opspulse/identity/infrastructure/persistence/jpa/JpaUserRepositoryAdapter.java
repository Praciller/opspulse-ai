package com.opspulse.identity.infrastructure.persistence.jpa;

import com.opspulse.identity.application.DuplicateEmailException;
import com.opspulse.identity.application.port.out.UserRepository;
import com.opspulse.identity.domain.EmailAddress;
import com.opspulse.identity.domain.Role;
import com.opspulse.identity.domain.User;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository users;
    private final SpringDataRoleRepository roles;

    JpaUserRepositoryAdapter(
            SpringDataUserRepository users, SpringDataRoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return users.findOneByEmailIgnoreCase(email.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return users.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return users.existsByEmailIgnoreCase(email.value());
    }

    @Override
    public User save(User user) {
        var entity = new UserJpaEntity();
        entity.id = user.id();
        entity.email = user.email().value();
        entity.passwordHash = user.passwordHash();
        entity.fullName = user.fullName();
        entity.active = user.active();
        entity.organizationId = user.organizationId();
        entity.createdAt = user.createdAt();
        entity.createdBy = user.createdBy();
        entity.updatedAt = user.updatedAt();
        entity.updatedBy = user.updatedBy();
        entity.version = user.version();
        entity.roles = Set.copyOf(roles.findAllByNameIn(
                user.roles().stream().map(Role::name).toList()));
        if (entity.roles.size() != user.roles().size()) {
            throw new IllegalStateException("Configured role reference data is incomplete");
        }
        try {
            return toDomain(users.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(
                entity.id,
                EmailAddress.of(entity.email),
                entity.passwordHash,
                entity.fullName,
                entity.active,
                entity.roles.stream()
                        .map(role -> Role.valueOf(role.name))
                        .collect(Collectors.toUnmodifiableSet()),
                entity.organizationId,
                entity.createdAt,
                entity.createdBy,
                entity.updatedAt,
                entity.updatedBy,
                entity.version);
    }
}
