package com.opspulse.identity.infrastructure.security;

import com.opspulse.identity.application.port.out.UserRepository;
import com.opspulse.identity.domain.Role;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("persistedUserJwtAuthenticationConverter")
public class PersistedUserJwtAuthenticationConverter
        implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {

    private final UserRepository users;

    public PersistedUserJwtAuthenticationConverter(UserRepository users) {
        this.users = users;
    }

    @Override
    public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt jwt) {
        try {
            if (!NimbusTokenService.ACCESS_TOKEN_USE.equals(
                    jwt.getClaimAsString(NimbusTokenService.TOKEN_USE_CLAIM))) {
                throw new BadCredentialsException("Invalid access token");
            }
            UUID userId = UUID.fromString(jwt.getSubject());
            var user = users.findById(userId)
                    .filter(candidate -> candidate.active()
                            && candidate.email().value().equals(jwt.getClaimAsString("email")))
                    .orElseThrow(() -> new BadCredentialsException("Invalid access token"));
            var authorities = user.roles().stream()
                    .map(Role::name)
                    .sorted()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            return new JwtAuthenticationToken(jwt, authorities, user.id().toString());
        } catch (IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid access token", exception);
        }
    }
}
