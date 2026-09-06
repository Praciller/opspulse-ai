package com.opspulse.identity.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.SingleResultAuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

@Configuration(proxyBeanMethods = false)
public class RegistrationAuthorizationConfiguration {

    @Bean("registrationAuthorizationManager")
    AuthorizationManager<RequestAuthorizationContext> registrationAuthorizationManager(
            IdentitySecurityProperties properties) {
        if (properties.registrationMode()
                == IdentitySecurityProperties.RegistrationMode.PUBLIC_VIEWER) {
            return SingleResultAuthorizationManager.permitAll();
        }
        return AuthorityAuthorizationManager.hasRole("ADMIN");
    }
}
